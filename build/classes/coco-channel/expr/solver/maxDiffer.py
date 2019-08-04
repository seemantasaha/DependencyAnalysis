from expr import Expression, Operation 
from z3 import *
import ast
from graph import component
import string


class maxDiffer:

	def __init__(self, component, needRes):
		self.variables = set()
		self.booleanVariablez3 = {}
		self.allVariablesz3 = {}
		self.loopVariablez3 = {}
		self.loopBranchVariablez3 = {}
		self.optimize = Optimize()
		self.THRESHOLD = 1000 
		self.LOOPMAXVALUE = 20
		self.min_or_max = {}
		self.min_or_max_loop = {}
		self.component = component
		self.maxValueDictionary = {}
		self.maxValueSeperation = {}
		self.sepCause = {}
		self.storedDiff = {}
		self.marked = {}
		self.loopBranchtoBool = {}
		self.actualMaxValue = {}
		self.needRes = needRes


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
				new_expr.setBoolVersion(expr.getBoolVersion())
				mv = expr.getMaxValue()	
				if isinstance(mv, Operation.Operation):
					mv_new = _createNewMV(mv, index)
					mv = mv_new
				if isinstance(mv, Expression.WithinLoopBranchVariable):
					mv_new = Expression.WithinLoopBranchVariable(str(mv)+index)
					if mv.isSecret():
						mv_new.setSecret()
					mv_new.setBoolVersion(mv.getBoolVersion())
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
			mv = expr.getMaxValue()
			self.collectVariables(mv)
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
				b = v.getBoolVersion()
				if v.isSecret():
					self.loopBranchtoBool[name + "_s1"] = b+"_s1"
					self.loopBranchtoBool[name + "_s2"] = b+"_s2" 
					mv = v.getMaxValue()
					self.actualMaxValue[name+"_s1"] = mv
					self.actualMaxValue[name+"_s2"] = mv
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
					self.loopBranchtoBool[name] = b
					#check if mv is secret
					self.loopBranchVariablez3[name] = Int(name)
					if _boundSecret(mv):
						self.maxValueSeperation[mv] = v
						self.maxValueDictionary[name] = _getUpperBoundStringSecret(mv,2, extra)
					else:
						mv_string = _getUpperBoundString(mv,extra)
						self.maxValueDictionary[name] = mv_string

	def setKnownKnowledge(self):
		def getMv(mv, index = None):
			if isinstance(mv, Operation.Operation):
				r1 = getMv(mv.getOperands()[0], index)
				r2 = getMv(mv.getOperands()[1], index)
				return r1 +" - "+ r2
			name =str(mv)
			if mv.isSecret():
				name = str(mv) +"_s"
				if index ==1:
					name+="1"
				else:
					name+="2"
			if name in self.loopBranchVariablez3.keys():
				if "m" in v:
					i1 =string.find(v,"m")
					i2 = string.rfind(v,"_")
					check = v[0:i1-1]+v[i2:]
				else:
					check = v
				if check in self.storedDiff.keys():
					return self.storedDiff[check]
				else:
					b = self.loopBranchtoBool[name]
					mv_new = self.actualMaxValue[name]
					if b in self.storedDiff.keys():
						cons = self.storedDiff[b]
						if cons == 0:
							return "0" 
						else:
							return getMv(mv_new, index)
			if mv in self.storedDiff.keys():
				return self.storedDiff[mv]
			else:
				if index == 1:
					return "self.allVariablesz3['" + str(mv)+"_s1']"
				if index == 2:
					return "self.allVariablesz3['" + str(mv)+"_s2']"
				return "self.allVariablesz3['" + str(mv) + "']" 

		for v,k in self.booleanVariablez3.items():
			if v in self.storedDiff.keys():
				#print(v)
				#print(self.storedDiff[v])
				if self.storedDiff[v]:
					cons = self.storedDiff[v] 
					self.optimize.add(k == cons)
					self.marked[v] = cons
			else:
				print("LOG: Could not reuse a result for " + str(v))

		for v,k in self.loopVariablez3.items():
			if "m" in v:
				i1 =string.find(v,"m")
				if "s" in v:
					i2 = string.rfind(v,"_")
					check = v[0:i1-1]+v[i2:]
				else:
					check = v[0:i1-1]
			else:
				check = v
			if check in self.storedDiff.keys():
				cons = self.storedDiff[check]
				self.optimize.add(k==cons)
				self.marked[check] = cons
			else:
				print("LOG: Could not reuse a result for " + str(v))


		for v,k in self.loopBranchVariablez3.items():
			if "m" in v:
				i1 =string.find(v,"m")
				if "s" in v:
					i2 = string.rfind(v,"_")
					check = v[0:i1-1]+v[i2:]
				else:
					check = v[0:i1-1]
			else:
				check = v
			if check in self.storedDiff.keys():
				cons = self.storedDiff[check]
				self.optimize.add(k==cons)
				self.marked[v] = cons
			else:
				b = self.loopBranchtoBool[v]
				mv = self.maxValueDictionary[v]
				if b in self.storedDiff.keys():
					cons = self.storedDiff[b]
					if cons == 0:
						self.optimize.add(k==0)
						self.marked[v] = cons
					else:
						self.optimize.add(eval("k== "+ mv))
					self.marked[v] = 1
				else:
					print("LOG: Could not reuse a result for " + str(v))


	def solve(self, expr):

		def _setBooleanVariableConstraintsDiffer():
			for k,v in self.booleanVariablez3.items():
				if k not in self.marked.keys():
					self.optimize.add(v > -1, v < 2)

		def _setLoopBranchVariableConstraintsDiffer():
			for k,v in self.loopBranchVariablez3.items():
				if k not in self.marked.keys():
					self.optimize.add(v > -1)
					mv = "v <= " + self.maxValueDictionary[k]
					self.optimize.add(eval(mv))
				
		def _setLoopVariableConstraintsDiffer():
			for k,v in self.loopVariablez3.items():
				if k not in self.marked.keys():
					self.optimize.add(v > 0)
					self.optimize.add(v < self.LOOPMAXVALUE)


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
			if len(self.maxValueSeperation) == 0:
				query = res1 + " - (" + res2 + ")"
			else:
				for k,v in self.maxValueSeperation.items():
					_getCause(k)
			if len(self.sepCause) == 1:
				extra  = expr.toZ3(1,extra=self.sepCause)
				query = res1 + " + " + extra + " - ( " + res2 + ")"
			elif len(self.sepCause) > 1:
				print("WARNING: debug needed")
			
			return query 

		def _queryDiffer(query):
			self.optimize = Optimize()
			self.setKnownKnowledge()
			_setBooleanVariableConstraintsDiffer()
			_setLoopBranchVariableConstraintsDiffer()
			_setLoopVariableConstraintsDiffer()
			#t = simplify(eval(query))
			h = self.optimize.maximize(eval(query))
			res = self.optimize.check()
			if res == sat:
				m = self.optimize.model()
				return m 

		print("LOG: Using previous results")
		for c in self.component.getChildren():
			r = c.getMaxDiffSolns()
			for v,t in r.items():
				self.storedDiff[v] = t
		
		expr = self.expand(expr)
		self.collectVariables(expr)
		
		self.makeVariablesz3()

		#self.setKnownKnowledge()

		if len(self.maxValueSeperation)>0:
			self.makeVariablesz3(extra=True)
		query = _generateQuery(expr)
		res = _queryDiffer(query)

		diff_res = self.storedDiff
		for v,k in self.allVariablesz3.items():
			diff_res[v] = res[k]

		self.res = diff_res
		query_1 = expr.toZ3Diff(1)
		query_2 = expr.toZ3Diff(2)
		q = query_1 + " - " + query_2
		#print(q)
		if self.needRes:
			max_numeric = simplify(eval(q))
			max_numeric = max_numeric.as_long()
			#print(max_numeric > self.THRESHOLD)
			return max_numeric > self.THRESHOLD
			
		#print(max_numeric)
		return diff_res
		
