(ns cmp.ui.mp-meta
  (:require
   [cmp.config    :as config]
   [cmp.ui.index  :as ui]
   [cmp.api-utils :as au]
   [cmp.utils :as u]))

(defn doc-link
  [conf m]
  (when-let [id  (:doc-id m)]
    [:p  [:a {:href (str (config/lt-url conf) "/_utils/#database/"
                         (:lt-db conf) "/" id) }
          [:b  id ]] [:span {:class "tag is-info"} (:doc-version m)]]))

(defn card-content
  [conf m]
  [:div {:class "card-content"}  
   [:div {:class "columns"}
    [:div {:class "column"}
     [:figure {:class "image is-3by1"}
      (ui/img conf m "../../")]]
    [:div {:class "column"}
     (ui/mp-id-link conf m)
     [:p [:b "Standard: "          (:std m)]]
     [:p [:b "Beschreibung: "] [:i (:descr m)]]]]
   (when-not (empty? (:docs m))
     (into [:p [:b "CAL-Dokumente: "]]
           (mapv (fn [d]  (doc-link conf d)) (:docs m))))])
  
(defn card [conf m]
  (ui/card-template conf m (card-content conf m) (ui/card-footer conf (assoc m :no-idx (u/lp 0)))))

(defn view [conf req data] (ui/index conf req (card conf data)))
