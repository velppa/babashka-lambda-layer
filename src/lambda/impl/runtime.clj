(ns lambda.impl.runtime
  (:require
    [cheshire.core :as json]
    [clj-http.lite.client :as http]))


(defn- get-lambda-invocation-request [runtime-api]
  (http/request
   {:method   :get
    :url      (str "http://" runtime-api "/2018-06-01/runtime/invocation/next")
    :timeout  900000}))


(defn- send-response [runtime-api request-id response-body]
  (http/request
   {:method  :post
    :url     (str "http://" runtime-api "/2018-06-01/runtime/invocation/" request-id "/response")
    :body    response-body
    :headers {"Content-Type" "application/json"}}))


(defn- send-error [runtime-api request-id error-body]
  (http/request
   {:method  :post
    :url     (str "http://" runtime-api "/2018-06-01/runtime/invocation/" request-id "/error")
    :body    {:errorType (str (or (:error error-body) "unknown"))
              :errorMessage (str (when (:error error-body)
                                   (dissoc error-body :error)
                                   error-body))}
    :headers {"Content-Type" "application/json"}}))


(defn- request->response [request-body handler-fn context]
  (let [decoded-request (json/decode request-body true)
        event (or (-> decoded-request :body (json/decode true)) decoded-request)]
    (handler-fn event (into {} context))))


(defn init [handler-fn context]
  (let [runtime-api (System/getenv "AWS_LAMBDA_RUNTIME_API")]
    (loop [req (get-lambda-invocation-request runtime-api)]
      (let [request-id (get (get req :headers) "lambda-runtime-aws-request-id")]
        (when-let [error (get req :error)]
          (send-error runtime-api request-id (str error))
          (throw (Exception. (str error))))
        (try
          (let [response (request->response (get req :body) handler-fn context)
                send-fn (if (:error response) send-error send-response)]
            (send-fn runtime-api request-id response))
          (catch Exception e
            (send-error runtime-api request-id (str e)))))
      (recur (get-lambda-invocation-request runtime-api)))))
