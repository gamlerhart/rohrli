(ns gamlor.rohrli.main-tests
  (:require
    [clojure.test :refer :all]
    [gamlor.rohrli.main :as m])
  (:import (java.util Random)
           (java.security MessageDigest)
           (gamlor.rohrli Encoding)))

(prn (m/random-link-id))

(defn estimate-bits [l] (/ (Math/log l) (Math/log 2)))
(def estimate-bits-sylable (+ (estimate-bits m/consonats-length)
                              (estimate-bits m/vowels-length)
                              (estimate-bits m/consonats-length)
                              (estimate-bits m/vowels-length)
                              (estimate-bits m/consonats-length)))

(def estimate-bits-link (* m/syllable-count estimate-bits-sylable))
(prn estimate-bits-link)

(deftest may-link-ids-no-collision
  (let [ids (vec (repeatedly 1000 m/random-sylable))
        expected-count (count ids)]
    (is true)))


(deftest fill-templates
  (is (= "no-change" (m/fill-template "no-change" {})))
  (is (= "no-change template-keyword" (m/fill-template "no-change template-keyword" {:template-keyword "value"})))
  (is (= "no-change value" (m/fill-template "no-change {template-keyword}" {:template-keyword "value"})))
  (is (= "no-change mutli value" (m/fill-template "no-change {template-1} {template-2}" {:template-1 "mutli" :template-2 "value"})))
  )

