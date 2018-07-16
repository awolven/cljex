(ns cljex.core
  (:gen-class)
  (:require [clojure.string :as str])
  (:require [clojure.edn :as edn])
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as tc])
  (:require [clj-time.format :as tf]))

(def database (atom '[]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn string->universal-date [dob-string]
  (let [date-list (str/split dob-string #"[-/]")]
    (if (= (count date-list) 3)
      (let [month (edn/read-string (nth date-list 0))
            day (edn/read-string (nth date-list 1))
            year (edn/read-string (nth date-list 2))]
        (tc/to-long (t/date-time year month day)))
      (throw (Exception. "Invalid input (date) " dob-string)))))

(def custom-formatter (tf/formatter "MM/dd/yyyy"))

(defn universal-date->string [long]
  (let [date (tc/from-long long)]
    (tf/unparse custom-formatter (tc/from-long date))))

(defn includes-pipe? [string]
  (str/includes? string "|"))

(defn includes-comma? [string]
  (str/includes? string ","))

(defn canonicalize-gender [string]
  (if (or (= "female" (.toLowerCase string))
          (= "f" (.toLowerCase string)))
    :female
    (if (or (= "male" (.toLowerCase string))
            (= "m" (.toLowerCase string)))
      :male
      (throw (Exception. "Invalid input (gender) " string)))))

(defn record-list->hash-map [record-list]
  (hash-map :last-name (nth record-list 0)
            :first-name (nth record-list 1)
            :gender (canonicalize-gender (nth record-list 2))
            :favorite-color (nth record-list 3)
            :date-of-birth (string->universal-date (nth record-list 4))))

(defn parse-line [line]
  (let [includes-comma-or-pipe? (or (includes-pipe? line)
                                    (includes-comma? line))]

    (if includes-comma-or-pipe?
      (let [split-line (str/split line #"[|,]")
            record-list (map str/trim split-line)]
        (if (= (count record-list) 5)
          (record-list->hash-map record-list)
          (throw (Exception. "Invalid input (record) " line))))
      (if (str/includes? line " ")
        (let [record-list (str/split line #"\s+")]
          (if (= (count record-list) 5)
            (record-list->hash-map record-list)
            (throw (Exception. "Invalid input (record)" line))))))))

(defn update-database [record]
  (swap! database #(cons record %)))

(defn database->json [key]
  (sort-by key @database))

(defn get-all-females-sorted []
  (sort-by :last-name (get-gender :female)))

(defn get-all-males-sorted []
  (sort-by :last-name (get-gender :male)))

(defn get-gender-sorted [gender]
  (filter #(= gender (get % :gender)) @database))
       


            
        
