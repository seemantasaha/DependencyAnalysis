from collections import defaultdict

class backwardsDfs:

	def __init__(self, g, stop, start):
		self.graph = g 
		self.stop = stop
		self.start = start
		self.seen = {}
		self.loop = {stop:1}
		self.callingContext = defaultdict(list)

	def visit(self, n):
		'''def _addToLoop():
			for p in newpath:
				self.loop[p] = 1
		newpath = []
		for e in path:
			newpath.append(e)
		newpath.append(n)'''
		self.seen[n] = 1

		if n in self.graph.predecessors.keys():
			for out in self.graph.predecessors[n]:
				self.callingContext[out].append(n)
				if out not in self.seen.keys() and out!=self.stop:
					#print("trying to visit " + str(out) + " from " + str(n))

					self.visit(out)
				#if out == self.stop:
				#	_addToLoop()

	def getPath(self):
		def _add(n):
			self.loop[n] = 1
			for pNode in self.callingContext[n]:
				if pNode not in self.loop.keys() and n != self.start:
					_add(pNode)

		_add(self.stop)
		return self.loop.keys()

