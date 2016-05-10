(ns animation.MyGame
  (:import (com.badlogic.gdx Game Screen Gdx))
  (:require [animation.main-screen :as main-screen]))

(gen-class
 :name animation.MyGame
 :extends com.badlogic.gdx.Game)
 
(defn -create [^Game this]
  (.setScreen this (main-screen/screen)))