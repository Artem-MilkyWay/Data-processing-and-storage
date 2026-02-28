(ns task-c5)

(defn make-forks
  [n]
  (vec (map (fn [i]
              (ref {:id i
                    :uses 0
                    :owner nil}))
            (range n))))

(def tx-count (atom 0))

(defn acquire-forks
  [id left right]
  (dosync
   (swap! tx-count inc)
   (let [l @left
         r @right]
     (when (and (nil? (:owner l))
                (nil? (:owner r)))
       (alter left  (fn [f]
                      (-> f
                          (update :uses (fnil inc 0))
                          (assoc :owner id))))
       (alter right (fn [f]
                      (-> f
                          (update :uses (fnil inc 0))
                          (assoc :owner id))))
       true))))

(defn release-forks
  [left right]
  (dosync
   (alter left assoc :owner nil)
   (alter right assoc :owner nil)))

(defn eat-once
  [id left right think-ms eat-ms]
  (Thread/sleep think-ms)
  (loop []
    (if (acquire-forks id left right)
      (do
        (Thread/sleep eat-ms)
        (release-forks left right))
      (do
        (Thread/sleep 5)
        (recur)))))

(defn philosopher
  [id left right think-ms eat-ms meals]
  (future
    (dotimes [_ meals]
      (eat-once id left right think-ms eat-ms))))

(defn run-simulation
  [philosophers think-ms eat-ms meals]
  (reset! tx-count 0)
  (let [forks  (make-forks philosophers)
        start  (System/nanoTime)
        workers (doall
                 (map (fn [id]
                        (let [left  (nth forks id)
                              right (nth forks (mod (inc id) philosophers))]
                          (philosopher id left right think-ms eat-ms meals)))
                      (range philosophers)))]
    (doseq [w workers] @w)
    (let [elapsed-sec (/ (- (System/nanoTime) start) 1e9)
          total-meals (* philosophers meals)
          attempts    @tx-count
          extra-attempts (max 0 (- attempts total-meals))]
      (println "Philosophers:" philosophers
               "meals per philosopher:" meals)
      (println "Think (ms):" think-ms
               "Eat (ms):" eat-ms)
      (println "Elapsed time:" elapsed-sec "sec")
      (println "Transaction bodies executed:" attempts)
      (println "Extra attempts (retries / failed acquires):" extra-attempts)
      (doseq [f forks]
        (println "Fork" (:id @f) "uses:" (:uses @f)))
      {:philosophers   philosophers
       :meals-per-phil meals
       :think-ms       think-ms
       :eat-ms         eat-ms
       :elapsed-sec    elapsed-sec
       :tx-attempts    attempts
       :extra-attempts extra-attempts
       :forks          (mapv deref forks)})))