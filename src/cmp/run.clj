(ns cmp.run
  ^{:author "wactbprot"
    :doc "Finds and starts the up comming 
          tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.work :as work]            
            [cmp.task :as tsk]
            [cmp.utils :as u]))

;;------------------------------
;; listeners 
;;------------------------------
(def listeners (atom {}))

;;------------------------------
;; exception channel 
;;------------------------------
(def excep-chan (a/chan))
(a/go
  (while true
    (let [e (a/<! excep-chan)] 
      (timbre/error (.getMessage e)))))

;;------------------------------
;; ctrl channel invoked by poll 
;;------------------------------
(def ctrl-chan (a/chan))

(defn state-map->definition-key
  "Converts a state-map into a key."
  [m]
  (u/vec->key [(m :mp-name) (m :struct) (m :no-idx)
               "definition" (m :seq-idx) (m :par-idx)]))

(defn state-key->state-map
  "Converts a key in a state-map."
  [k]
  {:mp-name (u/key->mp-name k)
   :struct (u/key->struct k)
   :no-idx (u/key->no-idx k)
   :seq-idx (u/key->seq-idx k)
   :par-idx (u/key->par-idx k)
   :state (keyword (st/key->val k))})

(defn ks->state-map
  "Builds the state map `m` belonging to a key set `ks`.
  `m` is introduced in order to keep the functions testable." 
  [ks]
  (mapv
   (fn [k]
     (state-key->state-map k))
   ks))

(defn p->state-ks
  "Returns the state keys for a given path"
  [p]
  (sort (st/get-keys
         (u/vec->key [(u/key->mp-name p)
                      (u/key->struct p)
                      (u/key->no-idx p)
                      "state"]))))

(defn p->ctrl-k
  "Returns the `ctrl` key for a given path `p`."
  [p]
  (u/vec->key [(u/key->mp-name p)
               (u/key->struct p)
               (u/key->no-idx p)
               "ctrl"]))

(defn filter-state
  [m s]
  (filter (fn [x] (= s (x :state))) m))

(defn seq-idx->all-par-idx
  "Returns all `par` steps for a given
  state map `m` and `seq-idx`

  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :executed}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :executed}
   {:seq-idx 4, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 1, :state :executed}
   {:seq-idx 4, :par-idx 2, :state :ready}])
  
  (seq-idx->all-par-idx m 4)
  ({:seq-idx 4, :par-idx 0, :state :ready}
  {:seq-idx 4, :par-idx 1, :state :executed}
  {:seq-idx 4, :par-idx 2, :state :ready})
  ```"
  [m i]
  (filter (fn [x] (= i (x :seq-idx))) m))
  
(defn all-error
  "Returns all  steps with the state
  `:error` for a given state map `m`"
  [m]
  (filter-state m :error))

(defn all-ready
  "Returns all  steps with the state
  `:ready` for a given state map `m`"
  [m]
  (filter-state m :ready))

(defn all-working
  "Returns all  steps with the state
  `:working` for a given state map `m`.
  
  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :working}
   {:seq-idx 1, :par-idx 0, :state :working}
   {:seq-idx 2, :par-idx 0, :state :executed}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 0, :state :executed}
   {:seq-idx 5, :par-idx 0, :state :ready}])

  (all-working m)
  ```"
  [m]
  (filter-state m :working))

