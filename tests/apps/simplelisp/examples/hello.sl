(defun print-space (x) (if (= x 0) () (progn (print " ") (print-space (- x 1)))))

(defun forward (x) 
  (if (< x 9) 
      (progn 
	(print-space x) 
	(print "HELLO WORLD\n") 
	(sleep 1000)
	(forward (+ x 1))
	)
    )
  )

(defun backward (x) 
  (if (> x 0) 
      (progn 
	(print-space x) 
	(print "HELLO WORLD\n") 
	(sleep 1000)
	(backward (- x 1))
	)
    )
  )

(progn (forward 0) (backward 10))
