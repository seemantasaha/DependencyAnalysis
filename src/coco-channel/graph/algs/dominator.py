from visitors import dfs

class dominator:

	def __init__(self, g):
		self.g = g
		self.parent = {}
		self.sdom = {}
		self.best = {}
		self.scores = {}
		self.dom = {}
		self.discover_time = {}
		self.B = {}

	def run(self, s):
		def _initialize():
			for n in xrange(len(self.g.nodes.keys())):
				self.parent[n] = n
				self.best[n] = n
				self.sdom[n] = n
				self.B[n] = [] 

		def _find(vertex):#takes in a node 
			u = self.scores[vertex] #id for a node is its position in dfs
			if self.parent[u] != u:
				v = self.parent[u]
				self.parent[u] = _find(self.discover_time[v]) 
				if (self.best[v] < self.best[u]):
					self.best[u] = self.best[v]
			return self.parent[u]


		_initialize()
		search = dfs.dfs(self.g)
		search.visit(s)
		self.scores, self.discover_time = search.getScores()
		dfsTree = search.getTree()
		for u in xrange(len(self.discover_time.keys())-1, -1 ,-1):
			vertex = self.discover_time[u]
			for pred in self.B[u]:
				past = self.sdom[pred]
				_find(self.discover_time[pred])#taking in a node 
				compareTo = self.best[pred]
				if (compareTo < past):
					self.dom[pred] = compareTo
				else:
					self.dom[pred] = u
			for pred in self.g.predecessors.get(vertex,[]):
				v = self.scores[pred]
				_find(pred)
				current = self.sdom[u]
				compareTo = self.best[v]
				if (compareTo < current):
					self.sdom[u] = compareTo
			self.B[self.sdom[u]].append(u)
			self.best[u] = self.sdom[u]
			self.parent[u] = self.scores.get(dfsTree.get(vertex, 0),None)

		for u in xrange(len(self.scores)):
			if u !=0:
				if (self.dom[u]!=self.sdom[u]):
					print("changing " + str(self.discover_time[u]))
					#self.dom[u] = self.dom[self.dom[u]]
					self.dom[u] = self.dom[self.sdom[u]]



	def getDominatorTree(self):
		res = {}
		for u in xrange(len(self.scores)):
			res[self.discover_time[u]] = self.discover_time[self.dom.get(u,0)]
		return res


	def printScores(self):
		for u in xrange(len(self.scores)):
			print("---------------")
			print(self.discover_time[u])
			print(self.discover_time[self.dom.get(u,0)])


