from z3 import *

x = Int('x')
y = Int('y')
s = Solver()
s.add(x > 2, y > 2, x*y < 10)
print(s.check())
print(s.model())