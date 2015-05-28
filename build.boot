(set-env!
  :dependencies 
    '[[me.raynes/fs "1.4.6"]
      [ring/ring-core "1.3.2"]
      [ring/ring-jetty-adapter "1.3.2"]])

(ns boot.user
  (:require [me.raynes.fs :as fs]
            [ring.util.response     
              :refer [file-response response redirect not-found header]]
            [ring.adapter.jetty     :as jetty]
            [clojure.string :as string]))

(deftask simple
  "Simple example task"
  []
  (with-pre-wrap fileset
      (let [processed 
             (map #(string/upper-case %1) fileset)]
      (commit! processed))))

(defn mv-uc
  "Moves all of the files in fileset to upper-case versions"
  [fileset]
  (loop [files (:tree fileset)
         fs fileset]
    (if-let [[source fileobj] (first files)]
      (let [parts (string/split (str source) #"/")
            base (last parts)
            parent (butlast parts)
            dest (string/join "/" (concat parent [(string/upper-case base)]))]
         (recur 
           (dissoc files source)
           (mv fs source dest)))
      fs)))
        
(deftask uc-filenames
  []
  (fn middleware [next-handler]
      (fn handler [fileset]
        (next-handler (mv-uc fileset)))))

(deftask cli-example
  "This is the help text for this task"
  [f foo FOO str "The foo option."
   b bar int "The bar option - incrementer"
   c compound KEY:VAL {kw str} "A compound option"]
   
   (prn *opts*)
   (prn *args*))

(defn mapper-app
  "Given a map of relative paths to temporary locations, serve 
   the files within if they are requested"
  [mapping]
  (fn [request]
    (let [uri (subs (:uri request) 1)
          want (get mapping uri)]
      (if want
        (file-response (tmp-path want)
        (not-found "Not Found"))))))

(deftask serve-source
  "Serve all files in the source tree"
  [p port PORT int "The port to listen on"]
  (fn middleware [next-handler]
    (fn handler [fileset]
      (jetty/run-jetty 
        (mapper-app (:tree fileset)) 
        {:port (get *opts* :port 8080)}))))