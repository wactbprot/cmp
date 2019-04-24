(ns cmp.prep
  (:require [cmp.utils :as u]
            [cmp.st :as st]
            [cmp.task :as t]
            [cmp.lt :as lt])
  (:gen-class))

(defn container [path i]
  (let [definition-keys (st/get-keys
                  (u/gen-key [path "container" i "definition"]))]
    (map (fn [k]
           (let [state-key (u/replace-key-level 3 k "state")]
             (st/set-val state-key "start-prep")
             (println (t/compl (u/gen-map (st/get-val k))))
         ))
         definition-keys)))

