(ns request.middleware)

(defn wrap-auth-token
  "Middleware converting the :auth-token option into an Authorization header."
  [client & [token]]
  (fn [req]
    (if-let [auth-token (or (:auth-token req) token)]
      (client (-> req (dissoc :auth-token)
                  (assoc-in [:headers "authorization"]
                            (str "Token " auth-token))))
      (client req))))
