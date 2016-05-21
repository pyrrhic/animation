(ns animation.main-screen
  (:import [com.badlogic.gdx Screen Gdx InputProcessor Input Input$Keys Input$Buttons]
           [com.badlogic.gdx.graphics Color Texture GL20] 
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureAtlas TextureRegion]))

(def game {})

(defn update-game! [func]
  "Expects a function with 1 parameter which will be the game map. The function must return the updated game map."
  (alter-var-root (var game) #(func %)))

(defn clear-screen []
  (doto (Gdx/gl)
    (.glClearColor 1 1 1 1)
    (.glClear GL20/GL_COLOR_BUFFER_BIT)))

(defn draw [batch entities]
  (.begin batch)
  (.end batch))

(defn input-processor []
  (reify InputProcessor
    (touchDown [this x y pointer button] false)
    (keyDown [this keycode] 
      (alter-var-root (var game) #(assoc-in % [:inputs (keyword (Input$Keys/toString keycode))] true))
      true)
    (keyUp [this keycode] 
      (alter-var-root (var game) #(assoc-in % [:inputs (keyword (Input$Keys/toString keycode))] false))
      true)
    (keyTyped [this character] false)
    (touchUp [this x y pointer button] 
      (alter-var-root (var game) #(assoc-in % [:inputs :mouse-x] x))
      (alter-var-root (var game) #(assoc-in % [:inputs :mouse-y] (- 600 y)))
      false)
    (touchDragged [this x y pointer] false)
    (mouseMoved [this x y] false)
    (scrolled [this amount] false)))

;components
(defn transform-component [x y rot]
  (assoc {}
         :x x
         :y y
         :rotation rot))

(defn animation-component [animations]
  "anims example - {:walk [tex1 frame-duration1 tex2 frame-duration2]
                    :idle [tex1 frame-duration1]}"
  (assoc {}
         :tex-and-durs animations
         :is-playing true
         :current-frame 0
         :current-duration (second (:pistol-fire animations))))

;entities
(defn pistoleer-entity [{tex-cache :tex-cache}]
  (assoc {}
         :transform (transform-component 50 50 0)
         :animations (animation-component {:pistol-idle (:pistol-idle tex-cache)
                                            :pistol-fire (:pistol-fire tex-cache)})
         :renderable (first (:pistol-fire tex-cache))
         :state :firing))

;systems
(defn update-animation-and-render-comps [ent animation-state]
  (let [inc-frame (inc (:current-frame (:animations ent)))
        next-frame (if (>= inc-frame (/ (count (animation-state (:tex-and-durs (:animations ent)))) 2))
                     0
                     inc-frame)
        next-texture-idx (if (zero? next-frame) 0 (* 2 next-frame))
        next-texture (nth (animation-state (:tex-and-durs (:animations ent))) next-texture-idx)
        next-duration-idx (if (zero? next-frame) 1 (- (* 2 next-frame) 1))
        next-duration (nth (animation-state (:tex-and-durs (:animations ent))) next-duration-idx)]
    (-> ent
   (assoc-in [:animations :current-frame] next-frame)
   (assoc-in [:animations :current-duration] next-duration)
   (assoc :renderable next-texture))))

(defn update-anim-ents [ents]
  (mapv (fn [ent]
			    (if (:is-playing (:animations ent))
			      (let [animation-state (case (:state ent)
			                            :idle :pistol-idle
			                            :firing :pistol-fire)
			            animations (animation-state (get-in ent [:animations :tex-and-durs]))]
			        (if (neg? (:current-duration (:animations ent)))
			          (update-animation-and-render-comps ent animation-state)
			          (assoc-in ent [:animations :current-duration] (- (get-in ent [:animations :current-duration]) (:delta game)))))
			      ent))
			  ents))

(defn animate-pistoleer-system [{entities :entities :as game}]
  (let [ents (filterv (fn [ent] (and (:renderable ent) (:animations ent))) entities)
        other-ents (filterv (fn [ent] (not (and (:renderable ent) (:animations ent)))) entities)]
    (assoc game :entities (vec (concat other-ents (update-anim-ents ents))))))   

(defn render-system [{entities :entities batch :batch :as game}]
	(.begin batch)
	(loop [ents entities]
	  (if (empty? ents)
	    nil
	    (if-let [renderable (:renderable (first ents))]
	     (let [transform (:transform (first ents))]
	       (.draw batch renderable (float (:x transform)) (float (:y transform)) (float 64) (float 128))
	       (recur (rest ents)))
	     (recur (rest ents)))))
	(.end batch)
  game)    

(defn init-tex-cache []
  (let [atlas (TextureAtlas. "s.pack")]
    (assoc {}
         :pistol-fire [(.findRegion atlas "fire pistol00") 0.05
                       (.findRegion atlas "fire pistol01") 0.1
                       (.findRegion atlas "pistol idle") 0.1]
         :pistol-idle [(.findRegion atlas "pistol idle") 0.1]
         :wall [(.findRegion atlas "wall") 0.1]
         :floor [(.findRegion atlas "floor") 0.1]
         :tracer [(.findRegion atlas "tracer") 0.1])))

(defn init-game []
  (let [tex-cache (init-tex-cache)
        game-1 (assoc {}
                      :batch (SpriteBatch.)
                      :tex-cache tex-cache
                      :inputs {})
        game-2 (assoc game-1
                      :entities [(pistoleer-entity game-1)])] 
    game-2))

(defn game-loop [game]
  (clear-screen)
  (-> game
    (animate-pistoleer-system)
    (render-system)))

(defn screen []
  (reify Screen
    (show [this]
      (.setInputProcessor Gdx/input (input-processor))
      (def game (init-game)))
    
    (render [this delta]
      (if (empty? game) ;if this file is reloaded in the repl, setScreen does not get called and bad things happen. So this avoids doing anything.
        ""
	      (do 
         (update-game! #(assoc % :delta delta))
         (update-game! #(game-loop %)))))
    
    (dispose[this])
    (hide [this])
    (pause [this])
    (resize [this w h])
    (resume [this])))