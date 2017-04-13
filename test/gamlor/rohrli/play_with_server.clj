(ns gamlor.rohrli.play-with-server
  (:require

    [clojure.test :refer :all]
    [gamlor.rohrli.web_server :as web]
    [gamlor.rohrli.main :as app]
    [clojure.tools.logging :as log]))


(println "Test reload replacing handler of Jetty")

(def test-server (atom (app/app-routes)))
(def web-server (web/start-server
                  (fn [req] (@test-server req))))
(def web-server-url "http://localhost:8080")

(defn reload-server []
  (println "Test server start")
  (require '[gamlor.rohrli.main :as a] :reload)
  (reset! test-server (app/app-routes))
  )

(comment
  (do
    (gamlor.rohrli.web_server/stop-server)
    (require '[gamlor.rohrli.main :as app] :reload)
    (require '[gamlor.rohrli.web_server :as w] :reload)
    (gamlor.rohrli.web_server/start-server 8080
                                         (fn [req]
                                           (log/info "reloading code")
                                           (require '[gamlor.rohrli.main :as a] :reload)
                                           ((app/app-routes) req)
                                           ))))