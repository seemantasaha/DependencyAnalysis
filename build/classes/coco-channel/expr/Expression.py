from abc import ABCMeta, abstractmethod

class Expression:
	__metaclass__ = ABCMeta

	@abstractmethod
	def __str__(self):
		pass

	@abstractmethod
	def toZ3(self, index,extra=None):
		pass

	@abstractmethod
	def toZ3Diff(self, index,extra=None):
		pass

	@abstractmethod
	def toZ3min(self, index):
		pass

	@abstractmethod
	def toZ3max(self, index):
		pass

	@abstractmethod
	def toZ3NS(self):
		pass

class IntConstant(Expression):

	def __init__(self, cons):
		self.cons = cons

	def __str__(self):
		return str(self.cons)

	def getValue(self):
		return self.cons

	def toZ3(self, index, extra=None):
		return str(self.cons)


	def toZ3Diff(self, index, extra=None):
		return str(self.cons)

	def toZ3min(self, index):
		return str(self.cons)

	def toZ3max(self, index):
		return str(self.cons)

	def toZ3NS(self):
		return str(self.cons)

class LoopVariable(Expression):

	def __init__(self, var_name):
		self.var_name = var_name
		self.is_secret = False
		self.maxValue = None

	def setSecret(self):
		self.is_secret = True

	def isSecret(self):
		return self.is_secret

	def __str__(self):
		return self.var_name

	def setMaxValue(self, maxValue):
		self.maxValue = maxValue

	def getMaxValue(self):
		return self.maxValue

	def toZ3(self, index, extra = None):
		if extra:
			if self in extra.keys():
				return "self.loopVariablez3['" +str(self.var_name) + "_s1'] - self.loopVariablez3['" + str(self.var_name)+ "_s2']"
			else:
				return "self.loopVariablez3['" + str(self.var_name) + "_ex'] "
		if self.is_secret:
			return "self.loopVariablez3['" + str(self.var_name) + "_s" + str(index) +"']" 
		return "self.loopVariablez3['" + str(self.var_name) + "']"

	def toZ3Diff(self, index, extra = None):
		if extra:
			if self in extra.keys():
				return "self.res['" +str(self.var_name) + "_s1'] - self.res['" + str(self.var_name)+ "_s2']"
			else:
				return "self.res['" + str(self.var_name) + "_ex'] "
		if self.is_secret:
			return "self.res['" + str(self.var_name) + "_s" + str(index) +"']" 
		return "self.res['" + str(self.var_name) + "']"

	def toZ3min(self, index):
		return "min_res['" + str(self.var_name) + "']"

	def toZ3max(self, index):
		return "max_res['" + str(self.var_name) + "']"

	def toZ3NS(self):
		return "self.loopVariablez3['" + str(self.var_name) + "']"


class WithinLoopBranchVariable(Expression):

	def __init__(self, var_name):
		self.var_name = var_name
		self.is_secret = False
		self.maxValue = None

	def setSecret(self):
		self.is_secret = True

	def isSecret(self):
		return self.is_secret

	def __str__(self):
		return self.var_name

	def setBoolVersion(self, b):
		self.booleanVersion = b

	def getBoolVersion(self):
		return self.booleanVersion

	def setMaxValue(self, maxValue):
		self.maxValue = maxValue

	def getMaxValue(self):
		return self.maxValue

	def toZ3(self, index, extra=None):
		if extra:
			if self in extra.keys():
				print()
				return "self.loopBranchVariablez3['" +str(self.var_name) + "_s1 '] - self.loopBranchVariablez3['" + str(self.var_name)+ "_s2']"
			else:
				return "self.loopBranchVariablez3['" + str(self.var_name) + "_ex'] "
		if self.is_secret:
			return "self.loopBranchVariablez3['" + str(self.var_name) + "_s" + str(index) +"']" 
		return "self.loopBranchVariablez3['" + str(self.var_name) + "']"

	def toZ3Diff(self, index, extra=None):
		if extra:
			if self in extra.keys():
				print()
				return "self.res['" +str(self.var_name) + "_s1 '] - self.res['" + str(self.var_name)+ "_s2']"
			else:
				return "self.res['" + str(self.var_name) + "_ex'] "
		if self.is_secret:
			return "self.res['" + str(self.var_name) + "_s" + str(index) +"']" 
		return "self.res['" + str(self.var_name)+"']"


	def toZ3min(self, index):
		return "min_res['" + str(self.var_name) + "']"

	def toZ3max(self, index):
		return "max_res['" + str(self.var_name) + "']"

	def toZ3NS(self):
		return "self.loopBranchVariablez3['" + str(self.var_name) + "']"

class BooleanVariable(Expression):

	def __init__(self, var_name):
		self.var_name = var_name
		self.is_secret = False
		#self.node = node_

	def setSecret(self):
		self.is_secret= True

	def isSecret(self):
		return self.is_secret

	def __str__(self):
		return self.var_name

	#def toNode(self):
	#	return self.node

	def toZ3(self, index, extra=None):
		if extra:
			if self in extra.keys():
				return "self.BooleanVariablez3['" +str(self.var_name) + "_s1 '] - self.BooleanVariablez3['" + str(self.var_name)+ "_s2']"
			else:
				return "self.BooleanVariablez3['" + str(self.var_name) + "_ex'] "
		if self.is_secret:
			return "self.booleanVariablez3['" + str(self.var_name) + "_s" + str(index) +"']" 
		return "self.booleanVariablez3['" + str(self.var_name) + "']"


	def toZ3Diff(self, index, extra=None):
		if extra:
			if self in extra.keys():
				return "self.res['" +str(self.var_name) + "_s1 '] - self.res['" + str(self.var_name)+ "_s2']"
			else:
				return "self.res['" + str(self.var_name) + "_ex'] "
		if self.is_secret:
			return "self.res['" + str(self.var_name) + "_s" + str(index) +"']" 
		return "self.res['" + str(self.var_name) + "']"


	def toZ3min(self, index):
		return "min_res['" + str(self.var_name) + "']"

	def toZ3max(self, index):
		return "max_res['" + str(self.var_name) + "']"

	def toZ3NS(self):
		return "self.booleanVariablez3['" + str(self.var_name) + "']"