(ns cleebo.backend.handlers.settings
  (:require [re-frame.core :as re-frame]
            [cleebo.backend.middleware :refer [standard-middleware]]
            [cleebo.backend.db :refer [default-opts-map]]
            [ajax.core :refer [POST GET]]
            [taoensso.timbre :as timbre]))

(re-frame/register-handler              ;set global settings data to given path
 :set-settings
 standard-middleware
 (fn [db [_ path value]]
   (let [settings (get-in db [:settings])]
     (assoc-in db [:settings] (assoc-in settings path value)))))

(re-frame/register-handler
 :set-corpus
 standard-middleware
 (fn [db [_ corpus-name]]
   (re-frame/dispatch [:unset-query-results])
   (assoc-in db [:settings :query :corpus] corpus-name)))

(re-frame/register-handler              ;key is one of (:sort-opts, :filter-opts)
 :add-default-opts-map
 standard-middleware
 (fn [db [_ key & [default-opts]]]
   (update-in db [:settings :query key] conj (or default-opts (default-opts-map key)))))

(re-frame/register-handler              ;key is one of (:sort-opts, :filter-opts)
 :remove-opts-map
 standard-middleware
 (fn [db [_ key]] (update-in db [:settings :query key] pop)))

(re-frame/register-handler              ;set session data related to active project
 :set-project-settings
 standard-middleware
 (fn [db [_ path value]]
   (let [active-project (get-in db [:session :active-project])]
     ;; TODO: this should also send the updated settings to the db.
     ;; TODO: and also update session settings {:settings}
     (assoc-in db (into [:projects active-project :settings] path) value))))

(defn avatar-error-handler [& args]
  (re-frame/dispatch [:notify {:message "Couldn't update avatar"}]))

(re-frame/register-handler
 :regenerate-avatar
 (fn [db _]
   (POST "settings/new-avatar"
         {:params {}
          :handler #(re-frame/dispatch [:set-user [:avatar] %])
          :error-handler avatar-error-handler})
   db))

(GET "settings/user-settings"
     {:params {:a [1 2 3] :project "testProject"}
      :handler #(timbre/debug %)
      :error-handler #(timbre/debug "ERROR" %)})



