(ns vip.data-processor.output.tree-xml-test
  (:require [vip.data-processor.output.tree-xml :refer :all]
            [clojure.test :refer :all]
            [vip.data-processor.pipeline :as pipeline]))

;;; TODO: make a better test
(deftest ^:temporary pipeline-test
  (let [ctx {:spec-version "5.1.2"
             :import-id 4
             :pipeline pipeline}
        out-ctx (pipeline/run-pipeline ctx)]
    (is (= (-> out-ctx
               :xml-output-file
               .toFile
               slurp)
           "<?xml version=\"1.0\"?>\n<VipObject xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" schemaVersion=\"5.1.2\" xsi:noNamespaceSchemaLocation=\"http://votinginfoproject.github.com/vip-specification/vip_spec.xsd\">\n<Candidate id=\"can001\"><Name>Frank</Name><Party>Every day</Party><Title><Text language=\"en\">President</Text><Text language=\"es\">&quot;El&quot; Presidente</Text></Title><Nickname>&gt; Ezra</Nickname></Candidate><Contest id=\"con001\"></Contest></VipObject>"))))
