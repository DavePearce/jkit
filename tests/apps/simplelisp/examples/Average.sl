;; Average
;; =====
;;
;; This function simply computes the average
;; of a sequence of numbers.

(defun sum (x) 
  (if (equal x nil) 0
    (+ (car x) (sum (cdr x)))
    )
  )

(defun input-list () 
  (print "Enter number (or -1 for end of sequence): ")
  (let (
	(val (parse-integer (read-line)))
	)
    (if (>= val 0)
	(cons val (input-list))
      nil
      )
    )
)

(defun main ()
  (let (
	(input (input-list))
	)
    (print (/ (sum input) (length input)))
    )
  )

(main)
	
