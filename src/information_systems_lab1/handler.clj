(ns information-systems-lab1.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [clojure.java.jdbc :as db]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn logger [app]
  (fn [req]
    (println 
      (apply str (interpose " "
       [(:request-method req)
        (:uri req)])))
    (app req)))

(defn get-index []
  "Hello world")

(defn get-all-documents []
  (json/generate-string
    (db/query
      "postgresql://localhost:5432/my_blog"
      ["select * from posts"])))

(defn create-new-document []
  "Create new document")

(defn get-document [id]
  (str id))

(defn update-document [id body]
  (str id " " body))

(defn delete-document [id]
  (str id))

(defroutes app-routes
  (GET "/" [] (get-index))
  (context "/api" []
    (GET  "/" [] (get-all-documents))
    (POST "/" {body :body} (create-new-document body))
    (context "/:id" [id]
      (GET    "/" [] (get-document id))
      (PUT    "/" {body :body} (update-document id body))
      (DELETE "/" [] (delete-document id))))
  (route/not-found "Not Found"))

(def middlewares
  (-> app-routes
      logger))

(def app
  (wrap-defaults middlewares site-defaults))
