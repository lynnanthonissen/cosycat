(ns cleebo.backend.handlers.session
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [ajax.core :refer [GET]]
            [cleebo.backend.db :refer [default-history default-session]]
            [cleebo.backend.middleware :refer [standard-middleware]]
            [cleebo.backend.handlers.projects :refer [normalize-projects]]
            [cleebo.app-utils :refer [update-coll]]
            [cleebo.utils :refer [format]]
            [taoensso.timbre :as timbre]))

(re-frame/register-handler
 :update-filtered-users
 standard-middleware
 (fn [db [_ username flag]]
   (let [active-project (:active-project db)
         action (if flag conj disj)]
     (update-in db [:projects active-project :session :filtered-users] action username))))

(re-frame/register-handler              ;set session data to given path
 :set-session
 standard-middleware
 (fn [db [_ path value]]
   (let [session (:session db)]
     (assoc db :session (assoc-in session path value)))))

(re-frame/register-handler              ;set session data related to active project
 :set-project-session
 standard-middleware
 (fn [db [_ path value]]
   (let [active-project (get-in db [:session :active-project])]
     (assoc-in db (into [:projects active-project :session] path) value))))

(re-frame/register-handler
 :set-active-panel
 standard-middleware
 (fn [db [_ active-panel]]
   (assoc-in db [:session :active-panel] active-panel)))

(re-frame/register-handler
 :initialize-db
 standard-middleware
 (fn [_ [_ {:keys [me users corpora projects] :as payload}]]
   (-> payload
       (assoc :session (default-session :corpora corpora) :history default-history)
       (assoc-in [:session :init] true)
       (assoc :projects (normalize-projects projects me)))))

(re-frame/register-handler              ;load error
 :register-session-error
 standard-middleware
 (fn [db [_ {:keys [code message] :as args}]]
   (-> db
       (assoc-in [:session :active-panel] :error-panel)
       (assoc-in [:session :session-error] (select-keys args [:code :message])))))

(re-frame/register-handler              ;load error
 :drop-session-error
 standard-middleware
 (fn [db _] (-> db (assoc-in [:session :session-error] nil))))

(defn initialize-session-handler [payload]
  (re-frame/dispatch [:initialize-db payload]))

(defn initialize-session-error-handler [payload]
  (re-frame/dispatch
   [:register-session-error
    {:code "initialisation error"
     :message "Couldn't load user session :-S"}]))

(re-frame/register-handler
 :initialize-session
 (fn [db _]
   (GET "/session"
        {:handler initialize-session-handler
         :error-handler initialize-session-error-handler})
   {:session {:init false}}))
