(set-env!
  :dependencies '[[me.raynes/fs "1.4.6"]])

(ns boot.user
  (:require [me.raynes.fs :as fs]
            [clojure.string :as string]))

(deftask simple
  "Simple example task"
  []
  (with-pre-wrap fileset
      (let [processed 
             (map #(string/upper-case %1) fileset)]
      (commit! processed))))