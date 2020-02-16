(ns com.sixpages.lacinia-api.resolvers
  (:require [com.stuartsierra.component :as component]))


;;
;; Resolver Component protocol
;;   your resolver components must implement this

(defprotocol ResolverComponent
  "protocol for resolver components"
  (resolve-request [this context args value]))


;;
;; build helpers

(defn v-into-m
  [ks]
  (reduce
   #(assoc %1 %2 %2)
   {}
   ks))

(defn wrap
  [component]
  (fn [context args value]
    (resolve-request
     component
     context
     args
     value)))

(defn add-resolver-component
  [system]
  (fn [acc resolver-k component-k]
    (let [component (get system component-k)]
      (assoc
       acc
       resolver-k
       (wrap component)))))

(defn build
  [this
   resolver-deps-ks
   resolver-k-to-component-k]
  (if (empty? resolver-k-to-component-k)

    ;; no configuration supplied
    ;;  assume schema has :resolve with same name as resolver-deps
    ;;  components
    (->>  resolver-deps-ks
          v-into-m
          (reduce-kv
           (add-resolver-component this)
           {}))

    ;; with configuration (resolver-k-to-component-k)
    (reduce-kv
     (add-resolver-component this)
     {}
     resolver-k-to-component-k)))



;; Component

(defrecord ResolversComponent
    [resolver-deps-ks resolver-k-to-component-k]
  component/Lifecycle

  (start [this]
    (assoc
     this
     :m
     (build
      this
      resolver-deps-ks
      resolver-k-to-component-k)))

  (stop [this]
    (dissoc
     this
     :m)))


(defn new-component
  [config resolver-deps-ks]
  (map->ResolversComponent
   (-> (:schema config)
       (select-keys [:resolver-k-to-component-k])
       (assoc
        :resolver-deps-ks
        resolver-deps-ks))))
