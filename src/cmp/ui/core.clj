(ns cmp.ui.core
  (:require
     [hiccup.form    :as hf]
     [hiccup.page    :as hp]
     [cmp.key-utils  :as ku]
     [clojure.string :as string]
     [cheshire.core  :as che]))

(defn empty-msg [s] [:span {:class "tag is-info"} s])

(defn make-selectable [k] (when (string? k) (string/replace k ku/re-sep "_")))

(defn mp-id-link [m] [:a {:href (str  "/ui/" (:mp-id m) "/meta")} (:mp-id m)])

(defn state-link
  ([m]
   (state-link m false))
  ([m i]
  [:span {:class "icon"}
   [:a  {:class "is-link fas fa-cogs"
         :href (str  "/ui/" (:mp-id m) "/" (:struct m) "/state" (when i (str "/" i)))}]]))

(defn ctrl-link
  ([m]
   (ctrl-link m false))
  ([m i]
   [:span {:class "icon"}
     [:a  {:class "far fa-play-circle"
           :href (str  "/ui/" (:mp-id m) "/" (:struct m)  "/ctrl" (when i (str "/" i)))}]]))

(defn definition-link
  ([m]
   (definition-link m false))
  ([m i]
   [:span {:class "icon"}
    [:a  {:class "is-link far fa-folder"
          :href (str  "/ui/" (:mp-id m) "/" (:struct m) "/definition" (when i (str "/" i)))}]]))

(defn post-url [m] (str (:mp-id m) "/container"))

(defn button
  [m kw cls]
  (let [ds "button is-small is-light setter "
        cs (condp = cls
             :ok    "is-success"
             :warn  "is-warning"
             :error "is-danger"
             "is-primary")]
    [:button {:class (str ds cs)
              :data-value (kw m)
              :data-url (post-url m)
              :data-key (:key m)}
     (kw m)]))

;;------------------------------
;; table cell funs
;;------------------------------
(defmulti td-value  (fn [m kw] kw))

(defmethod td-value :mp-id [m kw] [:b (mp-id-link m)])

(defmethod td-value :no-idx 
  [m kw]
  [:span   {:class "tag"} (:no-idx m)
   [:span  {:class "tag"} (definition-link m (:no-idx m))]
   [:span  {:class "tag"} (state-link m (:no-idx m))]
   [:span  {:class "tag"} (ctrl-link m (:no-idx m))]])

(defmethod td-value :run [m kw] (button m kw :ok))
(defmethod td-value :stop [m kw] (button m kw :warn))
(defmethod td-value :mon [m kw] (button m kw :warn))
(defmethod td-value :ready [m kw] (button m kw :ok))
(defmethod td-value :working [m kw] (button m kw :warn))
(defmethod td-value :executed [m kw] (button m kw :ok))


(defmethod td-value :par-idx [m kw] [:i (kw m)])

(defmethod td-value :seq-idx [m kw] [:i (kw m)])

(defmethod td-value :level [m kw] [:i (kw m)])

(defmethod td-value :struct
  [m kw]
  [:span   {:class "tag"} (:struct m)
   [:span  {:class "tag"} (definition-link m)]
   [:span  {:class "tag"} (state-link m)]
   [:span  {:class "tag"} (ctrl-link m)]])

(defmethod td-value :func [m kw] [:span {:class "tag"} (kw m)])

(defmethod td-value :TaskName [m kw] [:span {:class "tag"} m])

(defmethod td-value :Replace [m kw] [:pre (che/encode m {:pretty true})])

(defmethod td-value :Use [m kw] [:pre (che/encode m {:pretty true})])

(defmethod td-value :key
  [m kw]
  [:span {:class "icon"}
   [:a  {:class "copy is-link fas fa-key" :data-copy (kw m) :title (str "click to console.log: " (kw m))}]])

(defmethod td-value :default
  [m kw]
  (if-let [x (kw m)]
    (cond
      (boolean? x) [:div {:class "tag"} x]
      (number? x)  [:div {:class "tag"} x]
      (string? x)  [:div {:class "tag" :id (make-selectable (:key m))} x]
      (map?    x)  (into [:ul] (mapv (fn [[k v]] [:li [:span (td-value v k)]]) x)))
    [:span  {:class "tag"} "::"]))

;;------------------------------
;; table funs
;;------------------------------
(defn kw-head [m] (keys (first m)))

(defn t-head
  [conf kws]
  (into
   (into [:thead] (mapv (fn [x] [:col {:class (name x)}]) kws))
   (mapv (fn [x] [:th (or (get-in conf [:ui :trans x]) x)]) kws)))

(defn td [m kws] (mapv (fn [kw] [:td (td-value m kw)]) kws))

(defn t-row [m kws] (mapv (fn [x] (into [:tr ] (td x kws))) m))

(defn t-base [conf kws]
  [:table {:class "table is-hoverable is-fullwidth fixed"}
   (t-head conf kws)])

(defn table
  ([conf data]
   (table conf data (kw-head data))) 
  ([conf data head]
   (if (empty? data) (empty-msg "no table data")
       (into (t-base conf head) (t-row data head)))))

;;------------------------------
;; page funs
;;------------------------------
(defn page-header
  [conf]
  [:head
   [:title (:page-title conf)]
   (hp/include-css "/css/bulma.css")
   (hp/include-css "/css/all.css")
   (hp/include-css "/css/ui.css")])

(defn index-title
  [conf]
  [:section {:class "hero is-info"}
   [:div {:class "hero-body"}
      [:div {:class "container"}
       [:h1 {:class "title"} (:main-title conf)]
       [:h2 {:class "subtitle"}]]]])

(defn index
  [{conf :ui} body]
  (hp/html5
   (page-header conf)
   [:body
    (index-title conf)
    [:section {:class "section"}
     [:div {:class "container content"}
      [:div {:class "box"}
       [:div {:class "columns"}
        body]]]]
    (hp/include-js "/js/jquery-3.5.1.min.js")
    (hp/include-js "/js/ws.js")
    (hp/include-js "/js/main.js")]))
