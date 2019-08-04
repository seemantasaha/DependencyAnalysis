from expr import Expression, Operation 
from graph import component

def multiply(expr1, expr2):
	if (isinstance(expr2, Expression.IntConstant)):
		mult = Operation.Operation(Operation.Operator.LOOPTIMES, [expr1, expr2])
		return mult
	if (isinstance(expr2, Expression.BooleanVariable)):
		return expr1
		return 
	if (isinstance(expr2, Operation.Operation)):
		op = expr2.getOperator()
		ops = expr2.getOperands()
		if op == Operation.Operator.ADD:
			res1 = multiply(expr1, ops[0])
			res2 = multiply(expr1, ops[1])
			return Operation.Operation(Operation.Operator.ADD, [res1, res2])
		elif op == Operation.Operator.BOOLADD:
			if expr2.getComponent():
				comp = expr2.getComponent()
				if comp.getType() == component.componentType.LOOP:
					res = Operation.Operation(Operation.Operator.LOOPEXPAND, [expr1, expr2])
					return res
			new_loop_var_id = expr2.getComponent().getIdentity()
			new_loop_var = Expression.WithinLoopBranchVariable("l_" + str(new_loop_var_id))
			#check = expr2.getComponent()
			'''print("making new loop var " + str(new_loop_var))
			if isinstance(expr2, Operation.Operation):
				op_expr = expr2.getOperator()
				operands = expr2.getOperands()
				#if op_expr != Operation.Operator.SUB:
				#	print("WARNING")
				#else:
				#	check = operands[0]'''
			if expr2.getComponent().isSecret():
				new_loop_var.setSecret()
			new_loop_var.setMaxValue(expr1)
			new_loop_var.setBoolVersion("b_" + str(new_loop_var_id))
			res1 = multiply(new_loop_var, ops[0])
			res2 = multiply(Operation.Operation(Operation.Operator.SUB, [expr1, new_loop_var]), ops[1])
			res = Operation.Operation(Operation.Operator.BOOLADD, [res1, res2])
			res.setComponent(expr2.getComponent())
			res.increaseOffset(expr2.getOffset())
			res.setVariable(new_loop_var)

			return res
		elif op == Operation.Operator.TIMES:
			res = multiply(expr1, ops[1])
			return res
		elif op == Operation.Operator.LOOPTIMES:
			res = Operation.Operation(Operation.Operator.LOOPEXPAND, [expr1, expr2])
			return res
		elif op == Operation.Operator.LOOPEXPAND:
			res = Operation.Operation(Operation.Operator.LOOPEXPAND, [expr1, expr2])
			return res
		elif op == Operation.Operator.SUB:
			return expr1
		else:
			print("WARNING: NOT HANDLED YET")

	return expr2
