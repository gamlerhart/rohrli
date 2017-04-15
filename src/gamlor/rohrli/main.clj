(ns gamlor.rohrli.main
  (:gen-class)
  (:require [compojure.core :refer :all]
            [ring.util.response :as r]
            [gamlor.rohrli.web_server :as web]
            [gamlor.rohrli.app-state :as state]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import (java.io InputStream ByteArrayInputStream PrintWriter FilterInputStream)
           (java.nio.charset StandardCharsets)
           (javax.servlet.http HttpServletRequest HttpServletResponse)
           (java.security SecureRandom)
           (java.util.concurrent ScheduledExecutorService Executors TimeUnit ScheduledFuture)
           (javax.servlet DispatcherType AsyncContext)
           (java.time Duration ZonedDateTime ZoneOffset)
           (org.apache.commons.fileupload.servlet ServletFileUpload)
           (org.apache.commons.fileupload FileItemIterator FileItemStream)))

(def server-url-env-var "SERVER_URL")
(def ^:dynamic service-url (or (System/getenv server-url-env-var) "http://localhost:8080"))

(defn- response-type
  [headers]
  (if (str/includes? (get headers "accept" "") "html")
    ::html-resp
    ::curl-resp))

(defn fill-template
  "Fill out a text template with the provided map values.
  In template, expect the place holder to be set in curly braces, like {key}"
  [template values]
  (reduce
    (fn [txt kv]
      (str/replace txt (str "{" (-> kv first name) "}") (-> kv second))
      ) template values)
  )

(def template-curl-guide
  (fill-template (slurp (io/resource "curl/guide-snippet.txt")) {:service-url service-url}))

(defn template-from-resource
  "Load a class path resource given the name.
  Fills out the :service-url and :guide place holders"
  [resource]
  (fill-template (slurp (io/resource resource)) {:service-url service-url :guide template-curl-guide})
  )

(defn response-template [resource]
  "Most respononses have a seperate html/curl response.
  This loads both for a given name"
  {
   ::curl-resp (template-from-resource (str "curl/" resource ".txt"))
   ::html-resp (template-from-resource (str "web/" resource ".html"))
   }
  )

(def template-index {
                     ::curl-resp (template-from-resource (str "curl/hello.txt"))
                     ::html-resp nil
                     })
(defn- response-from-template [template headers values]
  (fill-template (get template (response-type headers)) values)
  )

(def response-types {::curl-resp "plain/text"
                     ::html-resp "text/html"})

(defn- response-mime [headers]
  (get response-types (response-type headers))
  )

(def template-404 (response-template "404"))
(def template-downloaded (response-template "done-downloaded"))
(def template-expired (response-template "done-expired"))
(def template-created (response-template "fetch-url"))
(def template-download-started (response-template "request-timeout"))
(def template-download-complete (response-template "download-complete"))
(def template-progress {
                        ::curl-resp "."
                        ::html-resp " "
                        })

(def random (new SecureRandom))
(def vowels (.toLowerCase "AEIOUY"))
(def vowels-length (.length vowels))
(def consonats (.toLowerCase "BCDFGHKLMNPQRSTUVWXZ"))
(def consonats-length (.length consonats))
(def syllable-count 4)

(defn random-vowel [] (.charAt vowels (.nextInt random vowels-length)))
(defn random-consonant [] (.charAt consonats (.nextInt random consonats-length)))
(defn random-sylable []
  (str (random-consonant) (random-vowel) (random-consonant) (random-consonant) (random-vowel) (random-consonant))
  )
(defn random-link-id []
  (str/join "-" (repeatedly syllable-count random-sylable))
  )

(def async-timeout (Duration/ofMinutes 15))
(def end-request-timeout (Duration/ofMinutes 10))

(defn complete-request
  "Mark a request as completed: Remove it from the state/waiting-request map,
  adding it to state/completed-requests with the given reason"
  [link-id reason]
  (swap! state/completed-requests
         (fn [current]
           (assoc current link-id reason)))
  (swap! state/waiting-request
         (fn [current]
           (dissoc current link-id)))
  )

(defn ping-waiting-request []
  "Sends a pulse to the pending requests, so they stay alive.
  Also ensures that CURL does not close the request.
  Plus provide a nice feedback to the user that we're waiting for the download"
  (let [
        timeout-cutoff (.minus (ZonedDateTime/now ZoneOffset/UTC) end-request-timeout)
        to-ping @state/waiting-request]
    (log/info "Triggered pending request updates. Pending requests " (count to-ping))

    (doseq [[link-id
             {^PrintWriter writer       ::writer
              ^HttpServletResponse resp ::response
              ^ZonedDateTime started    ::start-time
              ^AsyncContext context     ::async-context
              type                      ::response-type}] to-ping]
      (if (.isAfter started timeout-cutoff)
        (do
          (.write writer (str (type template-progress)))
          (.flush writer)
          (.flushBuffer resp)
          )
        (do
          (log/info "Request" link-id "timed out. Completing it")
          (.write writer (str (type template-download-started)))
          (.close writer)
          (.complete context)
          (complete-request link-id ::expired)
          (.incrementAndGet state/timeout-count)
          )
        )
      )
    )
  )

