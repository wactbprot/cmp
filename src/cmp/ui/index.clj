(ns cmp.ui.index
  (:require
     [hiccup.form       :as hf]
     [hiccup.page       :as hp]
     [cmp.handler-utils :as au]
     [cmp.utils         :as u]
     [cmp.st-utils      :as stu]
     [clojure.string    :as string]))

(defn empty-msg [s] [:span {:class "tag is-info"} s])

(defn make-selectable [k] (when (string? k) (string/replace k stu/re-sep "_")))

(defn img
  "FIXME: The function name is more general than the function itself."
  [conf m rel]
  [:img {:src (str rel "img/" (get-in conf [:ui :img (keyword (:mp-id m))] "default.jpg"))}])

;;------------------------------
;; links
;;------------------------------
(defn mp-id-link [conf m]
  (let [mp (:mp-id m)]
    [:p "Messung/Steuerung: "
     [:a {:href (str  "/ui/" mp "/meta")}
      (get-in conf [:ui :mp-alias (keyword mp)] mp)]]))

(defn href [m p]
  (str  "/ui/" (:mp-id m)  p
        (when (:no-idx m) (str "/" (:no-idx m)))))

(defn container-link [conf m]
  [:a {:class "tag is-link is-light"
       :href (href m "/container")} (get-in conf [:ui :trans :container] "container")])

;;------------------------------
;; return data from client
;;------------------------------
(defn post-url [m] (str (:mp-id m) "/setter"))

(defn button
  [m kw cls]
  (when-let [value (kw m)]
    (let [ds "button is-small setter "
          cs (condp = cls
               :info    "is-outlined is-light is-info"
               :success "is-outlined is-light is-success"
               :warn    "is-outlined is-light is-warning"
               :error   "is-outlined is-light is-danger"
               "is-light is-outlined is-primary")]
      [:button {:class (str ds cs)
                :data-value (kw m)
                :data-url (post-url m)
                :data-key (:key m)}
       value])))

;;------------------------------
;; card funs
;;------------------------------
(defn card-footer
  [conf m]
  [:footer {:class "card-footer"}
   [:span {:class "card-footer-item"}
    (container-link conf m)]])

(defn card-template
  ([conf m content]
   (card-template conf m content nil))
  ([conf m content footer]
   [:div {:class "content"}
    [:div {:class "card"}
     [:div {:class "card-content"}
      content]
     footer]]))

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

(defn index-head-top
  [conf req]
  [:div {:class "hero-head"}
   [:div  {:class "navbar-menu"}
    [:div  {:class "navbar-start"}
     [:a {:class "navbar-item" :href "http://localhost:8009"} "DevProxy"]]
    [:div  {:class "navbar-end"}
     [:a {:class "navbar-item" :href "http://localhost:8081"} "Redis"]
     [:a {:class "navbar-item" :href "http://a75438.berlin.ptb.de:5601/app/discover"} "Logging"]
     [:a {:class "navbar-item" :href "https://a75436.berlin.ptb.de/vaclab/cmp"} "GitLab"]
     [:a {:class "navbar-item" :href "https://github.com/wactbprot/cmp"} "GitHub"]]]])

(defn index-head-body
  [conf req]
  (let [mp (au/req->mp-id req)]
    [:div {:class "hero-body"}
     [:div {:class "container"}
      [:h1 {:class "title"} (:main-title conf)]
      [:h2 {:class "subtitle"}  (get-in conf [:mp-alias (keyword mp)] mp)]]]))
  
(defn index-head-bottom
  [conf req]
  (let [mp-id (au/req->mp-id req)]
    [:div {:class "hero-foot"}
     [:nav {:class "tabs"}
      [:div {:class "container"}
       [:ul
        [:li [:a {:class "navbar-item is-link" :href "/ui/setup"} "Setup"]]
        [:li [:a {:class "navbar-item is-link" :href "/ui/listeners"} "Listener"]]
        (when-not (= mp-id "*")
          [:li [:a {:class "navbar-item is-link" :href (str "/ui/" mp-id "/meta")} "MP-Info"]])
        (when-not (= mp-id "*")
          [:li [:a {:class "navbar-item is-link" :href (str "/ui/" mp-id "/container")} "Container"]])
        (when-not (= mp-id "*")
          [:li [:a {:class "navbar-item is-link" :href (str "/ui/" mp-id "/elements")} "User Input/Output"]])]]]]))

(defn index-title
  [conf req]
  [:section {:class "hero is-dark"}
   (index-head-top    conf req)
   (index-head-body   conf req)
   (index-head-bottom conf req)])

(defn index
  [{conf :ui} req body]
  (let [mp-id (au/req->mp-id req)]
    (hp/html5 (page-header conf)
              [:body
               (index-title conf req)
               [:section {:class "section"}
                [:div {:class "container content"}
                 [:div {:class "box"}
                  body]]]
                  (hp/include-js "/js/jquery-3.5.1.min.js")
               (hp/include-js "/js/ws.js")
               (hp/include-js "/js/main.js")])))
  
