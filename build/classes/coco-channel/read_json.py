import json 
from graph import node 
json_data = open("input/insecure_no_sleep.json").read()
data = json.loads(json_data)

'''a graph should consist of:
	a list of nodes 
	a list of edges
	loops?
	branches?
	basically should I also include components of the graph? YES
	how should nesting info be included?
	only the topmost or all?
	 '''
nodes = {} #id to node object?
edges = []
cycles = {}

def getNode(identity):
	if identity not in nodes.keys():
		nodes[identity] = node.node(identity)
	return nodes[identity]


for d in data:
	identity = d['id'].split()[1]
	currentNode = getNode(int(identity))
	outgoing = d['outgoing'].keys()
	for out in outgoing:
		outNode = getNode(int(out))
		edges.append((currentNode, outNode))
		currentNode.updateOutgoing(outNode)
		outNode.updateIncoming(currentNode)

def addCycle(loop):
#Now let's try and find a loop 
from visitors import loopDetector
for n in nodes.key():
	l = loopDetector.loopDetector(n)
	l.visit(n, [])
	loops = l.getCycles()
	if loops:
		for loop in loops:
			addCycle()




