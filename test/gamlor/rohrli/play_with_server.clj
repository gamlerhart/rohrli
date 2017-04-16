(ns gamlor.rohrli.play-with-server
  (:require

    [clojure.test :refer :all]
    [gamlor.rohrli.web_server :as web]
    [gamlor.rohrli.main :as app]
    [clojure.tools.logging :as log]
    [clojure.java.io :as io])
  (:import (sun.security.tools.keytool CertAndKeyGen)
           (sun.security.x509 X500Name X509CertInfo CertificateExtensions BasicConstraintsExtension X509CertImpl)
           (java.security.cert X509Certificate Certificate)
           (java.security PrivateKey KeyStore)))


(println "Test reload replacing handler of Jetty")

(def test-config
  {:url "http://localhost:8080"
   ::web/http-port 8080
   }
  )

(def test-server (atom (app/app-routes)))
(def web-server (web/start-server
                  (fn [req] (@test-server req))
                  test-config))
(def web-server-url "http://localhost:8080")

(defn reload-server []
  (println "Test server start")
  (require '[gamlor.rohrli.main :as a] :reload)
  (reset! test-server (app/app-routes))
  )

(defn sign-cert
  "Based upon http://www.pixelstech.net/article/1406726666-Generate-certificate-in-Java----2"
  [^X509Certificate cert ^X509Certificate issuer ^PrivateKey issuer-private cert-authority]
  (let [^X500Name issuer-name (.getSubjectDN issuer)
        info (X509CertInfo. (.getTBSCertificate cert))]

    (.set info X509CertInfo/ISSUER issuer-name)
    (when cert-authority
      (let [ext (CertificateExtensions.)
            bce (BasicConstraintsExtension. true, -1)]
        (.set ext BasicConstraintsExtension/NAME (BasicConstraintsExtension. false (.getExtensionValue bce)))
        (.set info X509CertInfo/EXTENSIONS ext))
      )
    (let [our-cert (X509CertImpl. info)]
      (.sign our-cert issuer-private (.getSigAlgName issuer))
      our-cert
      )
    )
  )


(comment
  (do
    (gamlor.rohrli.web_server/stop-server)
    (require '[gamlor.rohrli.main :as app] :reload)
    (require '[gamlor.rohrli.web_server :as w] :reload)
    (gamlor.rohrli.web_server/start-server
      (fn [req]
        (log/info "reloading code")
        (require '[gamlor.rohrli.main :as a] :reload)
        ((app/app-routes) req)
        )
      {:url "https://localhost:8081"
       ::web/http-port 8080
       ::web/https-port 8081
       ::web/cert-password "RohrTest123"
       ::web/https-cert "./test/rohrli.pkcs12"
       }
      )))