from expr import Expression

class node:
	def __init__(self, identity):
		self.identity = identity
		self.length = 1 #Assume a cost of 1 per basic block unless otherwise told 
		self.cost = Expression.IntConstant(1)
		self.variable = None
		self.costSet = False
		self.secret = False

	def __eq__(self, other):
		if isinstance(other, self.__class__):
			return self.identity == other.identity
   		return False	

   	def __str__(self):
   		return str(self.identity)

	def getId(self):
		return self.identity

	def setLength(self, length):
		self.length = length
		self.cost = Expression.IntConstant(length)

	def getLength(self):
		return self.length

	def setAsSecret(self):
		self.secret = True 

	def isSecret(self):
		return self.secret

	def setCost(self, cost):
		self.cost = cost
		self.costSet = True

	def getCost(self):
		return self.cost

	def isCostSet(self):
		return self.costSet

	def setVariable(self, var):
		self.variable = var

	def getVariable(self):
		return self.variable
