(ns gamlor.rohrli.web_server
  "The web server: Code to set it up, serve static content and handle the requests"
  (:gen-class)
  (:require
    [ring.util.servlet :as servlet]
    [clojure.tools.logging :as log]
    [ring.middleware.cookies :refer [wrap-cookies]])
  (:import (org.eclipse.jetty.server Server Request Handler)
           (org.eclipse.jetty.server.handler ResourceHandler ContextHandler ContextHandlerCollection HandlerList AbstractHandler)
           (java.util UUID)
           (javax.servlet.http HttpServletResponse)))

(defn static-content-handler
  []
  (let [path (.. (Thread/currentThread) (getContextClassLoader) (getResource "web") (toExternalForm))
        resource-handler (new ResourceHandler)
        _ (doto resource-handler
            (.setEtags true)
            (.setDirectoriesListed false)
            (.setWelcomeFiles (into-array ["index.html"]))
            (.setResourceBase path)
            )]
    resource-handler))


(defn- session-cookie
  [id]
  {:cookies {"session" {:value id :max-age 3600 :http-only true}}})

(defn- new-session-cookie
  []
  (session-cookie (str (UUID/randomUUID))))


(defn with-cookies
  [page-handler]
  (fn [request-map]
    (let
      [
       session (get-in request-map [:cookies "session" :value])
       result (page-handler request-map)
       with-session (cond
                      (and result session) result
                      result (merge (new-session-cookie) result)
                      :else nil)]
      with-session)))

(defn our-handler
  [page-handler]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request ^HttpServletResponse response]
      (try
        (let [url (.getRequestURI request)
              start (System/currentTimeMillis)
              full-handler (wrap-cookies (with-cookies page-handler))
              request-map (servlet/build-request-map request)
              response-data (full-handler (merge request-map {::raw-req request ::raw-response response}))]
          (log/info "incomming:" (.getMethod request) url)
          (when response-data
            (if (::is-raw-reponse response-data)
              (do
                (log/info "handled raw respone:" (.getMethod request) url (.getStatus response) " in " (- (System/currentTimeMillis) start) "ms")
                )
              (do
                (servlet/update-servlet-response response response-data)
                (log/info "handled respone:" (.getMethod request) url (.getStatus response) " in " (- (System/currentTimeMillis) start) "ms")
                )
              )
            (.setHandled base-request true)
            ))
        (catch Exception e (do
                             (log/error "Failed request: " e)
                             (.setStatus response 500)
                             (.setHandled base-request true)))))))


(def last-server (atom nil))

(defn stop-server
  []
  (swap! last-server (fn [s] (when s (.stop s)) nil)))


(defn start-server
  ([port page-handler]

   (let [server (new Server port)
         handlers [(our-handler page-handler) (static-content-handler)]
         handler-list (new HandlerList)
         _ (.setHandlers handler-list (into-array Handler handlers))
         _ (. server (setHandler handler-list))
         _ (swap! last-server (fn [s] (when s (.stop s)) server))
         _ (. server (start))]
     server))
  ([page-handler]
   (start-server 8080 page-handler)))


