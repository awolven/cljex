(ns cljex.core
  (:gen-class)
  (:require [clojure.string :as str])
  (:require [clojure.edn :as edn])
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as tc])
  (:require [clj-time.format :as tf])
  (:require [cheshire.core :refer :all :as json])
  (:require [cheshire.generate :as jg])
  (:require [clojure.java.io :as io])
  (:require [org.httpkit.server :as hk])
  (:require [compojure.route :only [files not-found]])
  (:require [compojure.handler :only [site]]) ; form, query params decode; cookie; session, etc
  (:require [compojure.core :only [defroutes GET POST DELETE ANY context]]))

(def database (atom '[]))

(defn string->datetime [dob-string]
  (let [date-list (str/split dob-string #"[-/]")]
    (if (= (count date-list) 3)
      (let [month (Integer/parseInt (nth date-list 0))
            day (Integer/parseInt (nth date-list 1))
            year (Integer/parseInt (nth date-list 2))]
        (t/date-time year month day))
      (throw (Exception. (str (list "Invalid input (date) " dob-string)))))))

(def custom-formatter (tf/formatter "M/d/yyyy"))

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
      (throw (Exception. (str (list "Invalid input (gender) " string)))))))

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
          (throw (Exception. (str (list "Invalid input (record) " line))))))
      (if (str/includes? line " ")
        (let [record-list (str/split line #"\s+")]
          (if (= (count record-list) 5)
            (record-list->hash-map record-list)
            (throw (Exception. (str (list "Invalid input (record) " line))))))
        (throw (Exception. (str (list "Invalid input (record) " line))))))))

