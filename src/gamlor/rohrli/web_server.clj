(ns gamlor.rohrli.web_server
  "The web server: Code to set it up, serve static content and handle the requests"
  (:gen-class)
  (:require
    [ring.util.servlet :as servlet]
    [clojure.tools.logging :as log]
    [clojure.java.io :as io])
  (:import (org.eclipse.jetty.server Server Request Handler Connector ServerConnector HttpConnectionFactory)
           (org.eclipse.jetty.server.handler ResourceHandler ContextHandler ContextHandlerCollection HandlerList AbstractHandler)
           (java.util UUID)
           (javax.servlet.http HttpServletResponse)
           (java.net InetSocketAddress)
           (org.eclipse.jetty.util.ssl SslContextFactory)))

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



(defn our-handler
  [page-handler]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request ^HttpServletResponse response]
      (try
        (let [url (.getRequestURI request)
              start (System/currentTimeMillis)
              request-map (servlet/build-request-map request)
              response-data (page-handler (merge request-map {::raw-req request ::raw-response response}))]
          (log/info "incomming:" (.getMethod request) url)
          (when response-data
            (if (::is-raw-response response-data)
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

(defn https-only-handler
  [config]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request ^HttpServletResponse response]
      (if (.isSecure request)
        (do
          (.setHeader response "Strict-Transport-Security" "max-age=31536000")
          nil)
        (do
          (.setStatus response 301)
          (.setHeader response "Location" (str (:url config) (.getRequestURI request)))
          (.setHandled base-request true)
          )
        ))))

(def last-server (atom nil))

(defn stop-server
  []
  (swap! last-server (fn [s] (when s (.stop s)) nil)))

(defn- jetty-ssl-context
  [key-store-file password]

  (log/info "Looking at key file? " key-store-file)
  (when (not (.exists (io/file key-store-file)))
    (log/error "Key file does not exist? " key-store-file)
    )
  (let [ssl-factory (new SslContextFactory)]
    (.setKeyStorePath ssl-factory key-store-file)
    (.setKeyStorePassword ssl-factory password)
    (.setKeyManagerPassword ssl-factory password)
    ssl-factory))

(defn- jetty-http
  [server {port ::http-port}]
  (let [connector (new ServerConnector
                       server
                       )]
    (.setPort connector port)
    connector))

(defn- jetty-https
  [server {port ::https-port cert ::https-cert password ::cert-password}]
  (let [connector (new ServerConnector
                       server
                       (jetty-ssl-context cert password))]
    (.setPort connector port)
    connector))

(defn start-server
  ([page-handler config]
   (let [use-https (and (::https-cert config) (::cert-password config))
         server (new Server)
         general-handlers [(our-handler page-handler) (static-content-handler)]
         handlers (if use-https
                    (cons (https-only-handler config) general-handlers)
                    general-handlers)
         handler-list (new HandlerList)
         connectors (if use-https
                      [(jetty-https server config) (jetty-http server config)]
                      [(jetty-http server config)]
                      )
         _ (.setConnectors server (into-array Connector connectors))
         _ (.setHandlers handler-list (into-array Handler handlers))
         _ (. server (setHandler handler-list))
         _ (swap! last-server (fn [s] (when s (.stop s)) server))
         _ (. server (start))]
     server)))



