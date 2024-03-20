(in-ns 'sudoku.core)

;; The grid is represented as an array of length 81.

;; 00 01 02 | 03 04 05 | 06 07 08
;; 09 10 11 | 12 13 14 | 15 16 17 
;; 18 19 20 | 21 22 23 | 24 25 26
;; ------------------------------
;; 27 28 29 | 30 31 32 | 33 34 35
;; 36 37 38 | 39 40 41 | 42 43 44
;; 45 46 47 | 48 49 50 | 51 52 53
;; ------------------------------
;; 54 55 56 | 57 58 59 | 60 61 62
;; 63 64 65 | 66 67 68 | 69 70 71
;; 72 73 74 | 75 76 77 | 78 79 80

;; Offsets into this grid, or the related markup structure (see below), are
;; termed squares. In a grid squares either have a fixed value (1 to 9) or
;; are empty (have value 0). In a markup, a square has one or more possible
;; values. Having more than one value indicates that the value of the square
;; has to be one of the listed values.

;; Offsets into the grid of each of the 9 rows, columns and boxes.

(def rows (vec (for [i (range 0 81 9)] (vec (range i (+ i 9))))))

(def cols (vec (for [i (range 9)] (vec (range i 81 9)))))

(def boxes
  (vec
    (for [i [0 3 6 27 30 33 54 57 60]]
      [
        i (+ i 1) (+ i 2)
        (+ i 9) (+ i 10) (+ i 11)
        (+ i 18) (+ i 19) (+ i 20)
      ])))

;; Provide a mapping from grid index to a vector of the row, column, and box
;; which contain the indexed square.

(def range81 (range 81))

(def units
  (vec
    (for [i range81]
      [
        (rows (quot i 9))
        (cols (mod i 9))
        (boxes (+ (* 3 (quot i 27)) (quot (mod i 9) 3)))
      ])))

;; Area of Effect. The neighborhood of a square at given grid index. This is
;; the set of all squares in the same row, column or box as the index into
;; aoe but doesn't include the square itself.

(def aoe
  (vec 
    (for [i range81]
      (vec (disj (set (flatten(units i))) i)))))

(defn digit-to-int
  "Convert a digit as a character to its integer value"
  [d]
  (- (int d) (int \0)))

;; An individual digit d is represented by (1 << (d - 1)) where << is the
;; bitshift left operator. Zero maps to itself.

(defn encode-digit
  "Return the markup bitmask value of a digit"
  [n]
  (if (zero? n)
    0
    (bit-shift-left 1 (dec n))))

;; The following constant is the maximum possible value that can occur as an
;; element in a markup vector. It represents a value which has all bits set
;; indicating that all digit values (1 through 9) are still possible.

(def unlimited (dec 512))

(defn enumerate [coll] (map-indexed vector coll))

;; Take a problem in the form of a string of 81 digits with '0' representing 
;; an empty square, and replace all empty squares with the possible values it
;; could have so as not to contradict existing squares with defined values.
;; The value returned is conceptually a vector of sets (represented by bitmasks)
;; of possible values for each square in the grid.

;; Note that the solve function and its helper functions deal in terms of this
;; 'markup' format.

(defn markup
  "Convert string representation of a grid into a markup vector of bitmaps"
  [problem]
  (let [
    grid
      (vec (map (comp encode-digit digit-to-int) problem))]

    (vec (map
      (fn [[i n]]
        (if (zero? n)
          (- unlimited (reduce bit-or (mapv grid (aoe i))))
          n))
      (enumerate grid)))))

;; This is essentially the inverse of markup. It takes a solution in markup
;; format, that is a grid sized vector where each element is a bitmask with
;; a single set bit, and returns a string comprising the ordered digits of
;; the solution.

(defn markdown
  "Take a solved markup vector and convert it to a string"
  [solution]
  (let [
    digits {1 \1, 2 \2, 4 \3, 8 \4, 16 \5, 32 \6, 64 \7, 128 \8, 256 \9}]
    
  (apply str (map digits solution))))

(defn remove-digit-from-markup [m i b] (update m i (fn [n] (- n b))))

;; Varous bit twiddling functions used to generate and transform a markup.

(defn least-significant-bit [n] (bit-and n (- n)))

(defn bits-as-seq
  "Return the bits of an integer as a sequence of bitmasks"
  [n]
  (loop [n n, result []]
    (if (zero? n)
      result
      (let [b (least-significant-bit n)]
        (recur (- n b) (conj result b))))))

(defn bit-count
  "Return the number of set bits in an integer"
  [n]
  (count (bits-as-seq n)))

(defn single-set-bit?
  "Determine if an integer has a single set bit; that it is a power of two."
  [n]
  (= n (least-significant-bit n)))

;; A vector of the number of set bits of all possible values that can occur in
;; a markup vector.

(def bitcounts (vec (map bit-count (range (inc unlimited)))))

(declare assign)
(declare remove-digit-from-aoe)
(declare constrain-markup)

;; This is the main part of the solver. It takes a markup vector, an index
;; into that vector, and a bitmask with a single bit set. This corresponds
;; to the digit that is to be removed from the markup value at that index.
;; It returns a new markup vector with that digit removed as a possibility
;; for the final value that the indexed element will take.

;; If the digit specified is not in fact in the set of possible values at
;; the offset then that is not a problem and is actually the ideal case
;; because nothing else needs doing. If the digit does exist in the set of
;; possible digits then it needs removing from the successor markup vector
;; and all other elements recursively updated to reflect this.

(defn eliminate
  "Generate a new markup reflecting the elimination of a digit"
  [m i b]

  ;; If the digit to eliminate is not present in the markup value at the
  ;; given offset then the markup vector can be returned unchanged.

  (if (zero? (bit-and b (m i))) m

  ;; If the digit being removed is the only possible value for the element
  ;; at that offset then this represents an impossible solution indicated
  ;; by returning nil.

  (if-not (single-set-bit? (m i))
    (let [
      w
        (- (m i) b)
      m
        (remove-digit-from-markup m i b)]

      (if (single-set-bit? w)
        (let [
          m
            (remove-digit-from-aoe m i w)]

          (if m
            (constrain-markup m i b)))

        (constrain-markup m i b))))))

;; Create a new markup by removing all other possible digits from an element
;; of the markup vector at a specified offset. This imposes new constraints
;; on the solution; none of the other possible values that the element could
;; have had can now be the value of that element. This in turn affects the
;; values that other elements sharing a row, column or box can take. The
;; assignment is only possible if these additional constraints are possible.

(defn assign
  [m i b]
  (loop [m m, n (m i)]
    (if (zero? n)
      m
      (let [
        w
          (least-significant-bit n)]

        (if (not= w b)
          (if-let [ next-m (eliminate m i w)]
            (recur next-m (- n w)))
          
          (recur m (- n w)))))))

;; Create a new markup vector by removing a specified digit from the aoe of
;; an index into the original. If the digit being removed is the only possible
;; value for another offset then nil is returned to indicate that the markup
;; vector does not represent a possible solution.

(defn remove-digit-from-aoe
  "Remove a digit as a possible value for all elements in a neighborhood"
  [m i b]
  (let [
    neighborhood (aoe i)
    limit (count neighborhood)]

    (loop [m m, j 0]
      (if (= j limit)
        m
        (do
          (assert (not (nil? m)) "remove-digit-from-aoe")
          (if-let [
             next-m
               (eliminate m (neighborhood j) b)]
            (recur next-m (inc j))))))))

(defn digit-locations
  [m b unit]
  (filter (fn [i] (pos? (bit-and b (m i)))) unit))

(defn constrain-markup2
  [m i b]
  (letfn [
    (make-reducer [b]
      (do (println "make-reducer" b)
      (fn [m unit]
        (do (println "reducer" (take 10 m) unit)
        (if m
          (let [
            locations
              (digit-locations m b unit)]
          
            (if (pos? (count locations))
              (if (= 1 (count locations))
                (assign m (first locations) b)
                m)
              (reduced nil)))
          (reduced nil))
        ))))]

    (assert m)
    (reduce (make-reducer b) m (units i))))

(defn constrain-markup
  [m i b]
  (loop [j 0, m m]
    (if (= j (count (units i)))
      m
      (let [
        unit 
          ((units i) j)
        places
          (filter (fn [k] (pos? (bit-and b (m k)))) unit)]

        (if (zero? (count places))
          nil
          (if (= 1 (count places))
            (let [
              m
                (assign m (first places) b)]

              (if (nil? m)
                nil
                (recur (inc j) m)))

            (recur (inc j) m)))))))

(defn solved?
  "Determine if a markup represents a puzzle solution"
  [m]
  (every? single-set-bit? m))

;; Find the offset of a markup vector element which has the minimum number of
;; possible digits without being locked down to a single digit. You could
;; ignore this step and simply choose any square which still has multiple
;; potential digits, but spending a little effort in choosing an appropriate
;; square will have the effect of decreasing the search space and improving
;; the overall performance of the algorithm. There will likely be many offsets
;; that could be chosen.

(defn choose-start
  [m]
  (loop [i 0, min_i 0, min_len 10]
    (if (= i (count m))
      min_i
      (let [
        nbits (bitcounts (m i))]

      (if (and (< 1 nbits) (< nbits min_len))
        (if (= nbits 2)
          i
          (recur (inc i) i nbits))
        (recur (inc i) min_i min_len))))))

;; The main solver takes as input a markup, that is a "marked up" problem which
;; is the output of passing a problem string to the markup function. It is
;; essentially a vector of bitmasks indicating what values are still possible
;; for a given element in the solution vector to take.

(defn solve
  [m]
  (if-not m nil

  (if (solved? m) m

  ;; Choose a starting element such that it has the minimum number of
  ;; possible digits that could fit there.

  (let [
    start-index
      (choose-start m)
    markup-value
      (m start-index)]

    (loop [b markup-value]
      (if (pos? b)
        (let [
          v
            (least-significant-bit b)
          m
            (solve (assign m start-index v))]

          (if m
            m
            (recur (- b v))))

        ;; No solution available with this markup vector so backtrack.

        nil))))))
