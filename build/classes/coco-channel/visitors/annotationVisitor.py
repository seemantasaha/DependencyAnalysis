from expr.helper.addition import *
from expr.helper.multiplication import *
from collections import defaultdict 
from expr import Expression, Operation 
from graph import component


class callingContextVisitor:

	def __init__(self, g, component):
		self.graph = g
		self.seen = {}
		self.component = component 
		self.stop = component.getTail()
		self.visitable = component.getVisitableBody()
		self.callingContext = defaultdict(list)
		self.callingContextReversed = defaultdict(list)

	def visit(self, n):
		def _getCorrectComponent(n):
			for comp in self.graph.components[n].values():
				add = True
				for n in comp.getBody():
					if n not in self.component.getBody():
						add = False
				if add:
					while add:
						if comp.getParent() and comp.getParent().getHeader() == n:
							c = comp.getParent()
							for n in c.getBody():
								if not n in self.component.getBody():
									add = False
						else:
							add = False
						if add:
							comp =  c
					return comp  
			return None



		def _visitAncestors(n):
			if n == self.component.getHeader():
				for c in self.component.getChildren():
					if c.getHeader() == n:
						exit_to = c.getTail()
						self.callingContext[exit_to].append(n) 
						self.callingContextReversed[n].append(exit_to)
						self.visit(exit_to)
						return

			if n not in self.visitable:
				if n in self.graph.components.keys():
					c = _getCorrectComponent(n)
					if c and c.getHeader() == n:
						comp = c
						if comp.getType() == component.componentType.LOOP:
							breaks = comp.getExits()
							for exit in breaks.keys():
								if exit in self.component.getBody():
									#Should really check if exit is another child 
									child = False
									if exit in self.graph.components.keys() and exit != n and n not in self.callingContext[exit]:
										for ch in self.component.getChildren():
											if ch.getHeader() == exit:
												child = True
									if child: 
										exit_to = exit
										self.callingContext[exit_to].append(n)
										self.callingContextReversed[n].append(exit_to)
										self.visit(exit_to)
									elif n not in self.callingContext[exit]: 
										exit_to = breaks[exit][0]

										self.callingContext[exit_to].append(n)
										self.callingContextReversed[n].append(exit_to)
										self.visit(exit_to)
						else:
							self.callingContext[comp.getTail()].append(n)
							self.callingContextReversed[n].append(comp.getTail())
							self.visit(comp.getTail())
						return 
			if n in self.graph.ancestors.keys():
				for out in self.graph.ancestors[n]:
					if out!= self.component.getHeader() and out in self.component.getBody():
						self.callingContext[out].append(n)
						self.callingContextReversed[n].append(out)
						if out not in self.seen.keys():
							self.visit(out)

		self.seen[n] = 1
		_visitAncestors(n)

	def getCallingContext(self):
		return self.callingContext

	def getCallingContextReversed(self):
		return self.callingContextReversed

class contextRefiner:

	def __init__(self, component, callingContextReversed):
		self.component = component
		self.callingContextReversed = callingContextReversed
		self.callingContextFirst = defaultdict(list)
		self.touches = defaultdict(set)
		self.seen = defaultdict(set)
		self.conflict = []
		self.conflictOrder = {}

	def _touches(self, current, entry):
		self.seen[entry].add(current)
		for n in self.callingContextReversed[current]:
			self.touches[entry].add(n)
			if n not in self.seen[entry]:
				self._touches(n, entry)

	def _order(self, current):
		self.seen["fake"].add(current)
		if current in self.conflict:
			self.conflictOrder[current] = len(self.conflictOrder)
		for n in self.callingContextReversed[current]:
			if n not in self.seen["fake"]:
				self._order(n)


	def refine(self):
		for entry in self.callingContextReversed.keys():
			touches = self._touches(entry, entry)
			if entry in self.touches[entry]:
				self.conflict.append(entry)
		self._order(self.component.getHeader())
		return self.conflictOrder


