from expr import Expression, Operation 
from z3 import *
import ast
from graph import component


class basicSolver:

	def __init__(self, component, known_conditions=None):
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
		self.maxValueSeperation = {}
		self.sepCause = {}
		self.known_conditions = known_conditions




	def expand(self,expr, index =None):

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
					print("WARNING: Operation expected: Subtraction. Found: Other.")
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
			#mv = expr.getMaxValue()
			#self.collectVariables(mv)
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
				print("WARNING: Expression should already be expanded!")
					
	def makeVariablesz3(self, extra =False):
		def _getUpperBoundString(v, extra=False):
			if isinstance(v, Operation.Operation):
				ops = v.getOperands()
				res = _getUpperBoundString(ops[0], extra=extra)
				res += ' - '
				res+= _getUpperBoundString(ops[1], extra = extra)
				return res
			if not extra:
				return "self.allVariablesz3['" + str(v) + "']"
			return "self.allVariablesz3['" + str(v)+"_ex"+"']" 

		def _getUpperBoundStringSecret(v, index, extra=False):
			if isinstance(v, Operation.Operation):
				ops = v.getOperands()
				if _boundSecret(ops[0]):
					res = _getUpperBoundStringSecret(ops[0], index, extra)
				else:
					res=_getUpperBoundString(ops[0], extra)
				res += ' - '
				if _boundSecret(ops[1]):
					res+= _getUpperBoundStringSecret(ops[1], index, extra=extra)
				else:
					res+=_getUpperBoundString(ops[1], extra=extra)
				return res
			if not extra:
				return "self.allVariablesz3['" + str(v) + "_s" + str(index) + "']"
			elif v in self.maxValueSeperation.keys():
				return "self.allVariablesz3['" + str(v) + "_s1" + "'] - self.allVariablesz3['" + str(v) + "_s2" +"']"
			else:
				return "self.allVariablesz3['" + str(v) +"_ex_s" + str(index)+"']"


		def _boundSecret(v):
			if isinstance(v, Operation.Operation):
				ops = v.getOperands()
				return _boundSecret(ops[1]) or _boundSecret(ops[0])
			return v.isSecret()

		for v in self.variables:
			if extra:
				if v in self.maxValueSeperation.keys():
					continue
				name = str(v) +"_ex"
			else:
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
						self.maxValueDictionary[name+"_s1"] = _getUpperBoundStringSecret(mv, 1, extra) 
						self.maxValueDictionary[name+"_s2"] = _getUpperBoundStringSecret(mv, 2, extra)
					else:
						mv_string = _getUpperBoundString(mv,extra)
						self.maxValueDictionary[name+"_s1"] = mv_string
						self.maxValueDictionary[name+"_s2"] = mv_string
					self.loopBranchVariablez3[name+"_s1"] = Int(name+"_s1")
					self.loopBranchVariablez3[name+"_s2"] = Int(name+"_s2")
				else:
					mv = v.getMaxValue()
					#check if mv is secret
					self.loopBranchVariablez3[name] = Int(name)
					if _boundSecret(mv):
						self.maxValueSeperation[mv] = v
						self.maxValueDictionary[name] = _getUpperBoundStringSecret(mv,2, extra)
					else:
						mv_string = _getUpperBoundString(mv,extra)
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
			def _getCause(expr):
				if isinstance(expr, Operation.Operation):
					_getCause(expr.getOperands()[0])
					_getCause(expr.getOperands()[1])
					return 
				if expr.isSecret():
					self.sepCause[expr] = 1


			res1 = expr.toZ3(1)
			res2 = expr.toZ3(2)
			#TODO: What is this again?
			if len(self.maxValueSeperation) == 0:
				query = res1 + " - (" + res2 + ") > " + str(self.THRESHOLD)
			else:
				print("WARNING: Debug needed.")
				for k,v in self.maxValueSeperation.items():
					_getCause(k)
			if len(self.sepCause) == 1:
				extra  = expr.toZ3(1,extra=self.sepCause)
				query = res1 + " + " + extra + " - ( " + res2 + ") > " + str(self.THRESHOLD)
			elif len(self.sepCause) > 1:
				print("WARNING: Debug needed.")
			
			return query 

		def _querySolver(query):
			self.solver = Solver()
			_setBooleanVariableConstraintsSolver()
			_setLoopBranchVariableConstraintsSolver()
			_setLoopVariableConstraintsSolver()
			if self.known_conditions:
				for c in self.known_conditions:
					if c[0] not in self.allVariablesz3.keys():
						continue
					if c[1] not in self.allVariablesz3.keys():
						continue
					self.solver.add(eval(c[2]))
			#print(query)
			t = simplify(eval(query))
			self.solver.add(t)
			#print(self.solver)
			res = self.solver.check()
			print("LOG: Results of satisiability query:")
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


		
		expr = self.expand(expr)
		self.collectVariables(expr)
		
		self.makeVariablesz3()
		if len(self.maxValueSeperation)>0:
			self.makeVariablesz3(extra=True)
		query = _generateQuery(expr)
		res = _querySolver(query)
		return res
		
