import json, copy
from collections import defaultdict
from visitors import dfs, backwardsDfs, annotationVisitor
from algs import dominator
import node, component
from expr.solver import basicSolver, fastSolver, maxDiffer, MinMax, optimizer

class graph:
	def __init__(self):
		self.nodes = {}
		self.branchNodes = []
		self.entry = None
		self.exit = None
		self.predecessors = {}
		self.ancestors = {}
		self.dominatorTree = {}
		self.postDominatorTree = {}
		self.interestingBranches = []
		self.components = defaultdict(lambda:defaultdict())
		self.cycles = defaultdict(lambda:defaultdict())
		self.branches = defaultdict(lambda:defaultdict())
		self.solving_mode = "naive"

	def makeGraphFromJson(self, fname):
		def _getNode(identity):
			if identity not in self.nodes.keys():
				self.nodes[identity] = node.node(identity)
			return self.nodes[identity]

		def _addEdge(n, toAdd, d):
			if n not in d.keys():
				d[n] = [toAdd]
			else:
				d[n].append(toAdd)

		json_data = open(fname).read()
		data = json.loads(json_data)		
		for d in data:
			identity = d['id'].split()[1]
			currentNode = _getNode(identity)
			if 'instruction_count' in d:
				count = d['instruction_count']
				currentNode.setLength(int(count))
			if 'secret_dependent_branch' in d:
				is_secret = d['secret_dependent_branch']
				if str(is_secret) == "true":
					print("setting secret")
					currentNode.setAsSecret()
			#else:
			#	currentNode.setAsSecret()
			outgoing = d['outgoing'].keys()
			for out in outgoing:
				outNode = _getNode(out)
				_addEdge(currentNode.getId(), outNode.getId(), self.ancestors)
				_addEdge(outNode.getId(), currentNode.getId(), self.predecessors)
			if len(outgoing) > 1:
				self.branchNodes.append(identity)
		self.getEntry()
		self.getExit()


	#Assumes one connected component
	def getEntry(self):
		for n in self.nodes.keys():
			if n not in self.predecessors.keys():
				if not self.entry:
					self.entry = n
				else:
					print("WARNING MORE THAN 1 C ENTRY")

	#Assumes one connected component
	def getExit(self):
		for n in self.nodes.keys():
			if n not in self.ancestors.keys():
				if not self.exit:
					self.exit = n
				else:
					print("WARNING MORE THAN 1 C EXIT")

	def buildDominatorTree(self):
		d = dominator.dominator(self)
		d.run(self.entry)
		self.dominatorTree = d.getDominatorTree()
		return self.dominatorTree

	def buildPostDominatorTree(self):

		def _reverseGraph():
			rGraph = graph()
			rGraph.nodes = self.nodes
			rGraph.entry = self.exit
			rGraph.exit = self.entry
			rGraph.ancestors = self.predecessors
			rGraph.predecessors = self.ancestors
			return rGraph

		rGraph = _reverseGraph()
		self.postDominatorTree = rGraph.buildDominatorTree()
		
	def determineBranches(self):
		def _findBody(branch, merge):
			bdfs = dfs.dfsBranch(self,merge)
			bdfs.visit(branch)
			return bdfs.getPath()

		self.buildPostDominatorTree()
		branches = defaultdict(lambda:defaultdict())
		for branch in self.branchNodes:
			merge = self.postDominatorTree[branch]
			body = _findBody(branch, merge)
			c = component.component(component.componentType.BRANCH, branch, merge)
			c.updateBody(body)
			branches[branch][merge] = c 
		return branches


	def determineCycles(self):
		def _collectBackEdges():
			d = dfs.dfs(self)
			d.visit(self.entry)
			rank = d.getDFS()
			backEdges = {}
			for head,tails in self.ancestors.items():
				for tail in tails:
					if rank[head] > rank[tail]:

						if head in backEdges.keys():
							backEdges[head].append(tail)
						else:
							backEdges[head] = [tail]

			return backEdges

		def _findBody(head, tail):
			bdfs = backwardsDfs.backwardsDfs(self,tail, head)
			bdfs.visit(head)
			return bdfs.getPath()

		cycles = defaultdict(lambda:defaultdict())
		backEdges = _collectBackEdges()
		for k in backEdges.keys():
			for t in backEdges[k]:
				body = _findBody(k,t)			
				c = component.component(component.componentType.LOOP, t, k)
				c.updateBody(body)
				cycles[t][k] = c 

		return cycles

	def findComponents(self):
		def _findLoopBreaks():
			'''determine which branches, taken within the loop, result in leaving the loop'''
			for head in self.branches.keys():
				for loopEntry in self.cycles.values():
					for loopComponent in loopEntry.values():
						body = loopComponent.getBody()
						if head in body:
							tail = self.branches[head]
							add = False
							#if tail.keys()[0] not in body: 
							#NOTE: tail could be in the body!!!
							for nn in self.ancestors[head]:
								if nn not in body:
									add = True
									loopComponent.markExit(head, nn) 
									'''Only marked as an exit if one of the direct ancestors is an exit'''
							if tail.keys()[0] not in body:
								add = True
							if add:
								loopComponent.markExitPath(head)
								'''Marked as an exit path if there is a way to the exit
								TODO: check this'''
								branchComponent = tail.values()[0]
								branchComponent.markLoopBreak()
								branchComponent.setLoopBreakOf(loopComponent)

			for c,v in self.cycles.items():
				for comp in v.values():
					exits = comp.getExits().values()
					for e1 in exits:
						for e2 in exits:
							if e2[0] in self.ancestors[e1[0]]:
								for k in comp.getExits().keys():
									if comp.getExits()[k][0] == e2[0]:
										comp.removeExit(k)
		def _getComponents():
			idNum = 0 
			for key,entry in self.branches.items():
				for comp in entry.values():
					if not comp.isLoopBreak():
						comp.setIdentity(idNum)
						if self.nodes[comp.getHeader()].isSecret():
							comp.setSecret()
							#print("setting component as secret")
						idNum+=1
						self.components[key] = entry
			for key, entry in self.cycles.items():
				for comp in entry.values():
					for b in comp.getExits():
						if self.nodes[b].isSecret():
							print("2 : " + comp.getHeader() + "for" + b)
							comp.appendSecretBranchesInLoopComponent(b)
							comp.setSecret()
							#print("setting loop component as secret!")
					'''Case where the immediate ancestor is not an exit but it is a secret dep branch that leads to an exit'''
					for b in comp.getExitPaths():
						if self.nodes[b].isSecret():
							print("3 : " + comp.getHeader() + "for" + b)
							comp.appendSecretBranchesInLoopComponent(b)
							comp.setSecret()
					comp.setIdentity(idNum)
					idNum+=1
				self.components[key] = entry
			

		def _determineNesting():
			def _visitComponent(comp):
				head = comp.getHeader()
				tail = comp.getTail()
				bdfs = dfs.dfsNestingVisitor(self, tail, comp)
				bdfs.visit(head)
				nested = bdfs.getNested()
				for n in nested:
					comp.updateChildren(n)
					if not n.finishedChildren():
						_visitComponent(n)
				for n1 in nested:
					for n2 in n1.getChildren():
						if n2 in comp.getChildren():
							comp.removeChildren(n2) 
				comp.markChildrenDone()
				comp.initializeVisitableBody()
				for child in comp.getChildren():
					child.setParent(comp)
					for n in child.getBody():
						if n!= comp.getHeader():
							comp.removeFromVisitableBody(n)
							comp.setNodeToChild(n, child)


			for key, entry in self.components.items():
				for comp in entry.values():
					if not comp.finishedChildren():
						nested = _visitComponent(comp)

		self.cycles = self.determineCycles()
		self.branches = self.determineBranches()
		_findLoopBreaks()
		_getComponents()			
		_determineNesting()
		


	'''maybe try splitting this into two'''
	def analyzeComponent(self, comp, known_conditions):
		nestedComponents = comp.getChildren()
		for nc in nestedComponents:
			if not nc.isCostComputed():
				self.analyzeComponent(nc, known_conditions)


		print(comp)
		print(comp.getType())

		cVisitor = annotationVisitor.callingContextVisitor(self, comp)
		cVisitor.visit(comp.getHeader())
		cc = cVisitor.getCallingContext()
		ccr = cVisitor.getCallingContextReversed()
		cr = annotationVisitor.contextRefiner(comp, ccr)
		conflictOrder = cr.refine()
		cVisitor = annotationVisitor.annotateConflictComponent(self, comp, cc, ccr, conflictOrder)
		cVisitor.visit(comp.getHeader())
		aVisitor = annotationVisitor.annotationVisitor(self, comp, cc, ccr,conflictOrder)
		aVisitor.visit(comp.getHeader())
		aVisitor.updateComponentCosts()
		#now send query for this component
		print(comp.getCost())
		self.queryComponent(comp, known_conditions)
		print("=======")

	def costAnalysis(self, known_conditions=None,mode ="naive"):
		if mode!="naive":
			self.solving_mode = mode 
			print("changing solving mode!!!")
		for key, entry in self.components.items():
				for comp in entry.values():
					if not comp.isCostComputed():
						self.analyzeComponent(comp, known_conditions)
		print("=================================")
		print("\n\n\n\n\n\n\n\n\n\n\n")
		print("=================================")
		for key, entry in self.components.items():
			for comp in entry.values():
				if comp.isSideChannel() and comp.isSecret():
					print("FOUND A SIDE CHANNEL")
					print(comp)
					print(comp.getType())
					print("component id : " + str(comp.getHeader()))
					if comp.getType() == component.componentType.LOOP:
						print("Loop component secret dependent branches are :")
						for branch in comp.getSecretBranchesInLoopComponent():
							print(branch)
					print("===================")

	def _shouldBeQueried(self, comp):
		#head = self.nodes[comp.getHeader()]
		if comp.isSecret():
			return True
		elif comp.getType(): 
			for c in comp.getChildren():
				if self._shouldBeQueried(c):
					return True
		return False

	def _willHelpLater(self, comp):
		current = comp 
		while current.getParent():
			if self._shouldBeQueried(current.getParent()): 
				return True 
			current = current.getParent()
		return False 

	def _childSecret(self, comp):
		if comp.isSecret():
			return True
		ans = False
		for c in comp.getChildren():
			ans = ans or self._childSecret(c)
		return ans 

	def _childAlreadySideChannel(self, comp):
		if comp.isSideChannel():
			return True 
		ans = False
		for c in comp.getChildren():
			ans = ans or self._childAlreadySideChannel(c)
		return ans 

	def queryComponent(self, comp, known_conditions):

		def _queryMinMax(comp):
				mm = MinMax.minMax(comp)
				min_n, max_n, min_res, max_res = mm.translate(comp.getCost())
				comp.setMinValue(min_n)
				comp.setMaxValue(max_n)
				comp.setMinSolns(min_res)
				comp.setMaxSolns(max_res)

		def _querySolver(comp):
			if self.solving_mode=="naive" or self.solving_mode.startswith("o"):
				t = basicSolver.basicSolver(comp, known_conditions)
			else:
				print("other solving mode")
				if comp.isSecret():
					t = fastSolver.fastSolver(comp)
				else:
					#call differ
					print("comp itself is not secret, so don't call min maxer")
					print("will call differ instead")
					_queryDiffer(comp, True)
					return None
			res = t.solve(comp.getCost())
			return res

		def _queryDiffer(comp, needRes=False):
			t = maxDiffer.maxDiffer(comp, needRes)
			res = t.solve(comp.getCost())
			if needRes:
				if res:
					comp.setSideChannel()
			return res

		def _queryOptimizer(comp):
			o = optimizer.optimizer(comp, known_conditions)
			o.solve(comp.getCost())
			pass

		#if comp.isNotHandled():
		#	print("not handled case")
		#	return
		if self._childAlreadySideChannel(comp):
			#if comp.getType() == component.componentType.LOOP:
			comp.setSideChannel()
			print("a child is already a side channel")
			#return 
		query = False
		if comp.isSecret():
			query = True 
		elif comp.getType() == component.componentType.LOOP:
			if self._shouldBeQueried(comp):
				query = True  

		if not query:
			print("Component should not be queried!!")
			if self._willHelpLater(comp):
				print("but it will help later!!")
				if self.solving_mode!="naive" and not self.solving_mode.startswith("o") and self._childSecret(comp):
					print("calling differ")
					res = _queryDiffer(comp)
					comp.addMaxDiffSolns(res)
					print(res)
				if self.solving_mode!="naive" and not self.solving_mode.startswith("o"):
					print("calling minmaxer")
					_queryMinMax(comp)
			return 

		print("should query this one!!!")
		if comp.isSecret():
			print("because it itself is a secret")
		print("querying with mode "+ self.solving_mode)
		res = _querySolver(comp)
		print("RESULT FROM CALL WAS " + str(res))
		print("=========================")
		if self.solving_mode.startswith("o"):
			print("will call optimizer now!!")
			_queryOptimizer(comp)
			if res:
				comp.setSideChannel()
				return 
		if res:
			comp.setSideChannel()
			'''if self.solving_mode!="naive" and not self.solving_mode.startswith("o"):
				print("Now calling min max opt for secret")
				_queryMinMax(comp)
			if self.solving_mode!="naive" and not self.solving_mode.startswith("o") and self._willHelpLater(comp):
				print("now calling differ")
				res = _queryDiffer(comp)
				print(res)
				comp.addMaxDiffSolns(res)
				print("=========================")'''
			return

		if self.solving_mode!="naive" and not self.solving_mode.startswith("o") and self._willHelpLater(comp):
			print("now calling differ")
			res = _queryDiffer(comp)
			print(res)
			comp.addMaxDiffSolns(res)
		if self.solving_mode!="naive" and not self.solving_mode.startswith("o"):
			print("Now calling min max opt for secret")
			_queryMinMax(comp)

