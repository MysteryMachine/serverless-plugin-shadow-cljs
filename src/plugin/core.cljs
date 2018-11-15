(ns plugin.core
  (:require ["bluebird" :as bluebird]
            ["child_process" :as chp]
            ["node-watch" :as watch]
            ["fs" :as fs]
            [cljs.reader]))

(def exec (.-execSync chp))

(defn read-config! [path]
  (cljs.reader/read-string
   (.toString
    ((.-readFileSync fs)
     (str path "/shadow-cljs.edn")))))

(defn watched-builds [build-map]
  (->> (map (comp #(.substr % 1) str first) build-map)
       (filter #(not (.match % "repl")))))

(defn build-cljs* [serverless options builds]
  (fn []
    (js/console.log "")
    (js/console.log "shadow-cljs: Compiling Clojurescript!")
    (js/console.log "")
    (doseq [build builds]
      (js/console.log (str "shadow-cljs: Compiling build " build "..."))
      (exec (str "shadow-cljs release " build))
      (js/console.log (str "shadow-cljs: Done compiling build " build "!")))))

(defn build-hooks [serverless options build-map]
  (let [builds (watched-builds build-map)
        build-cljs (build-cljs* serverless options builds)]
    #js{"before:run:run" build-cljs
        "before:package:createDeploymentArtifacts" build-cljs
        "before:deploy:function:packageFunction" build-cljs
        "before:invoke:local:invoke" build-cljs
        "before:offline:start"
        (fn []
          (let [service-path (-> serverless
                                 (aget "config")
                                 (aget "servicePath"))]
            (js/console.log "shadow-cljs: Watching *.cljs files in" service-path)
            (js/console.log "")
            (watch service-path
                   #js {:recursive true
                        :filter (js/RegExp. "\\.cljs$")}
                   (fn [evt name] (when (= "update" evt) (build-cljs))))
            (build-cljs)))}))

(defn Plugin [serverless options]
  (js/console.log "")
  (js/console.log "shadow-cljs: The clouds cast their shadows! Preparing to compile Clojurescript.")
  (js/console.log "shadow-cljs: Be sure to run `shadow-cljs server` in order to speed up builds.")
  (let [service-path (-> serverless
                         (aget "config")
                         (aget "servicePath"))
        build-map (:builds (read-config! service-path))
        ret     #js {:hooks (build-hooks serverless options build-map)}]
    (js/console.log "shadow-cljs: Done initializing shadow-cljs!")
    ret))

(defn repl [])
