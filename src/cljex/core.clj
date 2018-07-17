(ns cljex.core
  (:gen-class)
  (:require [clojure.string :as str])
  (:require [clojure.edn :as edn])
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as tc])
  (:require [clj-time.format :as tf])
  (:require [cheshire.core :refer :all :as json])
  (:require [cheshire.generate :as jg]))
            

(def database (atom '[]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn string->datetime [dob-string]
  (let [date-list (str/split dob-string #"[-/]")]
    (if (= (count date-list) 3)
      (let [month (edn/read-string (nth date-list 0))
            day (edn/read-string (nth date-list 1))
            year (edn/read-string (nth date-list 2))]
        (t/date-time year month day))
      (throw (Exception. (str/join (list "Invalid input (date) " dob-string)))))))

(def custom-formatter (tf/formatter "MM/dd/yyyy"))

(jg/add-encoder org.joda.time.DateTime
                (fn [c jsonGenerator]
                  (.writeString jsonGenerator
                                (tf/unparse custom-formatter c))))

(defn includes-pipe? [string]
  (str/includes? string "|"))

(defn includes-comma? [string]
  (str/includes? string ","))

(defn canonicalize-gender [string]
  (if (or (= "female" (str/lower-case string))
          (= "f" (str/lower-case string)))
    :female
    (if (or (= "male" (str/lower-case string))
            (= "m" (str/lower-case string)))
      :male
      (throw (Exception. (str/join (list "Invalid input (gender) " string)))))))

(defn record-list->hash-map [record-list]
  (hash-map :last-name (nth record-list 0)
            :first-name (nth record-list 1)
            :gender (canonicalize-gender (nth record-list 2))
            :favorite-color (nth record-list 3)
            :date-of-birth (string->datetime (nth record-list 4))))

(defn parse-line [line]
  (let [includes-comma-or-pipe? (or (includes-pipe? line)
                                    (includes-comma? line))]

    (if includes-comma-or-pipe?
      (let [split-line (str/split line #"[|,]")
            record-list (map str/trim split-line)]
        (if (= (count record-list) 5)
          (record-list->hash-map record-list)
          (throw (Exception. (str/join (list "Invalid input (record) " line))))))
      (if (str/includes? line " ")
        (let [record-list (str/split line #"\s+")]
          (if (= (count record-list) 5)
            (record-list->hash-map record-list)
            (throw (Exception. (str/join (list "Invalid input (record) " line))))))
        (throw (Exception. (str/join (list "Invalid input (record) " line))))))))

(defn import-record [record]
  (swap! database #(cons record %)))

(defn get-gender [gender]
  (filter #(= gender (get % :gender)) @database))

(defn get-all-females-sorted []
  (hash-map :females (sort-by :last-name (get-gender :female))))

(defn get-all-males-sorted [] ;; names need to be ascending
  (hash-map :males (sort-by :last-name (get-gender :male))))

(defn get-all-dob-sorted []
  (sort-by :date-of-birth t/before? @database))

(defn get-all-names-sorted-descending []
  (sort-by :last-name #(compare %2 %1) @database))

(defn write-gender-json [writer]
  (json/encode-stream (list (get-all-females-sorted)
                            (get-all-males-sorted))
                      writer))

(defn write-birthdate-json [writer]
  (json/encode-stream (get-all-dob-sorted) writer))

(defn write-name-json [writer] ;; descending
  (json/encode-stream (get-all-names-sorted-descending) writer))

