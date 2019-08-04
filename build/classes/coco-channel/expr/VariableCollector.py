import Expression 

class VariableCollector:

	def __init__(self):
		self.variables = {}

	def visit(self, expr):
		if isinstance(expr, Expression.LoopVariable):
			var = expr.toString()
			if var not in self.variables.keys():
				self.variables[var] = 0
			return  
		if isinstance(expr, Expression.BooleanVariable):
			var = expr.toString();
			if var not in self.variables.keys():
				self.variables[var] = 1
			return  
		if isinstance(expr, Expression.IntConstant):
			return 
		oprs = expr.getOperands()
		for op in oprs:
			self.visit(op)

	def getVariables(self):
		return self.variables
