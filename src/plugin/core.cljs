(ns plugin.core
  (:require ["bluebird" :as bluebird]
            ["child_process" :as chp]
            ["node-watch" :as watch]))

(def exec (bluebird/promisify (.-exec chp) #js{:multiArgs true}))

(defn build-cljs* [serverless options]
  (fn []
    (js/console.log "")
    (js/console.log "Compiling Clojurescript!")
    (js/console.log "")
    (exec "shadow-cljs release serverless")))

(defn build-hooks [serverless options]
  (let [build-cljs (build-cljs* serverless options)]
    #js{"before:run:run" build-cljs
        "before:offline:start" build-cljs
        "before:offline:start:init" build-cljs
        "before:package:createDeploymentArtifacts" build-cljs
        "before:deploy:function:packageFunction" build-cljs
        "before:invoke:local:invoke" build-cljs}))

(defn Plugin [serverless options]
  (js/console.log "")
  (js/console.log "The clouds cast their shadows! Preparing to compile Clojurescript.")
  (js/console.log "Be sure to run `shadow-cljs server` in order to speed up builds.")
  (js/console.log "Watching *.cljs files in" (-> serverless (aget "config") (aget "servicePath")))
  (js/console.log "")
  (watch (-> serverless (aget "config") (aget "servicePath"))
         #js {:recursive true
              :filter (js/RegExp. "\\.cljs$")}
         (fn [evt name]
           (when (= "update" evt)
             ((build-cljs* serverless options)))))
  #js {:hooks (build-hooks serverless options)})

(defn repl [])
