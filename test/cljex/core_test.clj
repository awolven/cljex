(ns cljex.core-test
  (:require [clojure.test :refer :all]
            [cljex.core :refer :all])
  (:require [clojure.string :as str])
  (:require [clojure.edn :as edn])
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as tc])
  (:require [clj-time.format :as tf])
  (:require [cheshire.core :refer :all :as json])
  (:require [cheshire.generate :as jg])
  (:require [clojure.java.io :as io])
  (:require [org.httpkit.server :as hk])
  (:require [org.httpkit.client :as hkc])
  (:require [compojure.route :only [files not-found]])
  (:require [compojure.handler :only [site]]) ; form, query params decode; cookie; session, etc
  (:require [compojure.core :only [defroutes GET POST DELETE ANY context]]))

(deftest datetime-1
  (is (t/equal? (t/date-time 2000 1 1) (string->datetime "1/1/2000"))))

(deftest pipe-1
  (is (includes-pipe? "abc|123")))

(deftest comma-1
  (is (includes-comma? "abc,123")))

(deftest gender-1
  (is (= :female (canonicalize-gender "f"))))

(deftest gender-2
  (is (= :male (canonicalize-gender "MALE"))))

(deftest gender-3
  (is (thrown? Exception (canonicalize-gender "gmail"))))

(deftest record-list-1
  (is (let [m1 (hash-map :last-name "Smith", :first-name "Bob", :gender :male, :favorite-color "blue", :date-of-birth (t/date-time 1970 1 1))
            m2 (record-list->hash-map (list "Smith" "Bob" "m" "blue" "1/1/1970"))]
        (and (= (get m1 :last-name) (get m2 :last-name))
             (= (get m1 :first-name) (get m2 :first-name))
             (= (get m1 :gender) (get m2 :gender))
             (= (get m1 :favorite-color) (get m2 :favorite-color))
             (t/equal? (get m1 :date-of-birth) (get m2 :date-of-birth))))))

(deftest parse-line-1
  (is (let [m1 (hash-map :last-name "Smith", :first-name "Bob", :gender :male, :favorite-color "blue", :date-of-birth (t/date-time 1970 1 1))
            m2 (parse-line "Smith Bob Male blue 1/1/1970")]
        (and (= (get m1 :last-name) (get m2 :last-name))
             (= (get m1 :first-name) (get m2 :first-name))
             (= (get m1 :gender) (get m2 :gender))
             (= (get m1 :favorite-color) (get m2 :favorite-color))
             (t/equal? (get m1 :date-of-birth) (get m2 :date-of-birth))))))

(deftest parse-line-2
  (is (let [m1 (hash-map :last-name "Smith", :first-name "Bob", :gender :male, :favorite-color "blue", :date-of-birth (t/date-time 1970 1 1))
            m2 (parse-line "Smith, Bob, Male, blue, 1/1/1970")]
        (and (= (get m1 :last-name) (get m2 :last-name))
             (= (get m1 :first-name) (get m2 :first-name))
             (= (get m1 :gender) (get m2 :gender))
             (= (get m1 :favorite-color) (get m2 :favorite-color))
             (t/equal? (get m1 :date-of-birth) (get m2 :date-of-birth))))))

(deftest parse-line-3
  (is (let [m1 (hash-map :last-name "Smith", :first-name "Bob", :gender :male, :favorite-color "blue", :date-of-birth (t/date-time 1970 1 1))
            m2 (parse-line "Smith | Bob | Male | blue | 1/1/1970")]
        (and (= (get m1 :last-name) (get m2 :last-name))
             (= (get m1 :first-name) (get m2 :first-name))
             (= (get m1 :gender) (get m2 :gender))
             (= (get m1 :favorite-color) (get m2 :favorite-color))
             (t/equal? (get m1 :date-of-birth) (get m2 :date-of-birth))))))

(deftest import-record-and-parse-line-1
  (is (let [m1 (hash-map :last-name "Smith", :first-name "Bob", :gender :male, :favorite-color "blue", :date-of-birth (t/date-time 1970 1 1))]
        (reset! database nil)
        (import-record (parse-line "Smith | Bob | Male | blue | 1/1/1970"))
        (let [m2 (first @database)]
          (and (= (get m1 :last-name) (get m2 :last-name))
               (= (get m1 :first-name) (get m2 :first-name))
               (= (get m1 :gender) (get m2 :gender))
               (= (get m1 :favorite-color) (get m2 :favorite-color))
               (t/equal? (get m1 :date-of-birth) (get m2 :date-of-birth)))))))

