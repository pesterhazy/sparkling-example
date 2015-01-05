Flambo example
----------------

A quick example to demonstrate how to use Sparkling (https://github.com/gorillalabs/sparkling). Sparkling allows you to use Apache Spark (https://spark.apache.org/) from Clojure.

The example is a simple Apache log analyzer. It reads an access log ("access.log" in the project directory) and counts how often URLs were requested, sorted by frequency. The example was inspired by a [blog post](http://alvinalexander.com/scala/analyzing-apache-access-logs-files-spark-scala) by Alvin Alexander.

To try it out, the only thing you need is `leiningen`; Spark is pulled in automatically as a dependency.

1. Get a sample access log, e.g.:

        wget -O access.log http://redlug.com/logs/access.log

or
        curl -o access.log http://redlug.com/logs/access.log

2. Run the log analysis

        lein repl
        ...
        sparkling-example.core=> (report-log-entries)

3. Check out the results

        cat output/* | less

Comments welcome

Paulus Esterhazy

@pesterhazy / pesterhazy@gmail.com