(defn all-executed
  "Returns all-executed entrys of the given state-map.
  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :executed}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 0, :state :executed}
   {:seq-idx 4, :par-idx 0, :state :ready}])

  (all-executed (seq-idx->all-par-idx m 4))
  ;; gives
  ;; ({:seq-idx 4, :par-idx 0, :state :executed})

  ```"
  [m]
  (filter-state m :executed))

(defn all-executed?
  "Checks if all entries of map `m`
  are executed"
  [m]
  (=
   (count m)
   (count (all-executed m))))

(defn errors?
  "Checks if there are any errors in the map `m`."
  [m]
  (not (empty? (all-error m))))
          
(defn next-ready
  "Returns a map with the next
  step with state `:ready`. Returns an
  empty map if nothing next"
  [m]
  (let [am (all-ready m)
        n (count am)]
    (cond
      (= n 0) {}
      :else (first am))))

(defn predecessor-executed?
  "Checks if `all-executed?` in the
  step `i-1` of `m`."
  [m i]
  (all-executed?
   (seq-idx->all-par-idx m (- i 1))))

(defn find-next
  "The `find-next` function
  returns a list of maps containing the next
  tasks to start. It should work as follows:

  ```clojure
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :ready}
   {:seq-idx 0, :par-idx 1, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  ({:seq-idx 0, :par-idx 0, :state :ready}
  ;;   {:seq-idx 0, :par-idx 1, :state :ready})
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :working}
   {:seq-idx 0, :par-idx 1, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  ({:seq-idx 0, :par-idx 1, :state :ready})
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :working}
   {:seq-idx 0, :par-idx 1, :state :working}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  nil
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :executed}
   {:seq-idx 0, :par-idx 1, :state :executed}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  ({:seq-idx 1, :par-idx 0, :state :ready})
  ```
  It should not crash on:

  ```clojure
  cmp.run> (count (next-ready m))
  ;; 0
  cmp.run> (find-next m)
  ;; nil
  cmp.run> (find-next {})
  ;; nil
  cmp.run> (find-next nil)
  ;; nil
  ```"
  [m]
  (when-let [seq-idx ((next-ready m) :seq-idx)]
    (cond
      (or
       (predecessor-executed? m seq-idx)
       (= seq-idx 0)) (all-ready (seq-idx->all-par-idx m seq-idx))
      :else nil)))

;;------------------------------
;; registered?, de-register
;;------------------------------
(defn registered?
  "Checks if a `listener` is registered under
  the `listeners`-atom."
  [mp-id]
  (contains? (deref listeners) mp-id))

(defn de-register!
  "De-registers the listener with the
  key `mp-id` in the `listeners` atom."
  [mp-id]
  (cond
    (registered? mp-id) (do
                          (st/close-listener! ((deref listeners) mp-id))
                          (swap! listeners dissoc mp-id))
    :else (timbre/info "a ctrl listener for "
                       mp-id
                       " is not registered!")))

;;------------------------------
;; set value at ctrl-path 
;;------------------------------

(defn error-ctrl!
  "Sets the `ctrl` interface to `\"error`."
  [p]
  (timbre/error "got errors under path: " p)
  (st/set-val! (p->ctrl-k p) "error"))

(defn all-exec-ctrl!
  "Handels the case where all `state` interfaces
  are `\"executed\"`."
  [p]
  (let [ctrl-str (->> p
                      (p->ctrl-k)
                      (st/key->val)
                      (u/get-next-ctrl)
                      keyword)
        state-ks (p->state-ks p)]
    (cond
      (= ctrl-str :run) (do
                          (timbre/info "all done at " p
                                       "will set ready")
                          (st/set-val! p "ready")
                          (st/set-same-val! state-ks "ready")
                          (de-register! (u/key->mp-name p)))
      (= ctrl-str :mon) (do
                          (timbre/info "all done at " p
                                       "will keep mon")
                          (st/set-same-val! state-ks "ready")))))

(defn nil-ctrl!
  "Kind of `nop`."
  [p]
  (timbre/debug "no new task to start at path: " p))

;;------------------------------
;; pick next task
;;------------------------------
(defn start-next!
  "Receives the path p and picks the next thing to do.
  p looks like this (must be a string):
  ```clojure
  
  (start-next! \"se3-calib@container@0@ctrl\")
  ```"
  [p]
  (let [ctrl-path (p->ctrl-k p)
        state-ks (p->state-ks ctrl-path)
        state-map (ks->state-map state-ks)
        next-to-start (find-next state-map)]
    (cond
      (errors?       state-map)     (error-ctrl! ctrl-path)
      (all-executed? state-map)     (all-exec-ctrl! ctrl-path)
      (nil?          next-to-start) (nil-ctrl! ctrl-path)
      :else (run!
             (fn [m]
               (a/>!! work/ctrl-chan (state-map->definition-key m))
               (a/<!! (a/timeout 500)))
             next-to-start))))

;;------------------------------
;; status 
;;------------------------------
(defn status
  [p]
  (->> p
       (p->state-ks)
       (ks->state-map)))


;;------------------------------
;; registered?, de-register
;;------------------------------
(defn register!
  "Generates a `state` listener and registers him
  under the key `mp-id` in the `listeners` atom.
  The callback function is `start-next`."
  [mp-id]
  (cond
    (registered? mp-id) (timbre/info "a state listener for "
                                     mp-id
                                     " is already registered!") 
    :else (swap! listeners  assoc
                 mp-id
                 (st/gen-listener mp-id "state"
                                  (fn
                                    [msg]
                                    (start-next! (st/msg->key msg)))))))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [p (a/<! ctrl-chan)]
      (try
        (start-next! p)
        (register! (u/key->mp-name p))
        (catch Exception e
          (timbre/error "catch error at channel " p)
          (a/>! excep-chan e))))))
