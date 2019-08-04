from expr import Expression, Operation 
from graph import component

def add(expr1, expr2):
	def _addExpr(expr1, expr2):
		return Operation.Operation(Operation.Operator.ADD, [expr1, expr2])

	def _addConst(expr1, expr2):
		cons = expr1.getValue()
		if isinstance(expr2, Expression.IntConstant):
			cons2 = expr2.getValue()
			return Expression.IntConstant(cons+cons2)
		elif isinstance(expr2, Expression.LoopVariable):
			return expr2
		elif isinstance(expr2, Expression.WithinLoopBranchVariable):
			return expr2
		elif isinstance(expr2, Expression.BooleanVariable):
			return expr2
		elif isinstance(expr2, Operation.Operation):
			if expr2.getComponent():
				comp = expr2.getComponent()
				if comp.getType() == component.componentType.LOOP:
					return Operation.Operation(Operation.Operator.ADD, [expr1, expr2])
			op = expr2.getOperator()
			operands = expr2.getOperands()
			if op == Operation.Operator.ADD:
				#The two components are independent so I only need add to one!
				res = _addConst(expr1, operands[0])
				combinedRes = Operation.Operation(op, [res, operands[1]])
				return combinedRes
			if op == Operation.Operator.SUB:
				return expr2
			res = []
			for o in operands:
				r = _addConst(expr1, o)
				if r:
					res.append(r)
			combinedRes = Operation.Operation(op, res)
			combinedRes.setComponent(expr2.getComponent())
			if op == Operation.Operator.BOOLADD:
				offset = expr2.getOffset() + expr1.getValue()
				combinedRes.increaseOffset(offset)
			return combinedRes
			
	if isinstance(expr1, Expression.IntConstant):
		return _addConst(expr1, expr2)
	elif isinstance(expr2, Expression.IntConstant):
		return _addConst(expr2, expr1)
	else:
		return _addExpr(expr1, expr2)

