(ns vip.data-processor.validation.data-spec.value-format-test
  (:require [clojure.test :refer :all]
            [vip.data-processor.validation.data-spec.value-format :refer :all]))

(deftest electoral-district-type-regex-test
  (let [electoral-district-type-regex (:check electoral-district-type)]
    (testing "matches a lot of expected values"
      (are [s] (re-matches electoral-district-type-regex s)
        "state"
        "statewide"
        "STATEWIDE"
        "u.s. senate"
        "us rep"
        "u.s. house"
        "congressional"
        "state senate"
        "ut senate"
        "house"
        "county"
        "countywide"
        "school member district"
        "town"
        "township"
        "sanitary"))
    (testing "does not match any random string"
      (are [s] (nil? (re-matches electoral-district-type-regex s))
        "parliament"
        "prom court"
        "Kang v Kodos"))))

(deftest phone-regex-test
  (let [phone-regex (:check phone)]
    (testing "matches a lot of expected values"
      (are [s] (re-find phone-regex s)
        "(555) 555-5555"
        "555-555-5555"
        "555.555.5555"
        "+55 555 555 5555"
        "(555) 555 5555 x123"
        "Call this number: (555) 555 5555 x123"))
    (testing "does not match nonsense"
      (are [s] (nil? (re-find phone-regex s))
        "123456"
        "call me"
        "Call this number: (555)"))))