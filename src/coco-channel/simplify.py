from graph import graphSimplifier 
import json 
import sys

def writeGraphtoJson(gSimple, fname):
	f = open(fname, "w")
	f.write("[ ")
	i = 0
	for n,v in gSimple.nodes.items():
		if i == len(gSimple.nodes) -1:
			f.write("{ \"id\" : \"[ " + n + " ] \" , \"instruction_count\" : " + str(v.getLength()) + ", \"secret_dependent_branch\" : ")
			if v.isSecret():
				f.write("\"true\", ")
			else:
				f.write("\"false\", ")
			f.write(" \"outgoing\" : { ")
			if n in gSimple.ancestors.keys():
				for j,a in enumerate(gSimple.ancestors[n]):
					if j == len(gSimple.ancestors[n]) -1:
						f.write("\"" + a + "\" : \"1\"")
					else:
						f.write("\"" + a + "\" : \"1\", ")

			f.write("} } ]")		
		else:
			f.write("{ \"id\" : \"[ " + n + " ] \" , \"instruction_count\" : " + str(v.getLength()) + " , \"secret_dependent_branch\" : ")
			if v.isSecret():
				f.write("\"true\", ")
			else:
				f.write("\"false\", ")
			f.write(" \"outgoing\" : { ")
			if n in gSimple.ancestors.keys():
				for j, a in enumerate(gSimple.ancestors[n]):
					if j == len(gSimple.ancestors[n]) -1:
						f.write("\"" + a + "\" : \"1\"")
					else:
						f.write("\"" + a + "\" : \"1\", ")			
			f.write('} }, \n')
		i+=1
	f.close()

if __name__ == "__main__":
	if (len(sys.argv) != 3):
		print("Input error: please include a source JSON file and a destination JSON file name")
		sys.exit()
	inname = sys.argv[1]
	outname = sys.argv[2] 
	g = graphSimplifier.graphSimplifier()
	g.makeGraphFromJson(inname)
	gSimple = g.simplify()
	writeGraphtoJson(gSimple, outname)
