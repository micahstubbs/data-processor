(ns vip.data-processor.errors
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]))

(defrecord ValidationError [ctx severity scope
                            identifier error-type error-value])

(defn add-errors [{:keys [errors-chan] :as ctx}
                  severity scope identifier error-type
                  & error-data]
  (doseq [error-value error-data]
    (a/>!! errors-chan
           (->ValidationError ctx severity scope identifier error-type error-value)))

  ctx)

(defn close-errors-chan [{:keys [errors-chan] :as ctx}]
  (a/close! errors-chan)
  ctx)

(defn await-statistics
  "the process validations functions use bounded-batch-process from
  utility-works.async and puts the resulting core.async channel on the
  context at :processing-chan. Once the last validations have been
  saved, a thread will be created to calculate the feed's
  statistics. That thread's value will be a future that is placed on
  the core.async channel. Thus, here we wait on the core.async
  channel, and then wait on the future it gives."
  [ctx]
  (log/info "Awaiting statistics")
  (-> ctx
      :processing-chan
      a/<!!
      deref)
  (log/info "Statistics complete")
  ctx)
