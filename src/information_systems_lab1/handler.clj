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
(defentity songs)

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

(defn get-all-songs [params]
  (json/generate-string
    (select songs (limit (:limit params)) (offset (:offset params)))))

(defn create-new-song [params]
  (insert songs (values {:title  (:title  params)
                         :artist (:artist params)
                         :url    (:url    params)}))
  (json/generate-string {:message "Song uploaded"}))

(defn get-song [id]
  (select songs (where {:id id})))

(defn update-song [id params]
  (update songs (set-fields (merge
                              (if-let [title  (:title  params)] {:title  title})
                              (if-let [artist (:artist params)] {:artist artist})
                              (if-let [url    (:url    params)] {:url    url})))
    (where {:id id}))
  (select songs (where {:id id})))

(defn delete-song [id]
  (delete songs (where {:id id}))
  (json/generate-string {:message "Song deleted"}))

(defroutes api-routes
  (context "/songs" []
    (GET  "/" {params :params} (get-all-songs params))
    (POST "/" {params :params} (create-new-song params))
    (context "/:id" [id]
      (GET    "/" [] (get-song (read-string id)))
      (PUT    "/" {params :params} (update-song (read-string id) params))
      (DELETE "/" [] (delete-song (read-string id))))))

(defn print-params [params]
  (str params))

(defn parse-params [string]
  (clojure.walk/keywordize-keys
    (codec/form-decode string)))

(defn fb-auth-redirect []
  (res/redirect
    (str "https://www.facebook.com/dialog/oauth?client_id=" client-id
         "&response_type=" "code"
         "&redirect_uri=" redirect-uri)))

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
         "&grant_type=" "client_credentials")))))

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
  (GET "/fb" [] fb-auth-redirect)
  (GET "/success" {params :params}
    (save-token (:access_token (get-token (:code params)))))
  (GET "/inspect" {params :params}
    (inspect-token (:token params)))
  (GET "/me" {params :params}
    (get-fb-info (:token params)))
  (GET "/about" {user :user} (str user)))

(defroutes app-routes
  (GET "/" {params :params} (print-params params))
  (context "/api" [] api-routes)
  (context "/auth" [] auth-routes)
  (route/not-found "Not Found"))


(def app
  (wrap-defaults
    (-> app-routes
      logger
      auth)
    (-> site-defaults
        (assoc-in [:security :anti-forgery] false))))