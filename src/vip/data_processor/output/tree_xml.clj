(ns vip.data-processor.output.tree-xml
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [korma.core :as korma]
            [vip.data-processor.output.xml-helpers :refer [create-xml-file]]
            [vip.data-processor.db.postgres :as postgres]
            [vip.data-processor.db.util :as db.util])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [org.apache.commons.lang StringEscapeUtils]))

(def rows [{:path "VipObject.0.Candidate.0.id"
            :simple_path "VipObject.Candidate.id"
            :value "can001"}
           {:path "VipObject.0.Candidate.0.Name.0"
            :simple_path "VipObject.Candidate.Name"
            :value "Frank"}
           {:path "VipObject.0.Candidate.0.Party.1"
            :simple_path "VipObject.Candidate.Party"
            :value "Every day"}
           {:path "VipObject.0.Candidate.0.Title.2.Text.0.language"
            :simple_path "VipObject.Candidate.Title.Text.language"
            :value "en"}
           {:path "VipObject.0.Candidate.0.Title.2.Text.0"
            :simple_path "VipObject.Candidate.Title.Text"
            :value "President"}
           {:path "VipObject.0.Candidate.0.Title.2.Text.1.language"
            :simple_path "VipObject.Candidate.Title.Text.language"
            :value "es"}
           {:path "VipObject.0.Candidate.0.Title.2.Text.1"
            :simple_path "VipObject.Candidate.Title.Text"
            :value "\"El\" Presidente"}
           {:path "VipObject.0.Candidate.0.Nickname.3"
            :simple_path "VipObject.Candidate.Nickname"
            :value "> Ezra"}
           {:path "VipObject.0.Contest.1.id"
            :simple_path "VipObject.Contest.id"
            :value "con001"}])

(defn attr
  "Return the attribute of a path, if there is one."
  [path]
  (->> path
       (re-find #"(?:\.)(\D+)\z")
       second))

(defn opening-tag
  [tag]
  (str "<" tag ">"))

(defn closing-tag
  [tag]
  (str "</" tag ">"))

(defn tags-with-indexes
  "Returns a seq of the tags, with their indexes, of a path. Does not
  include attributes.

  (tags-with-indexes \"Root.0.Tag.10.id\") ;=> (\"Root.0\" \"Tag.10\")"
  [path]
  (when path
    (re-seq #"\w+\.\d+" path)))

(defn same-tag?
  "Are the two paths about the same tag. Thus:

  (same-tag? \"Root.0.Tag.10.id\" \"Root.0.Tag.10\") ;=> true"
  [prev-path path]
  (= (tags-with-indexes prev-path)
     (tags-with-indexes path)))

(defn shared-prefix
  "Returns a seq of the shared initial elements of two sequences."
  [a b]
  (loop [a a
         b b
         result []]
    (if (or (empty? a)
            (empty? b))
      result
      (let [[a1 & a-rest] a
            [b1 & b-rest] b]
        (if (= a1 b1)
          (recur a-rest
                 b-rest
                 (conj result a1))
          result)))))

(defn tag-without-index
  "Given a tag with an index, return the tag without the index."
  [tag-with-index]
  (re-find #"\A[^.]*" tag-with-index))

(defn to-close-and-to-open
  "Given a previous path and a current path, return the tags needed to
  be closed and the tags needed to be opened to transition from one to
  the other."
  [prev-path path]
  (let [prev-parts (tags-with-indexes prev-path)
        next-parts (tags-with-indexes path)
        shared-prefix (shared-prefix prev-parts next-parts)]
    {:to-close (->> prev-parts
                    (drop (count shared-prefix))
                    (map tag-without-index)
                    reverse)
     :to-open (->> next-parts
                   (drop (count shared-prefix))
                   (map tag-without-index))}))

;;; TODO: clean this up!
;;; TODO: tests!
;;; TODO: add autoincrement column to xml_tree_values
;;; DONE: pull the values from the db (lazily!)
;;; DONE: make a processing fn for this
;;; DONE: set schemaVersion from version of ctx
;;; DONE: create the file as a tempfile
;;; DONE: Add output file to ctx as :xml-output-file
(defn write-xml [file spec-version import-id]
  (with-open [f (io/writer (.toFile file))]
    (.write f (str "<?xml version=\"1.0\"?>\n<VipObject xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" schemaVersion=\"" spec-version "\" xsi:noNamespaceSchemaLocation=\"http://votinginfoproject.github.com/vip-specification/vip_spec.xsd\">\n"))
    (let [last-seen-path (atom "VipObject.0")
          inside-open-tag (atom false)]
      (doseq [{:keys [path value simple_path] :as row}
              (db.util/select-lazy 1000
                                   postgres/xml-tree-values
                                   (korma/where {:results_id import-id}))]
        (let [path (.getValue path)
              simple_path (.getValue simple_path)
              escaped-value (StringEscapeUtils/escapeXml value)]
          (if-let [attribute (attr path)]
            (do
              (if @inside-open-tag
                (if (same-tag? @last-seen-path path)
                  (.write f (str " " attribute "=\"" escaped-value "\""))
                  (let [{:keys [to-close
                                to-open]} (to-close-and-to-open @last-seen-path path)
                        last-tag (last to-open)
                        tags-to-open (butlast to-open)]
                    (.write f ">")
                    (.write f (apply str (map closing-tag to-close)))
                    (.write f (apply str (map opening-tag tags-to-open)))
                    (.write f (str "<" last-tag " " attribute "=\"" escaped-value "\""))))
                (let [{:keys [to-close
                              to-open]} (to-close-and-to-open @last-seen-path path)
                      last-tag (last to-open)
                      tags-to-open (butlast to-open)]
                  (.write f (apply str (map closing-tag to-close)))
                  (.write f (apply str (map opening-tag tags-to-open)))
                  (.write f (str "<" last-tag " " attribute "=\"" escaped-value "\""))))
              (reset! inside-open-tag true))
            (do
              (when @inside-open-tag
                (.write f ">"))
              (let [{:keys [to-close
                            to-open]} (to-close-and-to-open @last-seen-path path)]
                (.write f (apply str (map closing-tag to-close)))
                (.write f (apply str (map opening-tag to-open)))
                (.write f escaped-value))
              (reset! inside-open-tag false)))
          (reset! last-seen-path path)))
      (let [{:keys [to-close]} (to-close-and-to-open @last-seen-path "")]
        (when @inside-open-tag
          (.write f ">"))
        (.write f (apply str (map closing-tag to-close)))))))

(defn generate-xml-file [{:keys [spec-version import-id xml-output-file] :as ctx}]
  (write-xml xml-output-file spec-version import-id)
  ctx)

(def pipeline
  [create-xml-file
   generate-xml-file])
