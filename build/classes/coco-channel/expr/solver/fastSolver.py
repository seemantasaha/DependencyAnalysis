from expr import Expression, Operation 
from z3 import *
import ast
from graph import component


class fastSolver:

	def __init__(self, component):
		self.variables = set()
		self.booleanVariablez3 = {}
		self.allVariablesz3 = {}
		self.loopVariablez3 = {}
		self.loopBranchVariablez3 = {}
		self.solver = Solver()
		self.THRESHOLD = 50 
		self.LOOPMAXVALUE = 10
		self.min_or_max = {}
		self.min_or_max_loop = {}
		self.component = component
		self.maxValueDictionary = {}


	def determineIfMinMaxLoop(self, expr):
		#This still needs to be completed!
		def _visitChild(expr, index):
			if isinstance(expr, Expression.IntConstant):
				return
			elif expr.getComponent():
				self.min_or_max_loop[expr.getComponent()] = index
				return 
			elif isinstance(expr, Operation.Operation):
				op = expr.getOperator()
				ops = expr.getOperands()
				if op == Operation.Operator.ADD:
					_visitChild(ops[0], index)
					_visitChild(ops[1], index)
				else:
					return expr
			else:
				print("WARNING: Found neither an operation nor a component. Debug needed.")


		if isinstance(expr, Operation.Operation):
			op = expr.getOperator()
			ops = expr.getOperands()
			if op == Operation.Operator.BOOLADD:
				if expr.getVariable():
					if expr.getVariable().getMaxValue():
						_visitChild(expr,0)
						return
				_visitChild(ops[0], 0)
				_visitChild(ops[1], 1)
			elif op == Operation.Operator.ADD:
				#print("calling again on " + str(ops[0]) + " and " + str(ops[1]))
				self.determineIfMinMaxLoop(ops[0])
				self.determineIfMinMaxLoop(ops[1])
			elif op == Operation.Operator.LOOPEXPAND:
				_visitChild(ops[1],0)
			else:
				print("LOG: loop by constant found")
		else:
			print("WARNING: expected an operation. Did not find one.")

	def determineIfMinMax(self, expr):
		def _visitChild(expr, index):
			if isinstance(expr, Expression.IntConstant):
				return
			if expr.getComponent():
				#print("setting for " + str(expr))
				#print(expr.getComponent().getMaxValue())
				self.min_or_max[expr.getComponent()] = index
				return 
			if isinstance(expr, Operation.Operation):
				op = expr.getOperator()
				ops = expr.getOperands()
				if op == Operation.Operator.ADD:
					_visitChild(ops[0], index)
					_visitChild(ops[1], index)
				else:
					print("Warning. Debug needed.")
			else:
				print("IS NOT AN OP AND NO COMP")


		if isinstance(expr, Operation.Operation):
			op = expr.getOperator()
			ops = expr.getOperands()
			if op == Operation.Operator.BOOLADD:
				_visitChild(ops[0].getOperands()[1], 0)
				_visitChild(ops[1].getOperands()[1], 1)
			else:
				print("WARNING: boolean expected but not found.")
		else:
			print("WARNING: operation expected but not found.")


	def getMinMaxVersion(self, expr, minimum=True, loop=None):
		if (isinstance(expr, Expression.IntConstant)):
			return expr
		if expr.getComponent() and (expr.getVariable() or expr.getComponent() != self.component):
			if loop:
				if expr.getVariable():
					mv = expr.getVariable().getMaxValue()
					min_or_max = self.min_or_max_loop[expr.getComponent()]
					if expr.getComponent() == self.component:
						expr = list(expr.getComponent().getChildren())[0].getCost()
					if (min_or_max == 0 and minimum and loop == 1) or (min_or_max == 0 and not minimum and loop == 2):
						min_value = expr.getComponent().getMinValue()
						return Operation.Operation(Operation.Operator.LOOPTIMES, [mv,Expression.IntConstant(min_value + expr.getOffset())])
					elif min_or_max == 0:
						max_value = expr.getComponent().getMaxValue()
						return Operation.Operation(Operation.Operator.LOOPTIMES, [mv,Expression.IntConstant(max_value + expr.getOffset())])
					elif (min_or_max == 1 and minimum and loop == 1) or (min_or_max == 1 and not minimum and loop==1):
						max_value = expr.getComponent().getMaxValue()
						return Operation.Operation(Operation.Operator.LOOPTIMES, [mv,Expression.IntConstant(max_value + expr.getOffset())])
					else:
						min_value = expr.getComponent().getMinValue()
						return Operation.Operation(Operation.Operator.LOOPTIMES, [mv,Expression.IntConstant(min_value + expr.getOffset())])
				else:
					if minimum:
						return Expression.IntConstant(expr.getComponent().getMinValue())
					else:
						return Expression.IntConstant(expr.getComponent().getMaxValue())
					#print(expr.getComponent().getMinValue())
					#print(jk)
			#THIS COMPONENT SHOUlD ALREADY BE SOLVED FOR AND HAVE A MIN O_O
			#get from dictionary
			#print(expr)
			min_or_max = self.min_or_max[expr.getComponent()]
			if min_or_max == 0 and minimum or (min_or_max == 1 and not minimum):
				min_value = expr.getComponent().getMinValue()
				return Expression.IntConstant(min_value + expr.getOffset())
			else:
				max_value = expr.getComponent().getMaxValue()
				return Expression.IntConstant(max_value + expr.getOffset())

		if (isinstance(expr, Operation.Operation)):
			op = expr.getOperator()
			ops = expr.getOperands()
			if op == Operation.Operator.BOOLADD:
				res1 = self.getMinMaxVersion(ops[0], minimum=minimum, loop=loop)
				res2 = self.getMinMaxVersion(ops[1], minimum=minimum, loop= loop)
				res = Operation.Operation(Operation.Operator.BOOLADD, [res1, res2])
				return res
			elif op == Operation.Operator.TIMES:
				res1 = self.getMinMaxVersion(ops[1], minimum=minimum, loop=loop)
				return Operation.Operation(Operation.Operator.TIMES, [ops[0], res1])
			elif op == Operation.Operator.ADD:
				res1 = self.getMinMaxVersion(ops[0], minimum=minimum, loop=loop)
				res2 = self.getMinMaxVersion(ops[1], minimum=minimum, loop=loop)
				return Operation.Operation(Operation.Operator.ADD, [res1, res2])
			elif op == Operation.Operator.LOOPTIMES:
				return expr
			elif op == Operation.Operator.LOOPEXPAND:
				res1 = self.getMinMaxVersion(ops[1], minimum=minimum, loop=loop)
				return Operation.Operation(Operation.Operator.LOOPTIMES, [ops[0],res1])
			else:
				print("WARNING: Unexpected operation found::  " + str(op))

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


	def expand(self,expr, index =None):
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
					#print("MV IS AN OP!!!")
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
				#print("MAKING NEW VARIABlE " + str(new_expr))
				#print("IT'S UPPER BOUND IS " + str(mv))
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
			#if not index and op!=Operation.Operator.LOOPEXPAND:
			#	return expr
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


	def collectVariables(self,expr):
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

		def _boundSecret(v):
			if isinstance(v, Operation.Operation):
				ops = v.getOperands()
				return _boundSecret(ops[1])
			return v.isSecret()

		for v in self.variables:
			name = str(v)
			if v.isSecret():
				self.allVariablesz3[name+"_s1"] = Int(name+"_s1")
				self.allVariablesz3[name+"_s2"] = Int(name+"_s2")
			else:
				self.allVariablesz3[name] = Int(name)
			if (isinstance(v, Expression.BooleanVariable)):
				if v.isSecret():
					self.booleanVariablez3[name+"_s1"] = self.allVariablesz3[name+"_s1"]
					self.booleanVariablez3[name+"_s2"] = self.allVariablesz3[name+"_s2"]
				else:
					self.booleanVariablez3[name] = Int(name)
			elif (isinstance(v, Expression.LoopVariable)):
				if v.isSecret():
					self.loopVariablez3[name+"_s1"] = Int(name+"_s1")
					self.loopVariablez3[name+"_s2"] = Int(name+"_s2")
				else:
					self.loopVariablez3[name] = Int(name)
			else:
				if v.isSecret():
					mv = v.getMaxValue()
					if _boundSecret(mv):
						self.maxValueDictionary[name+"_s1"] = _getUpperBoundStringSecret(mv, 1)
						self.maxValueDictionary[name+"_s2"] = _getUpperBoundStringSecret(mv, 2)
					else:
						mv_string = _getUpperBoundString(mv)
						self.maxValueDictionary[name+"_s1"] = mv_string
						self.maxValueDictionary[name+"_s2"] = mv_string
					self.loopBranchVariablez3[name+"_s1"] = Int(name+"_s1")
					self.loopBranchVariablez3[name+"_s2"] = Int(name+"_s2")
				else:
					mv = v.getMaxValue()
					mv_string = _getUpperBoundString(mv)
					self.loopBranchVariablez3[name] = Int(name)
					self.maxValueDictionary[name] = mv_string


	def solve(self, expr):

		def _setBooleanVariableConstraintsSolver():
			for v in self.booleanVariablez3.values():
				self.solver.add(v > -1, v < 2)

		def _setLoopBranchVariableConstraintsSolver():
			for k,v in self.loopBranchVariablez3.items():
				self.solver.add(v > -1)
				mv = "v <= " + self.maxValueDictionary[k]
				self.solver.add(eval(mv))
				
		def _setLoopVariableConstraintsSolver():
			for v in self.loopVariablez3.values():
				self.solver.add(v > 0)
				self.solver.add(v < self.LOOPMAXVALUE)


		def _generateQuery(expr):
			res1 = expr.toZ3(1)
			res2 = expr.toZ3(2)
			query = res1 + " - (" + res2 + ") > " + str(self.THRESHOLD)
			return query 

		def _querySolver(query):
			self.solver = Solver()
			_setBooleanVariableConstraintsSolver()
			_setLoopBranchVariableConstraintsSolver()
			_setLoopVariableConstraintsSolver()
			t = simplify(eval(query))
			self.solver.add(t)
			res = self.solver.check()
			print("LOG: Results of satisiability query::")
			if res == sat:
				print("There exists a satisfying solution such that the difference in the cost of two paths is greater than " +str(self.THRESHOLD))
				print("One such solution is :")
				print(self.solver.model())
				print("======================")
				return True 
			else:
				print("There does not exist a satisfying solution such that the difference in the cost of two paths is greater than " +str(self.THRESHOLD))
				return False
				print("======================")


		def _handleLoop(expr):
			self.determineIfMinMaxLoop(expr)
			v1 = self.getMinMaxVersion(expr, minimum=True, loop=2)
			v2 = self.getMinMaxVersion(expr, minimum=True,loop=1)
			v3 = self.getMinMaxVersion(expr, minimum=False, loop=2)
			v4 = self.getMinMaxVersion(expr, minimum=False, loop=1)
			v1 = self.expand(v1)
			v2 = self.expand(v2)
			v3 = self.expand(v3)
			v4 = self.expand(v4)
			self.collectVariables(v1)
			self.makeVariablesz3()
			v1_query = _generateQuery(v1)
			#print(v1_query)
			res_v1 = _querySolver(v1_query)
			v2_query = _generateQuery(v2)
			#print(v2_query)
			res_v2 = _querySolver(v2_query)
			v3_query = _generateQuery(v3)
			#print(v3_query)
			res_v3 = _querySolver(v3_query)
			v4_query = _generateQuery(v4)
			#print(v4_query)
			res_v4 = _querySolver(v4_query)
			return res_v1 or res_v2 or res_v3 or res_v4


		if expr.getComponent().getType() == component.componentType.BRANCH:
			self.determineIfMinMax(expr)
			expr_min = self.getMinMaxVersion(expr, minimum = True)
			expr_max = self.getMinMaxVersion(expr, minimum = False)
		else:
			if not expr.getComponent().getChildren():
				expr_min = expr 
				expr_max = expr
			else:
				children = expr.getComponent().getChildren()
				#print("calling handle loop")
				return _handleLoop(expr)

		expr_min = self.expand(expr_min)
		expr_max = self.expand(expr_max)
		self.collectVariables(expr_min)
		
		self.makeVariablesz3()
		#print(expr_min)
		queryMin = _generateQuery(expr_min)
		#print(expr_max)
		queryMax = _generateQuery(expr_max)
		res_min = _querySolver(queryMin)
		res_max = _querySolver(queryMax)
		return res_min or res_max
		
