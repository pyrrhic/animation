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
(defn create-animation-c [animation-textures should-loop]
  (assoc {}
         :animations animation-textures
         :current-frame -1 ;using negative 1 as a way to indicate that this animation has no started.
         :current-duration 0.0
         :loop should-loop
         :rotation -1)) ;negative 1 indicates that no rotation is applied. otherwise should be in degrees.

(defn create-renderable-c [texture x y]
  (assoc {}
         :texture texture
         :x x
         :y y))

;entities
(defn create-pistoleer [tex-cache x y]
  (assoc {}
         :renderable (create-renderable-c (first (:pistol-idle tex-cache)) x y)
         :animation (create-animation-c (:pistol-fire tex-cache) true)))

;systems
(defn render-s [{entities :entities batch :batch}]
  (.begin batch)
  (loop [ents entities]
    (if (empty? ents)
      nil
      (if-let [renderable (:renderable (first ents))]
        (.draw batch (:texture renderable) (float (:x renderable)) (float (:y renderable)) (float 64) (float 128))
        (recur (rest ents)))))
  (.end batch)
  game)  

;find texture for frame = frame number * 2 - 1
;find duration for frame = frame number * 2
;except for frame 0, since 0 * 2 is 0, which isn't right. 
(defn animate-s [{entities :entities}]
	(loop [ents (:entities game)
         updated-ents []]
		(if (empty? ents)
		  updated-ents
		  (let [e (first ents)]
		    (if-let [animation (:animation e)]
		      (if-let [renderable (:renderable e)]
		        (cond
		          (neg? (:current-frame animation))
		            (recur (rest ents)
		                   (conj updated-ents
		                         (assoc-in e [:animation :current-frame] 0)))
		          (<= 0 (:current-duration animation))
		            (recur (rest ents)
		                   (conj updated-ents
		                         (let [new-frame-num (mod (inc (:current-frame animation)) (count (:animations animation)))]
		                           (assoc-in (assoc-in e [:animation :current-frame] new-frame-num) 
		                                     [:animation :current-duration] 
		                                     (nth (:animations animation) (- (* new-frame-num 2) 1)))))))))))))

(defn init-tex-cache []
  (let [atlas (TextureAtlas. "s.pack")]
    (assoc {}
         :pistol-fire [(.findRegion atlas "fire pistol00") 0.1
                       (.findRegion atlas "fire pistol01") 0.1
                       (.findRegion atlas "fire pistol02") 0.1]
         :pistol-idle [(.findRegion atlas "pistol idle") 0.1]
         :wall [(.findRegion atlas "wall") 0.1]
         :floor [(.findRegion atlas "floor") 0.1]
         :tracer [(.findRegion atlas "tracer") 0.1])))

(defn init-game []
  (let [tex-cache (init-tex-cache)]
    (assoc {}
           :batch (SpriteBatch.)
           :tex-cache tex-cache
           :inputs {}
           :entities [(create-pistoleer tex-cache 50 50)])))

(defn game-loop [game delta]
  (clear-screen)
  (render-s game))

(defn screen []
  (reify Screen
    (show [this]
      (.setInputProcessor Gdx/input (input-processor))
      (def game (init-game)))
    
    (render [this delta]
      (if (empty? game) ;if this file is reloaded in the repl, setScreen does not get called and bad things happen. So this avoids doing anything.
        ""
        (alter-var-root (var game) #(game-loop % delta))))
    
    (dispose[this])
    (hide [this])
    (pause [this])
    (resize [this w h])
    (resume [this])))