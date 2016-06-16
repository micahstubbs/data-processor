(ns vip.data-processor.output.tree-xml-test
  (:require [vip.data-processor.output.tree-xml :refer :all]
            [clojure.test :refer :all]
            [vip.data-processor.pipeline :as pipeline]))

(deftest attr-test
  (testing "returns the attribute of a path when it is present"
    (is (= "language"
           (attr "Root.0.Tag.0.Text.0.language"))))
  (testing "returns `nil` when the path doens't have an attribute"
    (is (nil? (attr "Root.0.Tag.0.Thingy.1")))))

(deftest tags-with-indexes-test
  (testing "indexes are included"
    (is (= '("Root.0" "Tag.0" "Thingy.1")
           (tags-with-indexes "Root.0.Tag.0.Thingy.1"))))

  (testing "attributes are not included"
    (let [path "VipObject.0.Candidate.0.BallotName.0.Text.0.language"
          result (set (tags-with-indexes path))]
      (is (false? (contains? result "language"))))))

(deftest same-tag?-test
  (testing "wasn't born yesterday"
    (is (false? (same-tag? "Root.0.Tag.0" "Toor.0.Gat.0"))))

  (testing "looks at tags, ignores attributes"
    (is (true? (same-tag? "Root.0.Tag.0" "Root.0.Tag.0.language"))))

  (testing "doesn't care about indexes"
    (is (false? (same-tag? "Root.0.Tag.0" "Root.0.Tag.1")))))

(deftest shared-prefix-test
  (testing "finds the common set of tags, left to right"
    (let [seq1 (tags-with-indexes "Root.0.Tag.0.Divergent.1")
          seq2 (tags-with-indexes "Root.0.Tag.0.UniqueRabbit.2")]
      (is (= ["Root.0" "Tag.0"]
             (shared-prefix seq1 seq2))))))

(deftest to-close-and-to-open-test
  (testing "when paths change, tags must be closed"
    (is (= {:to-close '("Thingy" "Tag")
            :to-open '("Gat" "Whizbang")}
           (to-close-and-to-open
            "Root.0.Tag.0.Thingy.0"
            "Root.0.Gat.1.Whizbang.0"))))

  (testing "in a sequence of the same tag, we open and close the same type"
    (is (= {:to-close '("Thingy")
            :to-open '("Thingy")}
           (to-close-and-to-open
            "Root.0.Tag.0.Thingy.0"
            "Root.0.Tag.0.Thingy.1")))))

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
