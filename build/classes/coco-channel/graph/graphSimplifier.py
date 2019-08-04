import json, copy
from collections import defaultdict
from visitors import dfs, backwardsDfs
from algs import dominator
import node, component

class graphSimplifier:
	def __init__(self):
		self.nodes = {}
		self.branchNodes = []
		self.entry = None 
		self.exit = None
		self.predecessors = {}
		self.ancestors = {}
		self.dominatorTree = {}
		self.postDominatorTree = {}
		self.components = defaultdict(lambda:defaultdict())
		self.cycles = defaultdict(lambda:defaultdict())
		self.branches = defaultdict(lambda:defaultdict())


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
			if 'cost' in d:
				count = d['cost']
				currentNode.setLength(int(count))
			elif 'instruction_count' in d:
				count = d['instruction_count']
				currentNode.setLength(int(count))
			if 'secret_dependent_branch' in d:
				is_secret = d['secret_dependent_branch']
				if str(is_secret) == "true":
					#print("secret set")
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

	def simplify(self):

		def _makeSimplerGraph(basic_blocks, gSimple):
			def _getNode(identity, gSimple):
				if identity not in gSimple.nodes.keys():
					gSimple.nodes[identity] = node.node(identity)
					if self.nodes[identity].isSecret():
						gSimple.nodes[identity].setAsSecret()
				return gSimple.nodes[identity]

			def _addEdge(n, toAdd, d):
				if n not in d.keys():
					d[n] = [toAdd]
				else:
					d[n].append(toAdd)

			def _getLength(n):
				return self.nodes[n].getLength()

			def _getType(n):
				if n in self.ancestors.keys():
					if len(self.ancestors[n])>1:
						return 0 #branch node
				if n in self.predecessors.keys():
					if len(self.predecessors[n])>1:
						return 1 # merge node
				if n not in self.ancestors.keys():
					return 3 #last node
				if n not in self.predecessors.keys():
					return 4 #first node
				return 2

			for n in basic_blocks:
				_getNode(n, gSimple)

			for n in basic_blocks:
				#print(n)
				#first set edges
				if n in self.ancestors.keys():
					for a in self.ancestors[n]:
						current = a 
						while current not in basic_blocks:
							current = self.ancestors[current][0]
						_addEdge(n, current, gSimple.ancestors)


				length = _getLength(n)
				t = _getType(n)
				#print(n)
				#print(t)
				'''if t == 0: #BRANCH
					for a in self.predecessors[n]:
						current = a
						while current not in basic_blocks:
							print("adding the cost of " + str(current))
							length+=_getLength(current)
							current = self.predecessors[current][0]'''
				if t == 2 or t == 4: #RES OF JUMP OR FIRST NODE
					for a in self.ancestors[n]:
						current = a 
						while current not in basic_blocks:
							#print("adding the cost of " + str(current))
							length+=_getLength(current)
							current = self.ancestors[current][0]

				if t == 3: #LAST NODE
					for a in self.predecessors[n]:
						current = a
						while current not in basic_blocks:
							#print("adding the cost of " + str(current))
							length+=_getLength(current)
							current = self.predecessors[current][0]
				gSimple.nodes[n].setLength(length)		


			for n in basic_blocks:
				if n in self.predecessors.keys():
					for p in self.predecessors[n]:
						current = p 
						while current not in basic_blocks:
							if len(self.predecessors[current]) > 1:
								print("WARNING: too many predecessors!")
							current = self.predecessors[current][0]
						_addEdge(n, current, gSimple.predecessors)
				
			return gSimple

		def _getBasicBlocks():
			basic_blocks = set()
			for n in self.nodes.keys():
				if n not in self.predecessors.keys():
					basic_blocks.add(n)
					continue
				if n not in self.ancestors.keys():
					basic_blocks.add(n)
					continue
				if len(self.predecessors[n]) > 1:
					basic_blocks.add(n)
				if len(self.ancestors[n]) > 1:
					basic_blocks.add(n)
					for p in self.ancestors[n]:
						basic_blocks.add(p)
			return basic_blocks

		basic_blocks = _getBasicBlocks()
		gSimple = graphSimplifier()
		gSimple =_makeSimplerGraph(basic_blocks, gSimple)
		return gSimple


	#Assumes one connected component
	def getEntry(self):
		for n in self.nodes.keys():
			if n not in self.predecessors.keys():
				#print(n)
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
			rGraph = graphSimplifier()
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
			for head in self.branches.keys():
				for loopEntry in self.cycles.values():
					for loopComponent in loopEntry.values():
						body = loopComponent.getBody()
						if head in body:
							tail = self.branches[head]
							if tail.keys()[0] not in body:
								branchComponent = tail.values()[0]
								branchComponent.markLoopBreak()
								for nn in self.ancestors[head]:
									if nn not in body:
										loopComponent.markExit(head, nn)
		
		def _getComponents():
			idNum = 0 
			for key,entry in self.branches.items():
				for comp in entry.values():
					if not comp.isLoopBreak():
						comp.setIdentity(idNum)
						idNum+=1
						self.components[key] = entry
			for key, entry in self.cycles.items():
				for comp in entry.values():
					comp.setIdentity(idNum)
					idNum+=1
				self.components[key] = entry
	

		self.cycles = self.determineCycles()
		self.branches = self.determineBranches()
		_findLoopBreaks()
		_getComponents()


	def checkAndFixConflicts(self):
		def _limitedOutgoing():
			for n in self.nodes.keys():
				if n in self.ancestors.keys():
					outgoing = self.ancestors[n]
					if len(outgoing) > 2:
						print("WARNING: too many outgoing!!")


		def _compare(comp1, comp2):
			h1 = comp1.getHeader()
			h2 = comp2.getHeader()
			t1 = comp1.getTail()
			t2 = comp2.getTail()
			if h1 in comp2.getBody() and t1 in comp2.getBody():
				return False
			if h2 in comp1.getBody() and t2 in comp1.getBody():
				return False
			if h1 in comp2.getBody() and h1!=t2 and t1 not in comp2.getBody():
				print("LOG: Found unsupported CFG structure! Re-formatting CFG")
				#print(comp1)
				#print(comp2)
				self.ancestors[t1].append(t2)
				self.predecessors[t2].append(t1)
				self.ancestors[t1].remove(h1)
				self.predecessors[h1].remove(t1)
				#self.ancestors[t1].append(t2)
				#self.predecessors[t2].append(t1)
				return True
			if h2 in comp1.getBody() and h2!=t1 and t2 not in comp1.getBody():
				print("LOG: Found unsupported CFG structure! Re-formatting CFG")
				#print(comp1)
				#print(comp2)
				self.ancestors[t2].append(t1)
				self.predecessors[t1].append(t2)
				self.ancestors[t2].remove(h2)
				self.predecessors[h2].remove(t2)
				#self.ancestors[t2].append(t1)
				#self.predecessors[t1].append(t2)
				return True
			return False


		self.findComponents()
		check = True 
		while (check):
			self.components = defaultdict(lambda:defaultdict())
			self.cycles = defaultdict(lambda:defaultdict())
			self.branches = defaultdict(lambda:defaultdict())
			self.findComponents()

			print("LOG: resetting computation for iterative check")
			check = False
			compared = defaultdict(lambda:defaultdict())
			for c,v in self.cycles.items():
				if check:
					break
				for comp in v.values():
					#BRUTE FORCE
					if check:
						break  
					for c2,v2 in self.cycles.items():
						if check:
							break
						for comp2 in v2.values():
							if check:
								break
							if comp!=comp2 and not comp2 in compared[comp].keys():
								compared[comp][comp2] = True
								compared[comp2][comp] = True
								#should not fix on the fly! should mark to be fixed as later might not need to 
								if _compare(comp, comp2):
									#print("found conflict")
									#print(comp)
									#print(comp2)
									check = True
									break 
		#_limitedOutgoing()

