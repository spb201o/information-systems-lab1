(defproject information-systems-lab1 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [cheshire "5.4.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [postgresql "9.1-901.jdbc4"]
                 [com.cemerick/friend "0.2.1"]
                 [friend-oauth2 "0.1.3"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler information-systems-lab1.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
