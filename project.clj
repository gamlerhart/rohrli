(defproject gamlor.rohrli.server "0.1.0-SNAPSHOT"
  :description "Röhrli. Pipe your files to the web"
  :url "https://rohrli.gamlor.info/"
  :license {:name "Mozilla Public License Version 2.0"
            :url  "https://www.mozilla.org/en-US/MPL/2.0/"}
  :source-paths ["src"]
  :java-source-paths ["src"]
  :dependencies [
                 [org.clojure/clojure "1.10.1"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-servlet "1.8.0"]
                 [org.eclipse.jetty/jetty-server "9.4.23.v20191118"]
                 [org.eclipse.jetty/jetty-servlet "9.4.23.v20191118"]
                 [compojure "1.6.1"]
                 [enlive "1.1.6"]
                 [org.clojure/tools.logging "0.5.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]   ;; More logging
                 ]

  :main ^:skip-aot gamlor.rohrli.main
  :target-path "target/%s"
  :uberjar-name "rohrli.jar"
  :jvm-opts ["-Xmx64m"]
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[org.clojure/test.check "0.10.0"]
                                      [clj-http "3.10.0"]]}})