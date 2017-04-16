(ns gamlor.rohrli.server-tests
  (:require
    [clojure.test :refer :all]
    [clj-http.client :as http]
    [gamlor.rohrli.play-with-server :as server]
    [clojure.string :as str])
  (:import (java.util Random)
           (java.security MessageDigest)
           (gamlor.rohrli Encoding)
           (org.apache.http.impl.client HttpClients CloseableHttpClient)
           (org.apache.http.client.methods HttpPost)
           (org.apache.http.entity ByteArrayEntity)))

(server/reload-server)


(def rnd (Random.))
(def ^int max-size (* 4 1024))

(defn- new-bytes []
  (let [
        size (.nextInt rnd max-size)
        bytes (byte-array size)]
    (.nextBytes rnd bytes)
    bytes
    )
  )

(defn sha256 [^bytes bytes]
  (let [hasher (MessageDigest/getInstance "SHA-256")
        hash (.digest hasher bytes)]
    (Encoding/bytesToHex hash)
    )
  )

(def ^CloseableHttpClient http-client (HttpClients/createDefault))
(defn stream-post [^String url ^bytes body]
  (let [post (doto (HttpPost. url)
               (.setEntity (ByteArrayEntity. body)))
        resp (.execute http-client post)
        link (.getValue (.getFirstHeader resp "Location"))]
    link
    )
  )

(defn not-found [accept]
  (let [
        link (str server/web-server-url "/outout-dated-outout-dated")
        resp (http/get link
                       {:accept accept :throw-exceptions false})
        body (:body resp)]
    (is (= 404 (:status resp)))
    (is (str/includes? body link))
    (is (str/includes? body "We didn't find it anywhere"))
    ))


(defn already-download [link accept]
  (let [
        resp (http/get link
                       {:accept accept :throw-exceptions false})
        body (:body resp)]
    (is (= 410 (:status resp)))
    (is (str/includes? body link))
    (is (str/includes? body "You already downloaded this pipe"))
    ))

(deftest not-found-link
  (not-found "text/html")
  (not-found "*/*")
  )


(deftest upload-download-pair
  (let [body (new-bytes)
        hash (sha256 body)
        link (stream-post server/web-server-url body)
        download (:body (http/get link {:as :byte-array}))]
    (is (= hash (sha256 download)))
    (already-download link "text/html")
    (already-download link "*/*")
    )
  )

(deftest holds-content-length
  (let [body (new-bytes)
        hash (sha256 body)
        link (stream-post server/web-server-url body)
        content-length (-> (http/get link {:as :byte-array}) :headers (get "Content-Length"))]
    (is (= (str (alength body)) content-length))
    )
  )

