(defproject flambo-example "0.1.0-SNAPSHOT"
  :description "Example of using Flambo (Spark with Clojure)

The projects demonstrates Flambo (Spark) by parsing apache access log.

Inspired by http://alvinalexander.com/scala/analyzing-apache-access-logs-files-spark-scala
"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [yieldbot/flambo "0.3.2"]
                 [org.apache.spark/spark-core_2.10 "1.0.1"]
                 [org.apache.spark/spark-streaming_2.10 "1.0.1"]
                 [org.apache.spark/spark-streaming-kafka_2.10 "1.0.1"]
                 [org.apache.spark/spark-sql_2.10 "1.0.1"]
                 [clj-time "0.8.0"]
                 [org.clojure/tools.trace "0.7.8"]
                 [clj-glob "1.0.0"]
                 ]
  :jvm-opts ["-Xmx2g"]
  :main ^:skip-aot flambo-example.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