; Install the ping-waiting-request handler.
; We stop the old handler, in case we reloaded this code
(swap! state/update-pending-request-tasks
       (fn [^ScheduledFuture old]
         (when old
           (.cancel old true))
         (.scheduleAtFixedRate state/sheduler ping-waiting-request 5 5 TimeUnit/SECONDS)
         ))

(defn- first-file-upload
  "Runs through iterator and returns the first file stream.
  Mutates the java interator passed in"
  [^FileItemIterator iter]
  (if-let [^FileItemStream item (and (.hasNext iter) (.next iter))]
    (if-let [file (and (not (.isFormField item)) (.openStream item))]
      file
      (first-file-upload iter)
      )
    nil
    )
  )


(defn pipe-reader [^HttpServletRequest req]
  (if (ServletFileUpload/isMultipartContent req)
    (do
      (let [upload (ServletFileUpload.)
            iter (.getItemIterator upload req)]
        (or
          (first-file-upload iter)
          (do
            (log/info "No file found in upload. Returning empty stream")
            (ByteArrayInputStream. (byte-array 0))
            ))
        )
      )
    (.getInputStream req)
    )
  )

(defn start-upload
  "Handle the upload request.
  Give initial info and then place the request into the state/waiting-request maps"
  [^HttpServletRequest req ^HttpServletResponse resp response-type]
  (.setContentType resp "text/plain;charset=utf-8")
  (let [link-id (random-link-id)
        url (str service-url "/" link-id)
        ^String text (fill-template (response-type template-created) {:download-url url})
        writer (.getWriter resp)]
    (.setHeader resp "Content-Type" (response-type response-types))
    (.setHeader resp "Location" url)
    (.setStatus resp 201)
    (.write writer text)
    (.flush writer)
    (.flushBuffer resp)
    (let [async (.startAsync req)
          reader (pipe-reader req)]
      (.setTimeout async (.toMillis async-timeout))
      (swap! state/waiting-request
             (fn [current]
               (merge
                 {link-id
                  {::start-time    (ZonedDateTime/now ZoneOffset/UTC)
                   ::reader        reader
                   ::writer        writer
                   ::response      resp
                   ::async-context async
                   ::response-type response-type}}
                 current)
               ))
      (.incrementAndGet state/created-count)
      )
    )
  )

(defn start-download [pending link-id]
  (complete-request link-id ::downloaded)
  (.incrementAndGet state/completed-count)
  (let [{^PrintWriter writer       ::writer
         ^HttpServletResponse resp ::response
         ^InputStream reader       ::reader
         ^AsyncContext context     ::async-context
         type                      ::response-type} pending]
    (.write writer "\n")
    (.write writer "Download started!\n")
    (.flush writer)
    (.flushBuffer resp)

    {
     :headers {
               "Content-Type"        "application/octet-stream"
               "Content-Disposition" (str "attachment; filename=\"" link-id ".bin\"")
               }
     :body
              (proxy [java.io.FilterInputStream] [reader]
                (close []
                  (.write writer (str (type template-download-complete)))
                  (.complete context)
                  (.close reader)
                  )
                )
     }
    )
  )

(defn not-found-handling [link-id headers]
  (case
    (get @state/completed-requests link-id ::not-found)
    ::not-found {
                 :status  404
                 :headers {"Content-Type" (response-mime headers)}
                 :body    (response-from-template
                            template-404
                            headers
                            {:request-url (str service-url "/" link-id)})
                 }

    ::downloaded {
                  :status  410
                  :headers {"Content-Type" (response-mime headers)}
                  :body    (response-from-template
                             template-downloaded
                             headers
                             {:request-url (str service-url "/" link-id)})
                  }

    ::expired {
               :status  410
               :headers {"Content-Type" (response-mime headers)}
               :body    (response-from-template
                          template-expired
                          headers
                          {:request-url (str service-url "/" link-id)})
               }
    )
  )

(defn app-routes []
  (routes
    (GET "/" {headers :headers}
      (.getAndIncrement state/main-site-count)
      (response-from-template template-index headers {})
      )
    (POST "/" {headers :headers req ::web/raw-req resp ::web/raw-response}
      (start-upload req resp ::curl-resp)
      {::web/is-raw-reponse true}
      )
    (POST "/browser-upload" {headers :headers req ::web/raw-req resp ::web/raw-response}
      (start-upload req resp ::html-resp)
      {::web/is-raw-reponse true}
      )
    (GET "/:link-id" {headers :headers {link-id :link-id} :params}
      (if-let [pending (get @state/waiting-request link-id)]
        (start-download pending link-id)
        (not-found-handling link-id headers)
        )
      )
    )
  )

(defn -main
  [& args]
  (println "starting. Arguments: " args)
  (log/info "Using url " service-url " Change url by setting enviroment variable " + server-url-env-var)
  (let [server
        (web/start-server 8080
                          (app-routes))
        ]
    (while (= 0 (.available (System/in)))
      (Thread/sleep 1000))
    (.stop server))
  )