(defn import-record [record]
  (swap! database #(cons record %)))

(defn get-gender [gender]
  (filter #(= gender (get % :gender)) @database))

;;(defn get-all-females-sorted []
;;  (hash-map :females (sort-by :last-name (get-gender :female))))

(defn get-all-females-sorted []
  (sort-by :last-name (get-gender :female)))

;;(defn get-all-males-sorted [] ;; names need to be ascending
;;  (hash-map :males (sort-by :last-name (get-gender :male))))

(defn get-all-males-sorted [] ;; names need to be ascending
  (sort-by :last-name (get-gender :male)))

(defn get-all-dob-sorted []
  (sort-by :date-of-birth t/before? @database))

(defn get-all-names-sorted-descending []
  (sort-by :last-name #(compare %2 %1) @database))

(defn get-all-names-sorted-ascending []
  (sort-by :last-name @database))

(defn write-plain [type-kwd]
  (doseq [record (cond (= type-kwd :name) (get-all-names-sorted-descending)
                       (= type-kwd :birthdate) (get-all-dob-sorted)
                       (= type-kwd :gender) (concat (get-all-females-sorted)
                                                    (get-all-males-sorted)))]
    (println (str (get record :last-name) ",")
             (str (get record :first-name) ",")
             (str (name (get record :gender)) ",")
             (str (get record :favorite-color) ",")
             (tf/unparse custom-formatter (get record :date-of-birth)))))

(defn write-gender-json [writer]
  (json/encode-stream (concat (get-all-females-sorted)
                              (get-all-males-sorted))
                      writer))

(defn write-birthdate-json [writer]
  (json/encode-stream (get-all-dob-sorted) writer))

(defn write-name-json [writer] ;; descending
  (json/encode-stream (get-all-names-sorted-descending) writer))

(defn import-test-data
  [filename]
  (with-open [in (io/reader filename)]
    (let []
      (println "loading" filename)
      (loop [count 0]
        (let [line (.readLine in)]
          (if (= line nil)
            count
            (let [record (parse-line line)]
              (import-record record)
              (recur (inc count)))))))))

(defn post-records-handler [ring-request]
  (hk/with-channel ring-request channel
    (try (import-record (parse-line (slurp (.bytes (:body ring-request)) :encoding "UTF-8")))
         (catch Exception e (hk/send! channel {:status 500 :body "invalid input"})))
    (hk/send! channel {:status 202})))

(defn get-gender-handler [ring-request]
  (hk/with-channel ring-request channel
    (hk/send! channel {:status 200
                       :headers {"Content-Type" "application/json; charset=utf-8"}
                       :body (let [writer (new java.io.StringWriter)]
                               (write-gender-json writer)
                               (.toString writer))})))

(defn get-birthdate-handler [ring-request]
  (hk/with-channel ring-request channel
    (hk/send! channel {:status 200
                       :headers {"Content-Type" "application/json; charset=utf-8"}
                       :body (let [writer (new java.io.StringWriter)]
                               (write-birthdate-json writer)
                               (.toString writer))})))

(defn get-name-handler [ring-request]
  (hk/with-channel ring-request channel
    (hk/send! channel {:status 200
                       :headers {"Content-Type" "application/json; charset=utf-8"}
                       :body (let [writer (new java.io.StringWriter)]
                               (write-name-json writer)
                               (.toString writer))})))

(compojure.core/defroutes all-routes
  (compojure.core/POST "/records" [] post-records-handler)
  (compojure.core/GET "/records/gender" [] get-gender-handler)
  (compojure.core/GET "/records/birthdate" [] get-birthdate-handler)
  (compojure.core/GET "/records/name" [] get-name-handler))

(defn show-help []
  (println "usage: java -jar <cljex-standalone-jar> [-h | --help] [-p | --port] [-i | --input] [-l | --list]")
  (println "Runs clojure example, optionally starting REST server, optionally loading data files.")
  (println)
  (println "Arguments:")
  (println "-h, --help                display this message")
  (println "-l, --list <TYPE>         list database sorted by <TYPE>, where")
  (println "                             <TYPE> is \"gender\", \"birthdate\" or \"name\"")
  (println "-p, --port <PORT>         start HTTP server at specified <PORT>")
  (println "-i, --input <PATH>        load file specified by <PATH> into database")
  (println "                             --input <PATH> can be used multiple times"))

(def cmd-line-args (atom (hash-map)))

(defn parse-arguments [args]
  (let [list (atom args)]
    (loop []
      (if (not (empty? @list))
        (let []
          (if (or (= (nth @list 0) "-p")
                  (= (nth @list 0) "--port"))
            (let []
              (reset! cmd-line-args (assoc @cmd-line-args :port (Integer/parseInt (nth @list 1))))
              (reset! list (rest (rest @list))))
            (if (or (= (nth @list 0) "-i")
                    (= (nth @list 0) "--input"))
              (let []
                (reset! cmd-line-args (assoc @cmd-line-args :inputs (cons (nth @list 1) (get @cmd-line-args :inputs))))
                (reset! list (rest (rest @list))))
              (if (or (= (nth @list 0) "-l")
                      (= (nth @list 0) "--list"))
                (let []
                  (reset! cmd-line-args (assoc @cmd-line-args :list (keyword (str/lower-case (nth @list 1)))))
                  (reset! list (rest (rest @list))))
                (if (or (= (nth @list 0) "-h")
                        (= (nth @list 0) "--help"))
                  (let []
                    (reset! cmd-line-args (assoc @cmd-line-args :help true))
                    (reset! list (rest @list)))))))
          (recur))))))

(defonce server (atom nil))                      

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (parse-arguments args)
  (if (get @cmd-line-args :help)
    (show-help)
    (let []
      (if (get @cmd-line-args :inputs)
        (let []
          (doseq [input (get @cmd-line-args :inputs)]
            (import-test-data input))))
      (if (get @cmd-line-args :list)
        (write-plain (get @cmd-line-args :list)))
      (if (get @cmd-line-args :port)
        (let []
          (println "starting server on port" (get @cmd-line-args :port))    
          (reset! server (hk/run-server (compojure.handler/site #'all-routes) {:port (get @cmd-line-args :port)})))))))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))
