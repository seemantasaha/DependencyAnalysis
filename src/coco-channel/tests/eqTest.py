from z3 import *


if __name__ == "__main__":
	x_0 = Int('x_0')
	y_0 = Int('y_0')
	z_0 = Int('z_0')
	s = Solver()
	s.add(x_0 <= 1)
	s.add(x_0 > 0)
	s.add(y_0 > 0)
	s.add(z_0 > 0)
	s.add(x_0*2 + y_0*1 == z_0*6)
	print(s.check())
	print(s.model())
	
