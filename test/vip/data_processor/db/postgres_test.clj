(ns vip.data-processor.db.postgres-test
  (:require [vip.data-processor.db.postgres :refer :all]
            [clojure.test :refer :all])
  (:import [java.net URLDecoder]))

(deftest build-public-id-test
  (testing "builds public ids with as much information as it has"
    (is (= "2015-06-18-federal-Ohio-4" (build-public-id "2015-06-18" "federal" "Ohio" 4)))
    (is (= "federal-Ohio-4" (build-public-id nil "federal" "Ohio" 4)))
    (is (= "2015-06-18-Ohio-4" (build-public-id "2015-06-18" nil "Ohio" 4)))
    (is (= "2015-06-18-federal-4" (build-public-id "2015-06-18" "federal" nil 4)))
    (is (= "2015-06-18-4" (build-public-id "2015-06-18" nil nil 4)))
    (is (= "federal-4" (build-public-id nil "federal" nil 4)))
    (is (= "Ohio-4" (build-public-id nil nil "Ohio" 4)))
    (is (= "2015-06-18-federal-Ohio-4" (build-public-id "2015-06-18" "federal" "Ohio" 4)))
    (is (= "federal-Ohio-4" (build-public-id "" "federal" "Ohio" 4)))
    (is (= "2015-06-18-Ohio-4" (build-public-id "2015-06-18" "" "Ohio" 4)))
    (is (= "2015-06-18-federal-4" (build-public-id "2015-06-18" "federal" "" 4)))
    (is (= "2015-06-18-4" (build-public-id "2015-06-18" "" "" 4)))
    (is (= "federal-4" (build-public-id "" "federal" "" 4)))
    (is (= "Ohio-4" (build-public-id "" "" "Ohio" 4)))
    (is (= "2015-06-18-federal-Ohio-4" (build-public-id "6/18/2015" "federal" "Ohio" 4)))
    (is (= "federal-Ohio-4" (build-public-id "//////" "federal" "Ohio" 4)))
    (is (= "federal-Ohio-4" (build-public-id "" "federal " "Ohio " 4))))
  (testing "gives an 'invalid' named id if all of date, election-type and state are nil"
    (is (= "invalid-4" (build-public-id nil nil nil 4)))
    (is (= "invalid-4" (build-public-id "" nil "" 4)))))

(deftest generate-public-id-test
  (testing "public ids, being used in URLs, are encoded properly"
    (is (= "2016-11-08-federal-Pima County, Arizona-508"
           (generate-public-id {:date "2016-11-08"
                                :election-type "federal"
                                :state "Pima County, Arizona"
                                :import-id "508"})))))
(deftest coerce-identifier-test
  (testing "coerces valid identifiers"
    (is (= global-identifier (coerce-identifier :global)))
    (is (= (BigDecimal. 4) (coerce-identifier "4")))
    (is (= 5 (coerce-identifier 5)))
    (is (nil? (coerce-identifier nil))))
  (testing "uses the invalid-identifier when given an invalid identifier"
    (is (= invalid-identifier (coerce-identifier :garbage)))
    (is (= invalid-identifier (coerce-identifier '(a list))))
    (is (= invalid-identifier (coerce-identifier "ABC")))))

(deftest election-id-test
  (testing "election-id generation"
   (is (= "2015-10-10-LOUISIANA-GENERAL"
          (build-election-id "2015-10-10" "LOUISIANA" "GENERAL")))
   (is (= "2015-10-10-LOUISIANA-GENERAL"
          (build-election-id "2015-10-10" "LOUISIANA   " "GENERAL")))
   (is (nil? (build-election-id "2015-10-10" "LOUISIANA" nil)))
   (is (nil? (build-election-id "2015-10-10" "" "GENERAL")))))
