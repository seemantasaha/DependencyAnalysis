from z3 import *

if __name__ == "__main__":
	x = Int('x')
	y = Int('y')
	s = Solver()
	s.add(x + y > 5)
	print(s.check())
	print(s.model())