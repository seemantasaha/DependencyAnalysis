class dfs:

	def __init__(self, g):
		self.graph = g 
		self.seen = {}
		self.discoverTime = {}
		self.parent = {}
		self.dfn = {}
		self.counter = len(self.graph.nodes) -1

	def visit(self, n):

		self.seen[n] = len(self.seen)
		self.discoverTime[self.seen[n]] = n
		if n in self.graph.ancestors.keys():
			for out in self.graph.ancestors[n]:
				if out not in self.seen.keys():
					self.parent[out] = n	
					self.visit(out)
		self.dfn[n] = self.counter
		self.counter = self.counter -1

	def getScores(self):
		return self.seen, self.discoverTime

	def getTree(self):
		return self.parent

	def getDFS(self):
		return self.dfn

class dfsBranch:

	def __init__(self, g, stop):
		self.graph = g
		self.stop = stop
		#self.branch = {}
		self.seen = {stop:1}

	def visit(self, n):
		self.seen[n] = 1
		if n in self.graph.ancestors.keys():
			for out in self.graph.ancestors[n]:
				if out not in self.seen.keys() and out!=self.stop:
					self.visit(out)
		

	def getPath(self):
		return self.seen.keys()


class dfsNestingVisitor:

	def __init__(self, g, stop, comp):
		self.graph = g
		self.stop = stop
		self.nestedHeads = []
		self.visitable = comp.getBody()
		self.seen = {stop:1}
		self.component = comp 

	def visit(self, n):
		if n in self.graph.components.keys():
			for v in self.graph.components[n].values():
				if v == self.component:
					continue
				add = True
				for b in v.getBody():
					if b not in self.visitable:
						add = False
				if add:
					self.nestedHeads.append(v)

		self.seen[n] = 1
		if n in self.graph.ancestors.keys():
			for out in self.graph.ancestors[n]:
				if out not in self.seen.keys() and out !=self.stop and out in self.visitable:
					self.visit(out)

		
	def getNested(self):
		return self.nestedHeads