class annotateConflictComponent:

	def __init__(self, g, component, callingContext, callingContextReversed, conflictOrder):
		self.graph = g
		self.component = component
		self.callingContext = callingContext
		self.callingContextReversed = callingContextReversed
		self.conflictOrder = conflictOrder
		self.seen = {}

	def visit(self, n):


		def _computeCost(n):
			preds = set()
			for c in self.callingContext[n]:
				if c in self.conflictOrder.keys():
					if self.conflictOrder[c] > self.conflictOrder[n]:
						preds.add(c)
			if len(preds) > 1:
				print("LOG: WARNING IN CONFLICT COMPUTATION -- too many predecessors. Structure not handled!")
			if len(preds) == 1:
				cost_current = self.graph.nodes[n].getCost()
				past_cost = self.graph.nodes[preds.pop()].getCost()
				cost = add(cost_current, past_cost)
				self.graph.nodes[n].setCost(cost)


		def _visitAncestors(n):
			for c in self.callingContextReversed[n]:
				if c not in self.seen.keys():
					self.visit(c)

		self.seen[n] = True 
		_visitAncestors(n)
		if n in self.conflictOrder.keys():
			_computeCost(n)




class annotationVisitor:

	def __init__(self, g, component, callingContext, callingContextReversed, conflictOrder):
		self.graph = g
		self.stop = component.getTail()
		self.visitable = component.getVisitableBody()
		self.seen = {}
		self.component = component
		self.callingContext = callingContext
		self.callingContextReversed = callingContextReversed
		self.conflictOrder = conflictOrder

	def _getCorrectComponent(self, n):
		for comp in self.graph.components[n].values():
			add = True
			for n in comp.getBody():
				if n not in self.component.getBody():
					add = False
				if add:
					while add:
						if comp.getParent() and comp.getParent().getHeader() == n:
							c = comp.getParent()
							for n in c.getBody():
								if not n in self.component.getBody():
									add = False
						else:
							add = False
						if add:
							comp =  c
					return comp  
		return None


	def visit(self, n):
		def _readyToVisit(n):
			preds = self.callingContext[n]
			for p in preds:
				if p not in self.seen.keys():
					if n not in self.conflictOrder.keys():
						return False
					if p not in self.conflictOrder.keys():
						return False

			return True 

		def _getCostOfPrevious(n):
			if n in self.graph.components.keys():
				comp = self._getCorrectComponent(n)
				if comp and comp.isCostComputed():
					if (comp.getTail() in self.component.getBody()):
						cc_expression = comp.getCost()
						return cc_expression
			nNode = self.graph.nodes[n]
			current_cost = nNode.getLength()
			cc_expression = Expression.IntConstant(current_cost)
			return cc_expression


		def _getEnteringCosts(n, preds, cc_expression):
			costs = []
			preds.sort()
			for p in preds:
				pNode = self.graph.nodes[p]
				pNodeCost = pNode.getCost()
				toAdd = True
				if p in self.graph.components.keys():
					comp = self._getCorrectComponent(p)
					toAdd2 = True
					if comp and comp.isCostComputed(): #Is the else ever possible??
						toAdd = False
						if (self.graph.components[p].keys()[0]) ==n:
							#print("DO NOT DOUBLE COUNT")
							costs.append(pNodeCost)
							continue
						if (n in self.graph.components.keys()):
							for c2 in self.component.getChildren():
								if c2.getHeader() == n:
									comp2 = c2
									if comp2.getTail() in self.component.getBody():
										toAdd2=False
										costs.append(add(cc_expression, pNodeCost))
						if toAdd2:
							costs.append(add(cc_expression,pNodeCost)) #Still to confirm if pNodeCost or cost of component
							#costs.append(pNodeCost) 
				if toAdd:
					costs.append(add(cc_expression, pNodeCost))
			return costs 

		def _checkIfOverlappingComponent(n):
			for c in self.component.getChildren():
				if c.getHeader() == n:
					return c 
			return None 

		def _computeCost(n):
			preds = list(set(self.callingContext[n])) #make it a set to ensure the order matches
			nNode = self.graph.nodes[n]
			if n in self.conflictOrder.keys():
				#print("IS CONFLICT KEY")
				return 
			if len(preds) == 0:
				if not _checkIfOverlappingComponent(n):
					nNode.setCost(Expression.IntConstant(nNode.getLength()))
					return
				else:
					#nNode.setCost(Expression.IntConstant(nNode.getLength()))
					return
			cc_expression = _getCostOfPrevious(n)
			costs = _getEnteringCosts(n, preds, cc_expression)

			if len(costs) == 1:
				nNode.setCost(costs[0])
			elif len(costs) == 2:
				if self.component.getType() == component.componentType.LOOP:
					print("LOG: More than one incoming edge for a loop. Might be incorrectly handled.")
					cost = Operation.Operation(Operation.Operator.ADD, [costs[0], costs[1]])
					nNode.setCost(cost)
					return
				b_id = self.component.getIdentity()
				head = self.graph.nodes[self.component.getHeader()]
				bv = Expression.BooleanVariable("b_" +str(b_id))
				if (head.isSecret()):
					bv.setSecret()
				cost1 = Operation.Operation(Operation.Operator.TIMES, [bv, costs[0]])
				bv_neg = Operation.Operation(Operation.Operator.SUB, [Expression.IntConstant(1), bv])
				cost2 = Operation.Operation(Operation.Operator.TIMES, [bv_neg, costs[1]])
				cost = Operation.Operation(Operation.Operator.BOOLADD, [cost1, cost2])
				cost.setComponent(self.component)
				nNode.setCost(cost)
			else:
				#print("WARNING!!! MORE THAN TWO PREDS!!")
				#for c in costs:
				#	print(c)
				if n!=self.component.getTail():
					print("LOG: Warning -- structure may be unsupported.")
					self.component.setNotHandled()
				if self.component.getType() == component.componentType.LOOP:
					res = Operation.Operation(Operation.Operator.ADD, [costs[-2], costs[-1]])
					for i in range(len(costs)-3, -1, -1):
						res = Operation.Operation(Operation.Operator.ADD, [costs[i], res])
					#print(res)
					cost = res
					cost.setComponent(self.component)
					nNode.setCost(cost)
				else:
					b_id = self.component.getIdentity()
					head = self.graph.nodes[self.component.getHeader()]
					bv = Expression.BooleanVariable("b_" +str(b_id) + "_0")
					if (head.isSecret()):
						bv.setSecret()
					cost1 = Operation.Operation(Operation.Operator.TIMES, [bv, costs[-1]])
					bv_neg = Operation.Operation(Operation.Operator.SUB, [Expression.IntConstant(1), bv])
					cost2 = Operation.Operation(Operation.Operator.TIMES, [bv_neg, costs[1]])
					cost = Operation.Operation(Operation.Operator.BOOLADD, [cost1, cost2])
					for i in range(len(costs) -1, -1, -1):
						bv = Expression.BooleanVariable("b_" +str(b_id) + "_" + str(i))
						if (head.isSecret()):
							bv.setSecret()
						cost1 = Operation.Operation(Operation.Operator.TIMES, [bv, costs[i]])
						bv_neg = Operation.Operation(Operation.Operator.SUB, [Expression.IntConstant(1), bv])
						cost2 = Operation.Operation(Operation.Operator.TIMES, [bv_neg, cost])
						cost = Operation.Operation(Operation.Operator.BOOLADD, [cost1, cost2])
					cost.setComponent(self.component)
					nNode.setCost(cost)

		def _visitAncestors(n):
			for child in self.callingContextReversed[n]:
				if not child in self.seen.keys():
					if child not in self.conflictOrder.keys():
						self.visit(child)
					elif n not in self.conflictOrder.keys():
						self.visit(child)

		ready = _readyToVisit(n)
		if not ready:
			return
		self.seen[n] = 1
		_computeCost(n)
		_visitAncestors(n)
		

	def updateComponentCosts(self):
		def _setIndepComponentsCost(cost):
			cost.setComponent(self.component)
			if isinstance(cost, Operation.Operation):
				op = cost.getOperator()
				operands = cost.getOperands()
				if op == Operation.Operator.ADD:
					_setIndepComponentsCost(operands[0])
					_setIndepComponentsCost(operands[1])


		def _handleLoop(cost):
			l_id = self.component.getIdentity()
			lv = Expression.LoopVariable("k_" +str(l_id))
			if self.component.isSecret():
				print("LOG: Setting " + str(lv) + " to secret")
				lv.setSecret()
			return multiply(lv, cost)

		def _resetCosts():
			for c, v in self.graph.components.items():
				for comp in v.values():
					if comp.isCostComputed():
						h = comp.getHeader()
						self.graph.nodes[h].setCost(comp.getCost())
						add = True
						while add:
							if comp.getParent():
								comp = comp.getParent()
								if comp.isCostComputed():
									h = comp.getHeader()
									self.graph.nodes[h].setCost(comp.getCost())
								else:
									add = False
							else:
								add = False 


		tail = self.component.getTail()
		cost = self.graph.nodes[tail].getCost()
		if self.component.getType() == component.componentType.LOOP:
			cost = _handleLoop(cost)	
		cost.setComponent(self.component)
		_setIndepComponentsCost(cost)
		self.component.setCost(cost)
		self.graph.nodes[self.component.getHeader()].setCost(cost)
		_resetCosts()

