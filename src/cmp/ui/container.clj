(ns cmp.ui.container
  (:require [cmp.ui.task  :as tsk]
            [cmp.ui.index :as ui]
            [cmp.st-utils :as stu]
            [cmp.utils :as u]))

;;------------------------------
;; table cell funs
;;------------------------------

(defmulti td-value  (fn [conf m kw] kw))

(defmethod td-value :task    [conf m kw] (tsk/card conf m))

(defmethod td-value :mp-id   [conf m kw] [:b (ui/mp-id-link conf m)])

(defmethod td-value :struct  [conf m kw] [:span {:class "tag"} (:struct m)])

(defmethod td-value :no-idx  [conf m kw] [:span {:class "tag"} (:no-idx m)])

(defmethod td-value :seq-idx [conf m kw] [:span   {:class "tag"} (:seq-idx m)])

(defmethod td-value :par-idx [conf m kw] [:span {:class "tag"} (kw m)])

(defmethod td-value :level   [conf m kw] [:i (kw m)])

(defmethod td-value :func    [conf m kw] [:span {:class "tag"} (kw m)])

(defmethod td-value :title
  [conf m kw]
  [:a {:class "is-link is-light"
       :href (ui/href m "/container")} (kw m)])

(defmethod td-value :run
  [conf m kw]
  (ui/button (assoc m :key (:ctrl-key m)) kw :success))

(defmethod td-value :suspend
  [conf m kw]
  (ui/button (assoc m :key (:ctrl-key m)) kw :info))

(defmethod td-value :stop
  [conf m kw]
  (ui/button (assoc m :key (:ctrl-key m)) kw :warn))

(defmethod td-value :mon
  [conf m kw]
  (ui/button (assoc m :key (:ctrl-key m)) kw :success))

(defmethod td-value :ready
  [conf m kw]
  (ui/button (assoc m :key (:state-key m)) kw :info))

(defmethod td-value :working
  [conf m kw]
  (ui/button (assoc m :key (:state-key m)) kw :warn))

(defmethod td-value :executed
  [conf m kw]
  (ui/button (assoc m :key (:state-key m)) kw :success))

(defmethod td-value :state
  [conf m kw]
  (let [state (kw m)]
    [:div {:class (str "is-size-6 " state) :id (ui/make-selectable (:state-key m))} state]))

(defmethod td-value :ctrl
  [conf m kw]
  (let [ctrl (kw m)]
    [:div {:class (str "is-size-6 " ctrl) :id (ui/make-selectable (:ctrl-key m))} ctrl]))

(defmethod td-value :key
  [conf m kw]
  [:span {:class "icon"}
   [:a  {:class "copy is-link fas fa-key" :data-copy (kw m) :title "click to copy key to clipboard"}]])

(defmethod td-value :default
  [conf m kw]
  (if-let [x (kw m)]
    (cond
      (boolean? x) [:div {:class "tag"} x]
      (number? x)  [:div {:class "tag"} x]
      (string? x)  [:div {:class (str "is-size-6 " x) :id (ui/make-selectable (:key m))} x]
      (map?    x)  (into [:ul] (mapv (fn [[k v]] [:li [:span (td-value v k)]]) x)))
    [:span  {:class "tag"} "::"]))

;;------------------------------
;; table funs
;;------------------------------
(defn td [conf m kws] (mapv (fn [kw] [:td (td-value conf m kw)]) kws))

(defn table-row [conf m kws] (mapv (fn [x] (into [:tr ] (td conf x kws))) m))

(defn table-head
  [conf kws]
  (into
   (into [:thead] (mapv (fn [x] [:col {:class (name x)}]) kws))
   (mapv (fn [x] [:th (or (get-in conf [:ui :trans x]) x)]) kws)))

(defn table-base
  [conf kws]
  [:table {:class "table is-bordered is-hoverable is-fullwidth"}
   (table-head conf kws)])

(defn table
  ([conf data]
   (table conf data (keys (first data)))) 
  ([conf data head]
   (if (empty? data) (ui/empty-msg "no table data")
       (into (table-base conf head) (table-row conf data head)))))

;;------------------------------
;; modal message
;;------------------------------
(defn message-id
  [conf m]
  (ui/make-selectable (stu/key->message-key (:state-key m))))
  
(defn message
  [conf m]
  [:div {:class "modal" :id (message-id conf m)} 
   [:div {:class "modal-background"}]
   [:div {:class "modal-content"}
    [:div {:class "notification is-light is-info"}]
    [:button {:class "message_ok button is-light is-success"
              :data-url (ui/post-url m)
              :data-key (stu/key->message-key (:state-key m))
              :data-value "ok"} "ok"]]])

(defn messages [conf data]
  (into [:div] (mapv (fn [m]
                       (when (= 0 (+ (u/ensure-int (:seq-idx m))
                                     (u/ensure-int (:par-idx m))))
                         (message conf m))) data)))

;;------------------------------
;; view-*
;;------------------------------
(defn view
  [conf req data]
  (let [cols [:title
              :ctrl :run :suspend :stop :mon
              :state :ready :working :executed
              :key :task]]
    (ui/index conf req
              [:div {:class "content"}
               (table conf data cols)
               (messages conf data)])))
