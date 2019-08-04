from graph import graph 
import time
import sys

if __name__ == "__main__":

	if (len(sys.argv) < 2):
		print("Input error: please include a source JSON file")
		sys.exit()
	if (len(sys.argv) > 3):
		print("Input error: please include only a source JSON file and an optional solving mode. Enter all known conditions through the main.py source file.")
	b = time.time()
	g = graph.graph()
	known_conditions = []
	infile = sys.argv[1]
	g.makeGraphFromJson(infile)
	g.findComponents()
	if (len(sys.argv) == 2):
		g.costAnalysis(known_conditions=known_conditions)
	else:
		g.costAnalysis(known_conditions=known_conditions, mode=sys.argv[2])

