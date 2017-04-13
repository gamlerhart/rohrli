(defproject gamlor.rohrli.server "0.1.0-SNAPSHOT"
  :description "Röhrli. Pipe your files to the web"
  :url "https://rohrli.gamlor.info/"
  :license {:name "Mozilla Public License Version 2.0"
            :url  "https://www.mozilla.org/en-US/MPL/2.0/"}
  :source-paths ["src"]
  :java-source-paths ["src"]
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-servlet "1.4.0"]
                 [org.eclipse.jetty/jetty-server "9.3.13.v20161014"]
                 [org.eclipse.jetty/jetty-servlet "9.3.13.v20161014"]
                 [compojure "1.4.0"]
                 [enlive "1.1.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7"]   ;; More logging
                 ]

  :main ^:skip-aot gamlor.rohrli.main
  :target-path "target/%s"
  :uberjar-name "rohrli.jar"
  :jvm-opts ["-Xmx64m"]
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[org.clojure/test.check "0.9.0"]
                                      [clj-http "2.2.0"]]}})