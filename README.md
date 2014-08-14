Flambo example
----------------

A quick example to demonstrate how to use Flambo (https://github.com/yieldbot/flambo). Flambo allows you to use Apache Spark (https://spark.apache.org/) from Clojure.

The example is a simple Apache log analyzer. It reads an access log ("access.log" in the project directory) and counts how often URLs were requested, sorted by frequency.

To try it out, the only thing you need is `leiningen`; Spark is pulled in automatically as a dependency:

1. Get a sample access log:

        wget -O access.log http://redlug.com/logs/access.log

2. Run the log analysis

        lein repl
        ...
        flambo-example.core=> (report-log-entries)

3. Check out the results

        cat output/* | less

Comments welcome

Paulus Esterhazy

@pesterhazy / pesterhazy@gmail.com
