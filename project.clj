(defproject sparkling-example "0.1.0-SNAPSHOT"
  :description "Example of using Gorillalabs Sparkling (Spark with Clojure)

The projects demonstrates Sparkling/Spark by parsing apache access log.

Inspired by http://alvinalexander.com/scala/analyzing-apache-access-logs-files-spark-scala
"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [gorillalabs/sparkling "1.0.0-SNAPSHOT"]
                 [org.apache.spark/spark-core_2.10 "1.1.1"]
                 [clj-time "0.8.0"]
                 [org.clojure/tools.trace "0.7.8"]
                 [clj-glob "1.0.0"]
                 ]
  :jvm-opts ["-Xmx2g"]
  :main ^:skip-aot sparkling-example.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:aot [sparkling-example.core]}})
