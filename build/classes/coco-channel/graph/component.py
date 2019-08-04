from enum import Enum
from collections import defaultdict

class componentType(Enum):
	BRANCH = 0 
	LOOP = 1

class component:

	def __init__(self, cType, header, tail):
		self.type = cType
		self.header = header
		self.tail = tail
		self.identity = None

		self.body = set()
		self.bodyVisitable = set()
		self.childrenNodesToChild = {}

		self.children = set()
		self.parent = None
		self.cost = None
		self.exits = defaultdict(list)
		self.exit_paths = []
		self.loopBreaks = defaultdict(list)

		self.secretBranchesForLoop = set()

		self.loopBreak = False
		self.childrenFound = False
		self.costComputed = False

		self.secret = False
		self.min_value = None
		self.max_value = None
		self.min_solns = None
		self.max_solns = None

		self.max_diff_solns = {}

		self.sideChannel = None
		self.notHandledCase = False; 

	
	def getType(self):
		return self.type 

	def setIdentity(self, idnum):
		self.identity = idnum

	def getIdentity(self):
		return self.identity

	def getHeader(self):
		return self.header

	def getTail(self):
		return self.tail

	def updateBody(self, bodyNodes):
		for n in bodyNodes:
			self.body.add(n)

	def getBody(self):
		return self.body

	def initializeVisitableBody(self):
		for c in self.body:
			self.bodyVisitable.add(c)

	def removeFromVisitableBody(self, c):
		self.bodyVisitable.discard(c)

	def getVisitableBody(self):
		return self.bodyVisitable

	def nodeToChild(self, n):
		return self.childrenNodesToChild[n]

	def setNodeToChild(self, n, c):
		self.childrenNodesToChild[n] = c 

	def isCostComputed(self):
		return self.costComputed

	def setCost(self, cost):
		self.cost = cost
		self.costComputed = True

	def getCost(self):
		return self.cost

	def updateChildren(self, child):
		self.children.add(child)

	def removeChildren(self, child):
		self.children.remove(child)

	def getChildren(self):
		return self.children

	def setParent(self, parentComponent):
		self.parent = parentComponent

	def getParent(self):
		return self.parent

	def setSecretBranchesInLoopComponent(self, setOfBranches):
		self.secretBranchesForLoop = setOfBranches

	def getSecretBranchesInLoopComponent(self):
		return self.secretBranchesForLoop

	def appendSecretBranchesInLoopComponent(self, branch):
		self.secretBranchesForLoop.add(branch)

	def markChildrenDone(self):
		self.childrenFound = True

	def finishedChildren(self):
		return self.childrenFound

	def markLoopBreak(self):
		self.loopBreak = True

	def isLoopBreak(self):
		return self.loopBreak

	def loopBreakOf(self):
		return self.loops

	def setLoopBreakOf(self, c):
		self.loops = c

	def markExit(self, exit, exit_to):
		self.exits[exit].append(exit_to)

	def removeExit(self, exit):
		del self.exits[exit]

	def getExits(self):
		return self.exits 

	def markExitPath(self, exit_path):
		self.exit_paths.append(exit_path)

	def getExitPaths(self):
		return self.exit_paths;

	def isSecret(self):
		return self.secret

	def setSecret(self):
		self.secret = True

	def setSideChannel(self):
		self.sideChannel = True

	def isSideChannel(self):
		return self.sideChannel

	def setMinValue(self, min_value):
		self.min_value = min_value

	def setMaxValue(self, max_value):
		self.max_value = max_value

	def getMaxValue(self):
		return self.max_value

	def getMinValue(self):
		return self.min_value

	def setMinSolns(self, min_solns):
		self.min_solns = min_solns

	def setMaxSolns(self, max_solns):
		self.max_solns = max_solns

	def getMinSolns(self):
		return self.min_solns

	def getMaxSolns(self):
		return self.max_solns

	def getMaxDiffSolns(self):
		return self.max_diff_solns

	def addMaxDiffSolns(self, max_diff_solns):
		self.max_diff_solns = max_diff_solns

	def setNotHandled(self):
		self.notHandledCase=True

	def isNotHandled(self):
		return self.notHandledCase
		
	def __str__(self):
		res = "[ " + str(self.header) + ", " + str(self.tail) + " :"
		for n in self.body:
			res+= " " + str(n) 
		res += "]"
		return res