(deftest get-gender-1
  (is (let [r1 (parse-line "Haynes Jane f green 3/4/1960")
            r2 (parse-line "Smith Bob Male blue 1/1/1970")
            r3 (parse-line "Davis Mary female red 5/6/1965")]
        (reset! database nil)
        (import-record r1)
        (import-record r2)
        (import-record r3)
        (let [females (get-all-females-sorted)]
          (and (= (get (first females) :last-name) "Davis")
               (= (get (second females) :last-name) "Haynes")
               (empty? (rest (rest females))))))))

(deftest get-gender-2
  (is (let [r1 (parse-line "Haynes Jane f green 3/4/1960")
            r2 (parse-line "Smith Bob Male blue 1/1/1970")
            r3 (parse-line "Davis Mary female red 5/6/1965")]
        (reset! database nil)
        (import-record r1)
        (import-record r2)
        (import-record r3)
        (let [males (get-all-males-sorted)]
          (and (= (get (first males) :last-name) "Smith")
               (empty? (rest males)))))))

(deftest get-dob-sorted-1
    (is (let [r1 (parse-line "Smith Bob Male blue 1/1/1970")
              r2 (parse-line "Haynes Jane f green 3/4/1960")
              r3 (parse-line "Davis Mary female red 5/6/1965")]
          (reset! database nil)
          (import-record r1)
          (import-record r2)
          (import-record r3)
          (let [records (get-all-dob-sorted)]
            (and (= (get (nth records 0) :last-name) "Haynes")
                 (= (get (nth records 1) :last-name) "Davis")
                 (= (get (nth records 2) :last-name) "Smith"))))))

(deftest get-names-sorted-1
  (is (let [r1 (parse-line "Haynes Jane f green 3/4/1960")
            r2 (parse-line "Smith Bob Male blue 1/1/1970")
            r3 (parse-line "Davis Mary female red 5/6/1965")]
        (reset! database nil)
        (import-record r1)
        (import-record r2)
        (import-record r3)
        (let [records (get-all-names-sorted-descending)]
          (and (= (get (nth records 0) :last-name) "Smith")
               (= (get (nth records 1) :last-name) "Haynes")
               (= (get (nth records 2) :last-name) "Davis"))))))

(deftest http-get-records-name-test-1
  (is (let []
        (-main "--port" "10999")
        (reset! database nil)
        @(hkc/post "http://localhost:10999/records"
                   {:body "Davis Mary female red 5/6/1965"})
        @(hkc/post "http://localhost:10999/records"
                  {:body "Smith Bob Male blue 1/1/1970"})
        @(hkc/post "http://localhost:10999/records"
                  {:body "Haynes Jane f green 3/4/1960"})
        (let [{:keys [status headers body error opts] :as resp}
              @(org.httpkit.client/get "http://localhost:10999/records/name")]
          (let [body (get resp :body)]
            (stop-server)
            (let [records (json/parse-string body true)]
              (and (= (count records) 3)
                   (= "Smith" (get (first records) :last-name)))))))))

(deftest http-get-records-gender-test-1
  (is (let []
        (-main "--port" "10999")
        (reset! database nil)
        @(hkc/post "http://localhost:10999/records"
                   {:body "Davis Mary female red 5/6/1965"})
        @(hkc/post "http://localhost:10999/records"
                  {:body "Smith Bob Male blue 1/1/1970"})
        @(hkc/post "http://localhost:10999/records"
                  {:body "Haynes Jane f green 3/4/1960"})
        (let [{:keys [status headers body error opts] :as resp}
              @(org.httpkit.client/get "http://localhost:10999/records/gender")]
          (let [body (get resp :body)]
            (stop-server)
            (let [records (json/parse-string body true)]
              (and (= (count records) 3)
                   (= "Davis" (get (first records) :last-name)))))))))

(deftest http-get-records-birthdate-test-1
  (is (let []
        (-main "--port" "10999")
        (reset! database nil)
        @(hkc/post "http://localhost:10999/records"
                   {:body "Davis Mary female red 5/6/1965"})
        @(hkc/post "http://localhost:10999/records"
                  {:body "Smith Bob Male blue 1/1/1970"})
        @(hkc/post "http://localhost:10999/records"
                  {:body "Haynes Jane f green 3/4/1960"})
        (let [{:keys [status headers body error opts] :as resp}
              @(org.httpkit.client/get "http://localhost:10999/records/birthdate")]
          (let [body (get resp :body)]
            (stop-server)
            (let [records (json/parse-string body true)]
              (and (= (count records) 3)
                   (= "Haynes" (get (first records) :last-name)))))))))
