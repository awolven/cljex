(defproject cljex "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojure.java-time "0.3.2"]
                 [clj-time "0.14.4"]
                 [cheshire "5.8.0"]
                 [http-kit "2.2.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [compojure "1.6.1"]]
  :main cljex.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
