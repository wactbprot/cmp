(ns cmp.ui.container
  (:require [cmp.ui.task  :as tsk]
            [cmp.ui.index :as ui]))

;;------------------------------
;; table cell funs
;;------------------------------

(defmulti td-value  (fn [conf m kw] kw))

(defmethod td-value :task     [conf m kw] (tsk/card conf m))

(defmethod td-value :mp-id    [conf m kw] [:b (ui/mp-id-link m)])

(defmethod td-value :title    [conf m kw] [:div {:class "is-size-6"} (kw m)])

(defmethod td-value :run      [conf m kw] (ui/button m kw :info))

(defmethod td-value :stop     [conf m kw] (ui/button m kw :warn))

(defmethod td-value :mon      [conf m kw] (ui/button m kw :warn))

(defmethod td-value :ready    [conf m kw] (ui/button m kw :info))

(defmethod td-value :working  [conf m kw] (ui/button m kw :warn))

(defmethod td-value :executed [conf m kw] (ui/button m kw :success))

(defmethod td-value :par-idx  [conf m kw] [:span {:class "tag"} (kw m)])

(defmethod td-value :seq-idx  [conf m kw] [:span {:class "tag"} (kw m)])

(defmethod td-value :level    [conf m kw] [:i (kw m)])

(defmethod td-value :func     [conf m kw] [:span {:class "tag"} (kw m)])

(defmethod td-value :key
  [conf m kw]
  [:span {:class "icon"}
   [:a  {:class "copy is-link fas fa-key" :data-copy (kw m) :title (str "click to console.log: " (kw m))}]])

(defmethod td-value :struct
  [conf m kw]
  (let [m (dissoc m :no-idx :seq-idx)]
    [:span   {:class "tag"} (:struct m)
     [:span  {:class "tag"} (ui/ctrl-link m)]
     [:span  {:class "tag"} (ui/state-link m)]
     [:span  {:class "tag"} (ui/definition-link m)]]))

(defmethod td-value :no-idx 
  [conf m kw]
  (let [m (dissoc m :seq-idx)]
    [:span   {:class "tag"} (:no-idx m)
     [:span  {:class "tag"} (ui/ctrl-link m)]
     [:span  {:class "tag"} (ui/state-link m)]
     [:span  {:class "tag"} (ui/definition-link m)]]))

(defmethod td-value :seq-idx
  [conf m kw]
    [:span   {:class "tag"} (:seq-idx m)
     [:span  {:class "tag"} (ui/state-link m)]
     [:span  {:class "tag"} (ui/definition-link m)]])

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
  [:table {:class "table is-hoverable is-fullwidth fixed"}
   (table-head conf kws)])

(defn table
  ([conf data]
   (table conf data (keys (first data)))) 
  ([conf data head]
   (if (empty? data) (ui/empty-msg "no table data")
       (into (table-base conf head) (table-row conf data head)))))

;;------------------------------
;; view-*
;;------------------------------
(defn view-ctrl
  [conf req data]
  (let [cols [:key  :struct :func :no-idx :title :value :run :stop :mon]]
  (ui/index conf req (table conf data cols))))

(defn view-state
  [conf req data]
  (let [cols [:key :struct :func :no-idx :seq-idx :value :ready :working :executed]]
  (ui/index conf req
            (into [:div]
                  [[:h3 {:class "title"} (:title (first data))]
                   (table conf data cols)]))))

(defn view
  [conf req data]
  (let [cols [:key  :struct :func :no-idx :seq-idx :task]]
    (ui/index conf req (table conf data cols))))
