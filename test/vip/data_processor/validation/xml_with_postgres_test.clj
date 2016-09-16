(ns vip.data-processor.validation.xml-with-postgres-test
  (:require [clojure.test :refer :all]
            [vip.data-processor.test-helpers :refer :all]
            [vip.data-processor.validation.xml :refer :all]
            [vip.data-processor.validation.v5.email :as v5.email]
            [vip.data-processor.pipeline :as pipeline]
            [vip.data-processor.db.postgres :as psql]
            [squishy.data-readers]
            [korma.core :as korma]))

(use-fixtures :once setup-postgres)
(use-fixtures :each with-clean-postgres)

(deftest ^:postgres load-xml-ltree-test
  (testing "imports xml values into postgres"
    (let [ctx {:input (xml-input "v5_sample_feed.xml")
               :pipeline [psql/start-run
                          load-xml-ltree]}
          out-ctx (pipeline/run-pipeline ctx)]
      (is (= (-> (korma/select psql/xml-tree-values
                   (korma/where {:results_id (:import-id out-ctx)})
                   (korma/where (psql/ltree-match psql/xml-tree-values
                                                  :path
                                                  "VipObject.0.Source.*{1}.VipId.*{1}")))
                 first
                 :value)
             "51")))))

(deftest ^:postgres validate-emails-test
  (testing "adds errors to the context for badly formatted emails"
    (let [ctx {:input (xml-input "v5-bad-emails.xml")
               :pipeline [psql/start-run
                          load-xml-ltree
                          v5.email/validate-emails]}
          out-ctx (pipeline/run-pipeline ctx)]
      (is (get-in out-ctx [:errors :email "VipObject.0.Person.0.ContactInformation.0.Email.0" :format]))
      (is (get-in out-ctx [:errors :email "VipObject.0.Person.1.ContactInformation.0.Email.0" :format]))
      (testing "but not for good emails"
        (is (nil? (get-in out-ctx [:errors :email "VipObject.0.Person.2.ContactInformation.0.Email.0" :format])))))))
