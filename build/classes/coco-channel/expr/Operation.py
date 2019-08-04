from Expression import *

class Operator:

	def __init__(self, rep, max_arity):
		self.rep = rep
		self.max_arity = max_arity

	def __str__(self):
		return self.rep

Operator.ADD = Operator("+", 2)
Operator.BOOLADD = Operator("/+/", 2)
Operator.SUB = Operator("-", 2)
Operator.TIMES = Operator("*", 2)
Operator.LOOPTIMES = Operator("/*/", 2)
Operator.EQ = Operator("==", 2)
Operator.NEQ = Operator("!=", 2)
Operator.LOOPEXPAND = Operator("#", 2)


class Operation(Expression):

	def __init__(self,  op, operands):
		self.operator = op
		self.operands = operands
		self.component = None
		self.offset = 0 
		self.variable = None

	def getOperator(self):
		return self.operator

	def getOperands(self):
		return self.operands

	def setComponent(self, comp):
		self.component = comp

	def getComponent(self):
		return self.component

	def increaseOffset(self, offset):
		self.offset += offset

	def getOffset(self):
		return self.offset

	def setVariable(self, var):
		self.variable = var

	def getVariable(self):
		return self.variable

	def __str__(self):
		ops = self.operands
		res = "(" + str(ops[0]) + " " + str(self.operator) + " "
		if len(ops) > 1:
			res+= str(ops[1])+ ")"
		'''res = "( " + self.operator.toString() + " "
		for op in self.operands:
			res += op.toString() + " "
		res += ")"'''
		return res

	def toZ3(self, index, extra=None):
		ops = self.operands
		if self.operator == Operator.LOOPEXPAND:
			return "LOOPEXPAND should not be used in z3 formula!"
		res = "(" + ops[0].toZ3(index, extra) + " " 
		if self.operator == Operator.BOOLADD:
			res += "+"
		elif self.operator == Operator.LOOPTIMES:
			res += "*"
		else:
			res += str(self.operator)
		res += " "
		if len(ops) > 1:
			res+= ops[1].toZ3(index,extra)+ ")"
		
		return res


	def toZ3Diff(self, index, extra=None):
		ops = self.operands
		if self.operator == Operator.LOOPEXPAND:
			return "LOOPEXPAND should not be used in z3 formula!"
		res = "(" + ops[0].toZ3Diff(index, extra) + " " 
		if self.operator == Operator.BOOLADD:
			res += "+"
		elif self.operator == Operator.LOOPTIMES:
			res += "*"
		else:
			res += str(self.operator)
		res += " "
		if len(ops) > 1:
			res+= ops[1].toZ3Diff(index,extra)+ ")"
		
		return res

	def toZ3min(self, index, maxVal = 10):
		ops = self.operands
		if self.operator == Operator.LOOPEXPAND:
			return "LOOPEXPAND should not be used in z3 formula!"
		res = "(" + ops[0].toZ3min(index) + " " 
		if self.operator == Operator.BOOLADD:
			res += "+"
		elif self.operator == Operator.LOOPTIMES:
			res += "*"
		else:
			res += str(self.operator)
		res += " "
		if len(ops) > 1:
			res+= ops[1].toZ3min(index)+ ")"
		
		return res


	def toZ3max(self, index):
		ops = self.operands
		if self.operator == Operator.LOOPEXPAND:
			return "LOOPEXPAND should not be used in z3 formula!"
		res = "(" + ops[0].toZ3max(index) + " " 
		if self.operator == Operator.BOOLADD:
			res += "+"
		elif self.operator == Operator.LOOPTIMES:
			res += "*"
		else:
			res += str(self.operator)
		res += " "
		if len(ops) > 1:
			res+= ops[1].toZ3max(index)+ ")"
		
		return res


	def toZ3NS(self,maxVal = 10):
		ops = self.operands
		if self.operator == Operator.LOOPEXPAND:
			return "LOOPEXPAND should not be used in z3 formula!"
		res = "(" + ops[0].toZ3NS() + " " 
		if self.operator == Operator.BOOLADD:
			res += "+"
		elif self.operator == Operator.LOOPTIMES:
			res += "*"
		else:
			res += str(self.operator)
		res += " "
		if len(ops) > 1:
			res+= ops[1].toZ3NS()+ ")"
		
		return res

	
