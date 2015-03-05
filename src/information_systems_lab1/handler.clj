(ns information-systems-lab1.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [korma.core :refer :all]
            [korma.db :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as res]
            [ring.util.codec :as codec]
            [clj-http.lite.client :as http]
            [information-systems-lab1.facebook :refer :all]))


(defdb test-db
  (postgres {:db "my_blog"}))

(defentity tokens)
(defentity posts)

(defn logger [app]
  (fn [req]
    (println (clojure.string/join " " [(:request-method req)
                                       (:uri req)
                                       (:query-string req)]))
    (app req)))

(defn auth [app]
  (fn [req]
    (let [user (select tokens (where {:token (:token (:params req))}))]
      (if (= (count user) 1) (app (assoc req :user (first user)))
                             (app req)))))

(defn get-index []
  "Hello world")

(defn get-all-documents []
  (json/generate-string
    (select posts)))

(defn create-new-document []
  "Create new document")

(defn get-document [id]
  (str id))

(defn update-document [id params]
  (str id " " params))

(defn delete-document [id]
  (str id))

(defn print-params [params]
  (str params))

(defn parse-params [string]
  (clojure.walk/keywordize-keys
    (codec/form-decode string)))

(defn get-token [code]
  (parse-params (:body (http/get 
    (str "https://graph.facebook.com/oauth/access_token?client_id=" client-id
         "&redirect_uri=" redirect-uri
         "&client_secret=" client-secret
         "&code=" code)))))

(defn get-app-token []
  (parse-params (:body (http/get
    (str "https://graph.facebook.com/oauth/access_token?client_id=" client-id
         "&client_secret=" client-secret
         "&grant_type=client_credentials")))))

(defn inspect-token [token]
  (json/parse-string (:body (http/get
    (str "https://graph.facebook.com/debug_token?input_token=" token
         "&access_token=" app-token)))))

(defn get-fb-info [token]
  (json/parse-string (:body (http/get
    (str "https://graph.facebook.com/me?access_token=" token)))))

(defn save-token [token]
  (let [token-data ((inspect-token token) "data")]
    (insert tokens (values {:token token
                            :expires_at (token-data "expires_at")
                            :user_id (token-data "user_id")}))))
  

(defn current-timestamp []
  (quot (System/currentTimeMillis) 1000))

(defroutes auth-routes
  (GET "/" []
    (res/redirect (str "https://www.facebook.com/dialog/oauth?client_id=" client-id "&response_type=code&redirect_uri=" redirect-uri)))
  (GET "/success" {params :params}
    (save-token (:access_token (get-token (:code params)))))
  (GET "/inspect" {params :params}
    (inspect-token (:token params)))
  (GET "/me" {params :params}
    (get-fb-info (:token params)))
  (GET "/about" {user :user} (str user)))


(defroutes app-routes
  (GET "/" {params :params} (print-params params))
  (context "/api" []
    (GET  "/" [] (get-all-documents))
    (POST "/" {params :params} (create-new-document params))
    (context "/:id" [id]
      (GET    "/" [] (get-document id))
      (PUT    "/" {params :params} (update-document id params))
      (DELETE "/" [] (delete-document id))))
  (context "/auth" []
    auth-routes)
  (route/not-found "Not Found"))

(def middlewares
  (-> app-routes
      logger
      auth))

(def app
  (wrap-defaults middlewares site-defaults))
