(ns mpd.controllers.post-controls
  (:require [cljs.core.async :as async :refer [<! >! put!]])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(defmulti post-control-event!
  (fn [[event & args] com prev-state state] event))

(defmethod post-control-event! :default [[event & _] com _ _]
  ;; by default, send the command to the server
  ;; if the namespace of the event matches mpd
  (when (= "mpd" (namespace event))
    (let [socket (:socket com)]
      (go
        (>! socket [event])
        (<! socket)))))

(defmethod post-control-event! :mpd/playlistid [[event songid] com _ state]
  (let [socket (:socket com)]
    (go (>! socket [event songid])
        (.log js/console (pr-str "GET FOR " (<! socket))))))

(defmethod post-control-event! :status [[event data] com _ state]
  (let [event-bus (:event-bus com)
        songid nil]
    (when (and songid (not (get-in state [:cache :songid songid])))
      (put! event-bus [:mpd/playlistid songid]))))

(defmethod post-control-event! :mpd/status [[event & _] com _ _]
  (let [socket (:socket com)
        event-bus (:event-bus com)]
    (go
      (>! socket [event])
      (>! event-bus [:status (<! socket)]))))
