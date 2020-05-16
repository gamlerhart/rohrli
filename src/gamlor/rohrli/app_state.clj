(ns gamlor.rohrli.app-state
  "The defs which keep application state around.
  These are seperated out here, so we can hot reload the app code,
  but keep the state alive"
  (:gen-class)
  (:require [clojure.tools.logging :as log])
  (:import (java.util.concurrent Executors ScheduledExecutorService TimeUnit ThreadFactory)
           (java.util.concurrent.atomic AtomicInteger)))

;"Scheduler to maintaing and update pending requests."



(defonce ^AtomicInteger main-site-count (AtomicInteger.))
(defonce ^AtomicInteger created-count (AtomicInteger.))
(defonce ^AtomicInteger timeout-count (AtomicInteger.))
(defonce ^AtomicInteger completed-count (AtomicInteger.))

(defonce ^ScheduledExecutorService sheduler
         (Executors/newScheduledThreadPool 1
                                           (reify ThreadFactory
                                             (newThread [_ r]
                                               (let [t (.newThread (Executors/defaultThreadFactory) r)]
                                                 (doto t (.setDaemon true) (.setName "Background Task scheduler"))
                                                 t
                                                 )
                                               )
                                             )))

(defonce sheduled-job (.scheduleAtFixedRate sheduler
                                            (fn []
                                              (log/info "Stats: "
                                                        {
                                                         "main-site-count" (.get main-site-count)
                                                         "created"         (.get created-count)
                                                         "timeout-count"   (.get timeout-count)
                                                         "completed-count" (.get completed-count)
                                                         })
                                              ) 30 30 TimeUnit/SECONDS))


(def update-pending-request-tasks
  "The actual task which periodically updates pending request.
  Kept in this atom, so that when code get's reloaded, we can stop the old code"
  (atom nil))


(def waiting-request
  "All the requests which are waiting to be downloaded."
  (atom {}))

(def completed-requests
  "The completed requests which the status they completed.
  This allows to give more detailed 'not-found' information"
  (atom {}))

(log/info "Reset/Reloaded all state")