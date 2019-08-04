from expr import Expression, Operation 
from z3 import *
from graph import component


class minMax:
	''' Just finds the maximum that you can take along a path.
	Does not consider anything as secret'''

	def __init__(self, component):
		self.variables = set()
		self.booleanVariablez3 = {}
		self.allVariablesz3 = {}
		self.loopVariablez3 = {}
		self.loopBranchVariablez3 = {}
		self.minimizer = Optimize()
		self.maximizer = Optimize()
		self.LOOPMAXVALUE = 10
		self.maxValueDictionary = {}
		self.type = component.getType()
		self.component = component


	def getMinMaxVersion(self, expr, minimum=True):
		if (isinstance(expr, Expression.IntConstant)):
			return expr
		#if expr.getComponent() and expr.getComponent() == self.component:
		#	print("On the first level")
		elif expr.getComponent() and expr.getComponent()!=self.component:
			#print("there is an associated component for " + str(expr))
			if(self.component.getType() == component.componentType.LOOP and expr.getVariable()):
				#print("GOT VAR?")
				mv = expr.getVariable().getMaxValue()
				if minimum == True:
					min_value = expr.getComponent().getMinValue()
					return Operation.Operation(Operation.Operator.LOOPTIMES, [mv,Expression.IntConstant(min_value+expr.getOffset())])
				else:
					max_value = expr.getComponent().getMaxValue()
					return Operation.Operation(Operation.Operator.LOOPTIMES, [mv, Expression.IntConstant(max_value+expr.getOffset())])
			if minimum == True:
				min_value = expr.getComponent().getMinValue()
				return Expression.IntConstant(min_value + expr.getOffset())
			else:
				max_value = expr.getComponent().getMaxValue()
				return Expression.IntConstant(max_value + expr.getOffset())

		if (isinstance(expr, Operation.Operation)):
			op = expr.getOperator()
			ops = expr.getOperands()
			if op == Operation.Operator.BOOLADD:
				res1 = self.getMinMaxVersion(ops[0], minimum = minimum)
				res2 = self.getMinMaxVersion(ops[1], minimum = minimum)
				res = Operation.Operation(Operation.Operator.BOOLADD, [res1, res2])
				return res
			elif op == Operation.Operator.TIMES:
				res1 = self.getMinMaxVersion(ops[1], minimum = minimum)
				return Operation.Operation(Operation.Operator.TIMES, [ops[0], res1])
			elif op == Operation.Operator.ADD:
				res1 = self.getMinMaxVersion(ops[0], minimum = minimum)
				res2 = self.getMinMaxVersion(ops[1], minimum = minimum)
				return Operation.Operation(Operation.Operator.ADD, [res1, res2])
			elif op == Operation.Operator.LOOPTIMES:
				return expr
			elif op == Operation.Operator.LOOPEXPAND:
				res1 = self.getMinMaxVersion(ops[1], minimum=minimum)
				return Operation.Operation(Operation.Operator.LOOPTIMES, [ops[0],res1])
			else:
				print("WARNING: Unexpected operation: " + str(op))


	def expand(self, expr, index =None):
		def _getIdOfMultiplier(expr):
			if isinstance(expr, Operation.Operation):
				ops = expr.getOperands()
				res = _getIdOfMultiplier(ops[0])
				res += '.'
				res+= _getIdOfMultiplier(ops[1])
				return res
			name = str(expr)
			ids = name.split('_')[1]
			return ids

		def _createNewMV(expr, index):
			if isinstance(expr, Operation.Operation):
				if expr.getOperator()!=Operation.Operator.SUB:
					print("WARNING: subtraction expected and not found.")
				ops = expr.getOperands()
				res1 = _createNewMV(ops[0], index)
				res2 = _createNewMV(ops[1], index)
				res = Operation.Operation(Operation.Operator.SUB, [res1, res2])
				return res
			if isinstance(expr, Expression.LoopVariable):
				res = Expression.LoopVariable(str(expr)+ index)
				if expr.isSecret():
					res.setSecret()
				return res 
			if isinstance(expr, Expression.WithinLoopBranchVariable):
				res = Expression.WithinLoopBranchVariable(str(expr)+ index)
				if expr.isSecret():
					res.setSecret()
				return res


		if (isinstance(expr, Expression.IntConstant)):
			return expr
		if (isinstance(expr, Expression.BooleanVariable)):
			return expr
		if (isinstance(expr, Expression.WithinLoopBranchVariable)):
			if index:
				name = str(expr)
				new_expr = Expression.WithinLoopBranchVariable(name + index)
				if expr.isSecret():
					new_expr.setSecret()
				mv = expr.getMaxValue()	
				if isinstance(mv, Operation.Operation):
					mv_new = _createNewMV(mv, index)
					mv = mv_new
				if isinstance(mv, Expression.WithinLoopBranchVariable):
					mv_new = Expression.WithinLoopBranchVariable(str(mv)+index)
					if mv.isSecret():
						mv_new.setSecret()
					mv = mv_new
				elif isinstance(mv, Expression.LoopVariable):
					mv_new = Expression.LoopVariable(str(mv)+ index)
					if mv.isSecret():
						mv_new.setSecret()
					mv = mv_new
				new_expr.setMaxValue(mv)
				return new_expr
			return expr
		if (isinstance(expr, Expression.LoopVariable)):
			if index:
				name = str(expr)
				new_expr = Expression.LoopVariable(name + index)
				if expr.isSecret():
					new_expr.setSecret()
				mv = expr.getMaxValue()
				new_expr.setMaxValue(mv)
				return new_expr
			return expr
		if (isinstance(expr, Operation.Operation)):
			op = expr.getOperator()
			ops = expr.getOperands()
			if op != Operation.Operator.LOOPEXPAND:
				a = self.expand(ops[0], index)
				b = self.expand(ops[1], index)
				return Operation.Operation(op, [a,b])
			else:
				res = []
				extension = "_m" +_getIdOfMultiplier(ops[0])
				for i in xrange(self.LOOPMAXVALUE):
					left_res = self.expand(ops[0], index)
					a = Operation.Operation(Operation.Operator.SUB, [left_res, Expression.IntConstant(i)])
					b = self.expand(ops[1], extension + str(i))
					res.append(Operation.Operation(Operation.Operator.TIMES, [a,b]))
				result = Operation.Operation(Operation.Operator.ADD, [res[-2], res[-1]])
				for i in range(self.LOOPMAXVALUE -3,-1,-1):
					result = Operation.Operation(Operation.Operator.ADD, [res[i], result])
				return result

	def collectVariables(self, expr):
		if (isinstance(expr, Expression.BooleanVariable)):
			self.variables.add(expr)
		if (isinstance(expr, Expression.WithinLoopBranchVariable)):
			self.variables.add(expr)
		if (isinstance(expr, Expression.LoopVariable)):
			self.variables.add(expr)
		if (isinstance(expr, Operation.Operation)):
			op = expr.getOperator()
			ops = expr.getOperands()
			if op == Operation.Operator.ADD:
				self.collectVariables(ops[0])
				self.collectVariables(ops[1])
			if op == Operation.Operator.SUB:
				self.collectVariables(ops[0])
				self.collectVariables(ops[1])
			if op == Operation.Operator.BOOLADD:
				self.collectVariables(ops[0])
				self.collectVariables(ops[1])
			if op == Operation.Operator.TIMES:
				self.collectVariables(ops[0])
				self.collectVariables(ops[1])
			if op == Operation.Operator.LOOPTIMES:
				self.collectVariables(ops[0])
				self.collectVariables(ops[1])
			if op == Operation.Operator.LOOPEXPAND:
				print("WARNING: should already be expanded.")


	def makeVariablesz3(self):

		def _getUpperBoundString(v):
			if isinstance(v, Operation.Operation):
				ops = v.getOperands()
				res = _getUpperBoundString(ops[0])
				res += ' - '
				res+= _getUpperBoundString(ops[1])
				return res
			return "self.allVariablesz3['" + str(v) + "']"

		def _getUpperBoundStringSecret(v, index):
			if isinstance(v, Operation.Operation):
				ops = v.getOperands()
				res = _getUpperBoundStringSecret(ops[0], index)
				res += ' - '
				res+= _getUpperBoundStringSecret(ops[1], index)
				return res
			return "self.allVariablesz3['" + str(v) + "_s" + str(index) + "']"

		for v in self.variables:
			name = str(v)
			self.allVariablesz3[name] = Int(name)
			if (isinstance(v, Expression.BooleanVariable)):
				self.booleanVariablez3[name] = Int(name)
			elif (isinstance(v, Expression.LoopVariable)):
				self.loopVariablez3[name] = Int(name)
			else:
				mv = v.getMaxValue()
				mv_string = _getUpperBoundString(mv)
				self.loopBranchVariablez3[name] = Int(name)
				self.maxValueDictionary[name] = mv_string


	def translate(self, expr):				

		def _setBooleanVariableConstraintsMinimizer():
			for v in self.booleanVariablez3.values():
				self.minimizer.add(v > -1, v < 2)

		def _setBooleanVariableConstraintsMaximizer():
			for v in self.booleanVariablez3.values():
				self.maximizer.add(v > -1, v < 2)

		def _setLoopBranchVariableConstraintsMinimizer():
			for k,v in self.loopBranchVariablez3.items():
				self.minimizer.add(v > -1)
				mv = "v <= " + self.maxValueDictionary[k]
				self.minimizer.add(eval(mv))

		def _setLoopBranchVariableConstraintsMaximizer():
			for k,v in self.loopBranchVariablez3.items():
				self.maximizer.add(v > -1)
				mv = "v <= " + self.maxValueDictionary[k]
				self.maximizer.add(eval(mv))
				

		def _setLoopVariableConstraintsMinimizer():
			for v in self.loopVariablez3.values():
				self.minimizer.add(v > 0)
				self.minimizer.add(v < self.LOOPMAXVALUE)


		def _setLoopVariableConstraintsMaximizer():
			for v in self.loopVariablez3.values():
				self.maximizer.add(v > 0)
				self.maximizer.add(v < self.LOOPMAXVALUE)


		def _generateQuery(expr):
			return expr.toZ3NS()


		def _queryMinimizer(toMin):
			_setBooleanVariableConstraintsMinimizer()
			_setLoopVariableConstraintsMinimizer()
			_setLoopBranchVariableConstraintsMinimizer()

			h = self.minimizer.minimize(eval(toMin))
			print("LOG: Results of minimization query on " + toMin + ":")
			res = self.minimizer.check()
			if res == sat:
				m = self.minimizer.model()
				print(self.minimizer.model())
				return m 
			else:
				print("Unsat")
			print("=========================")


		def _queryMaximizer(toMax):
			_setBooleanVariableConstraintsMaximizer()
			_setLoopVariableConstraintsMaximizer()
			_setLoopBranchVariableConstraintsMaximizer()

			h = self.maximizer.maximize(eval(toMax))
			print("LOG: Results of maximization query on " + toMax + ":")
			res = self.maximizer.check()
			if res == sat:
				m = self.maximizer.model()
				print(self.maximizer.model())
				return m 
			else:
				print("Unsat")
			print("=========================")

		expr_min = expr
		expr_max = expr
		if expr.getComponent().getChildren():
			expr_min = self.getMinMaxVersion(expr)
			expr_max = self.getMinMaxVersion(expr, minimum=False)
	
		expr_min = self.expand(expr_min)
		expr_max = self.expand(expr_max)

		self.collectVariables(expr_min)
		self.makeVariablesz3()
		print(expr_min)
		print(expr_max)
		query_min = _generateQuery(expr_min)
		query_max = _generateQuery(expr_max)
		print("===========\n "+ str(query_min))
		mins = _queryMinimizer(query_min)
		maxs = _queryMaximizer(query_max)
		if not (mins or maxs):
			print("LOG: Branches are equal")
			ans = simplify(eval(query_min)).as_long()
			max_res = {}
			min_res = {}
			for v,k in self.allVariablesz3.items():
				max_res[v] = 0 
				min_res[v] = 1
			return ans, ans, min_res, max_res
		min_res  = {}
		for v,k in self.allVariablesz3.items():
			min_res[v] = mins[k]
			if not mins:
				min_res[v] = 1
		max_res = {}
		for v,k in self.allVariablesz3.items():
			max_res[v] = maxs[k]
			if not maxs:
				max_res[v] = 1
		if mins:
			query_min = expr_min.toZ3min(1)
			min_numeric = simplify(eval(query_min))
			min_numeric = min_numeric.as_long()
			print("LOG: Minimum value that can be taken is : " + str(min_numeric))
		else:
			min_numeric = simplify(eval(query_min)).as_long()
			print("LOG: Minimum value that can be taken is : " + str(min_numeric))


		if maxs:
			query_max = expr_max.toZ3max(1)
			max_numeric = simplify(eval(query_max))
			max_numeric = max_numeric.as_long()
			print("LOG: Max value that can be taken is : " + str(max_numeric))
		else:
			max_numeric = simplify(eval(query_max)).as_long()
			print("LOG: Max value that can be taken is : " + str(max_numeric))

		return min_numeric, max_numeric, min_res, max_res
		

		
		
