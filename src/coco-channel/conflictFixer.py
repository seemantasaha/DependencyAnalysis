from graph import graphSimplifier 
import json 
import sys
from simplify import writeGraphtoJson


if __name__ == "__main__":
	if (len(sys.argv) != 3):
		print("Input error: please include a source JSON file and a destination JSON file name")
		sys.exit()
	inname = sys.argv[1]
	outname = sys.argv[2] 

	g = graphSimplifier.graphSimplifier()
	g.makeGraphFromJson(inname)
	g.checkAndFixConflicts()
	writeGraphtoJson(g, outname)
	print("LOG: new JSON file created")
#Now rewrite g to file
