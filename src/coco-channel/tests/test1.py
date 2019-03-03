from z3 import *

k_00 = Int('k00')
k_01 = Int('k01')
k_02 = Int('k02')

s = Solver()
s.add(k_01 > 0, k_00 > 0, k_02 > 0, k_00 < 5, k_01 < 5, k_02 < 5)
l = Int('l')
s.add(l < 3, l > 0)
c_0 = If(l-1 > -1, 1,0)
c_1 = If(l-2 > -1, 1,0)
c_2 = If(l-3 > -1, 1,0)
s.add(c_0*k_00*6 + c_1*k_01*6 + c_2*k_02*6 > 29)
s.add(c_0*k_00*6 + c_1*k_01*6 + c_2*k_02*6 < 36)

print(s.check())
print(s.model())
