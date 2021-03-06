(ns sparkling-example.core
  (:require  [sparkling.api :as s]
             [sparkling.conf :as s-conf]
             [sparkling.destructuring :as sd]
             [clj-time.format :as tf]
             [clojure.tools.trace :refer [trace]]
             [clojure.java.shell :refer [sh]]
             [clojure.pprint :refer [pprint]]
             [org.satta.glob :refer [glob]]
             [com.brainbot.iniconfig :refer [read-ini]]) 
  (:gen-class))

(def master "local[*]")
(def conf {})
(def env {
          "spark.executor.memory" "4G",
          "spark.files.overwrite" "true"
          })

(defn new-spark-context-aws
  "Creates a new spark context with access to your AWS credentials"
  []
  (let [aws-config (get (read-ini (str (System/getProperty "user.home") "/.aws/credentials")) "default")
        access-key (get aws-config "aws_access_key_id")
        secret-key (get aws-config "aws_secret_access_key")
        c (-> (s-conf/spark-conf)
              (s-conf/master master)
              (s-conf/app-name "tfidf")
              (s-conf/set "spark.akka.timeout" "300")
              (s-conf/set conf)
              (s-conf/set-executor-env env))
        context (s/spark-context c)] 
    (.set (.hadoopConfiguration context) "fs.s3n.awsAccessKeyId" access-key)
    (.set (.hadoopConfiguration context) "fs.s3n.awsSecretAccessKey" secret-key)
    context))

(defn new-spark-context []
  (let [c (-> (s-conf/spark-conf)
              (s-conf/master master)
              (s-conf/app-name "sparkling")
              (s-conf/set "spark.akka.timeout" "300")
              (s-conf/set conf)
              (s-conf/set-executor-env env))]
    (s/spark-context c) ))

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
  (let [lines (s/text-file @sc in)]
    (-> lines
        (s/map parse-line)
        (s/filter (fn [entry] (= "200" (:status entry))))
        (s/map-to-pair (fn [entry] (s/tuple (:uri entry) 1)))
        (s/reduce-by-key (fn [a b] (+ a b)))
        (s/map-to-pair (sd/key-value-fn (fn [a b] (s/tuple b a))))
        (s/sort-by-key false)
        (s/map-to-pair (sd/key-value-fn (fn [a b] (s/tuple b a))))
        (s/map (sd/key-value-fn (fn [& xs] (clojure.string/join "\t" xs))))
        (s/save-as-text-file out))))

;; call these from the REPL
;;

(defn report-log-entries
  ([] (report-log-entries "in.log"))
  ([in] (let [out "output"]
          (sh "rm" "-rf" out)
          (process-log-entries in out)))) 

(defn line-count [lines]
  (->> lines
       count))

(defn line-count* [lines]
  (->> lines
       s/count))

(defn group-by-status-code [lines]
  (->> lines
       (map parse-line)
       (map (fn [entry] [(:status entry) 1]))
       (reduce (fn [a [k v]] (update-in a [k] #((fnil + 0) % v))) {})
       (map identity)))

(defn group-by-status-code* [lines]
  (-> lines
       (s/map parse-line)
       (s/map-to-pair (fn [entry] (s/tuple (:status entry) 1)))
       (s/reduce-by-key +)
       (s/map (sd/key-value-fn vector))
       (s/collect)))

(defn top-errors [lines]
  (->> lines
       (map parse-line)
       (filter (fn [entry] (not= "200" (:status entry))))
       (map (fn [entry] [(:uri entry) 1]))
       (reduce (fn [a [k v]] (update-in a [k] #((fnil + 0) % v))) {})
       (sort-by val >)
       (take 10)))

(defn top-errors* [lines]
  (-> lines
      (s/map parse-line)
      (s/filter (fn [entry] (not= "200" (:status entry))))
      (s/map-to-pair (fn [entry] (s/tuple (:uri entry) 1)))
      (s/reduce-by-key +)
      ;; flip
      (s/map-to-pair (sd/key-value-fn (fn [a b] (s/tuple b a))))
      (s/sort-by-key false) ;; descending order
      ;; flip
      (s/map-to-pair (sd/key-value-fn (fn [a b] (s/tuple b a))))
      (s/map (sd/key-value-fn vector))
      (s/take 10)))

(defn process [f]
  (with-open [rdr (clojure.java.io/reader "in.log")]
    (let [result (f (line-seq rdr))]
      (if (seq? result)
        (doall result)
        result))))

(defn process* [f]
  (let [lines-rdd (s/text-file @sc "in.log")]
    (f lines-rdd)))
