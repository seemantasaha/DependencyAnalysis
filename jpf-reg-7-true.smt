(declare-fun v1() Int)
(assert (>= v1 0))
(assert (< v1 5))
(check-sat)