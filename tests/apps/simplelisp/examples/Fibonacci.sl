;; Fibonacci
;; ========
;;
;; The first few numbers in the Fibonacci sequence are:
;; 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 
;; 987, 1597, 2584, 4181, 6765, 10946...
;;
;; As documented by Donald Knuth in The Art of Computer 
;; Programming, this sequence was first described by the 
;; Indian mathematicians Gopala and Hemachandra in 1150, 
;; who were investigating the possible ways of exactly bin 
;; packing items of length 1 and 2. In the West, it was first 
;; studied by Leonardo of Pisa, who was also known as 
;; Fibonacci (c. 1200), to describe the growth of an idealized 
;; rabbit population. The numbers describe the number of pairs
;;  in the rabbit population after n months if it is assumed that
;;
;;  * in the first month there is just one new-born pair,
;;  * new-born pairs become fertile from their second month on
;;  * each month every fertile pair begets a new pair, and
;;  * the rabbits never die
;;
;; Suppose that in month n we have a pairs of fertile and 
;; newly born rabbits and in month n + 1 we have b pairs. In 
;; month n + 2 we will necessarily have a + b pairs, because all 
;; a pairs of rabbits from month n will be fertile and produce a 
;; pairs of offspring, while the newly born rabbits in b will not be
;; fertile and will not produce offspring.
;;
;; -- Wikipedia

(defun fibonacci (x) 
  ;; compute fibonacci number x 
  (if (<= x 1) 1  
    (+ (fibonacci (- x 1)) (fibonacci (- x 2)))
    )
  )

(defun series (x) 
  (if (= x 0) (print  (fibonacci x))
    (progn
      (series (- x 1))
      (print " ")
      (print (fibonacci x))
      )
    )
)

(defun main () 
  (print "Enter length of series to generate: ")
  (series (- (parse-integer (read-line)) 1))
  (print "\n")
)

(main)