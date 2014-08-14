(ns flambo-example.core
  (:require  [flambo.api :as f]
             [flambo.conf :as fconf]
             [clj-time.format :as tf]
             [clj-time.core :as tc]
             [clojure.tools.trace :refer [trace]]
             [clojure.java.shell :refer [sh]]
             [clojure.pprint :refer [pprint]]
             [org.satta.glob :refer [glob]]
             ) 
  (:gen-class))

(def master "local[*]")
(def conf {})
(def env {
          "spark.executor.memory" "4G",
          "spark.files.overwrite" "true"
          })

(defn new-spark-context []
  (let [c (-> (fconf/spark-conf)
              (fconf/master master)
              (fconf/app-name "tfidf")
              (fconf/set "spark.akka.timeout" "300")
              (fconf/set conf)
              (fconf/set-executor-env env))]
    (f/spark-context c) ))

(defonce sc (delay (new-spark-context)))

;; parsing apache logs
;;

(def testline
  "87.161.251.240 - - [22/Jun/2014:02:20:03 +0200] \"GET /blubb?key=value HTTP/1.0\" 200 13751 \"http://blublii.net/\" \"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.153 Safari/537.36\"")

(defn transform-log-entry [m]
  (->
    m
    (update-in [:timestamp] #(tf/parse (tf/formatter "dd/MMM/yyyy:HH:mm:ss Z") %))
    (assoc :uri (-> (:request m)
                    (clojure.string/split #" ")
                    (get 1)
                    (clojure.string/split #"\?")
                    (get 0)))))

(defn parse-line [line]
  (some->> line
    (re-matches #"^(.*?) .*? .*? \[(.*?)] \"(.*?)\" (.*?) (.*?) \"(.*?)\" \"(.*?)\"(.*?)$")
    rest
    (zipmap [:ip :timestamp :request :status :length :referer :ua :duration])
    transform-log-entry))

(defn process-log-entries [in out]
  (let [
        lines (f/text-file @sc in)
        ]
    (-> lines
        (f/map (f/fn [line] (parse-line line)))
        (f/filter (f/fn [entry] (= "200" (:status entry))))
        (f/map (f/fn [entry] [(:uri entry) 1]))
        (f/reduce-by-key (f/fn [a b] (+ a b)))
        (f/map (f/fn [[a b]] [b a]))
        (f/sort-by-key false)
        (f/map (f/fn [[a b]] [b a]))
        (f/map (f/fn [xs] (clojure.string/join "\t" xs)))
        (f/save-as-text-file out)
        )))

;; call these from the REPL
;;

(defn report-log-entries []
  (let [in "access.log"
        out "output"]
    (sh "rm" "-rf" out)
    (process-log-entries in out))) 
