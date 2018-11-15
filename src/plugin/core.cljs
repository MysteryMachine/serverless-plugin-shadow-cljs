(ns plugin.core
  (:require ["bluebird" :as bluebird]
            ["child_process" :as chp]
            ["node-watch" :as watch]
            ["fs" :as fs]
            [cljs.reader]))

(def exec (bluebird/promisify (.-exec chp) #js{:multiArgs true}))

(defn read-config! [path]
  (cljs.reader/read-string
   (.toString
    ((.-readFileSync fs)
     (str path "/shadow-cljs.edn")))))

(defn watched-builds [build-map]
  (->> (map (comp #(.substr % 1) str first) build-map)
       (filter #(not (.match % "dev/")))))

(defn build-cljs* [serverless options builds]
  (fn []
    (js/console.log "")
    (js/console.log "Compiling Clojurescript!")
    (js/console.log "")
    (doseq [build builds]
      (exec (str "shadow-cljs release " build)))))

(defn build-hooks [serverless options build-map]
  (let [build-cljs (build-cljs* serverless options (watched-builds build-map))]
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
  (let [service-path (-> serverless
                         (aget "config")
                         (aget "servicePath"))
        build-map (:builds (read-config! service-path))] 
    (js/console.log "Watching *.cljs files in" service-path)
    (js/console.log "")
    (watch service-path
           #js {:recursive true
                :filter (js/RegExp. "\\.cljs$")}
           (fn [evt name]
             (when (= "update" evt)
               (build-cljs* serverless options (watched-builds build-map)))))
    #js {:hooks (build-hooks serverless options build-map)}))

(defn repl [])
