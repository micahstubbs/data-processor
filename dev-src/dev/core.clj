(ns dev.core
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [vip.data-processor.pipeline :as pipeline]
            [vip.data-processor.output.xml :as xml-output]
            [vip.data-processor.validation.data-spec :as data-spec]
            [vip.data-processor.validation.db :as db]
            [vip.data-processor.validation.transforms :as t]
            [vip.data-processor.validation.zip :as zip]
            [vip.data-processor.db.postgres :as psql]
            [vip.data-processor.s3 :refer [xml-filename]]
            [democracyworks.squishy.data-readers]))

(def pipeline
  (concat [zip/assoc-file
           zip/extracted-contents
           t/attach-sqlite-db
           (data-spec/add-data-specs data-spec/data-specs)
           t/xml-csv-branch]
          [psql/start-run]
          db/validations
          xml-output/pipeline
          [psql/insert-validations
           psql/import-from-sqlite]))

(defn -main [filename]
  (psql/initialize)
  (let [file (if (zip/xml-file? filename)
               (java.nio.file.Paths/get filename (into-array [""]))
               (java.io.File. filename))
        result (pipeline/process pipeline file)]
    (when-let [xml-output-file (:xml-output-file result)]
      (println "XML:" (.toString xml-output-file))
      (let [filename (xml-filename result)
            results (assoc result :generated-xml-filename filename)]
        (psql/complete-run results)))))
