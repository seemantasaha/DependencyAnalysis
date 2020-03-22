(declare-fun input () String)
(assert (in input /[A-Za-z0-9]{0,8}/))
(declare-fun pin () String)
(assert (in pin /[A-Za-z0-9]{0,8}/))

;(assert (not (= (charAt input 0) (charAt pin 0))))

(check-sat)