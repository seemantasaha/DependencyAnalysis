# Dependencies
  z3py. Please ensure that the z3/build/python directory is added to your PYTHONPATH. 
  
  Janalyzer.
  
 
# CFG Extraction 

CoCo-Channel requires an annotated control flow graph in JSON format. We provide a step-by-step overview of how to use Janalyzer to create such a JSON.

## Preprocessing Graph

We generate a graph in json format with taint analysis integrated and feed the json file to CoCo-Channel to do our imbalance analysis. We do this pre-processing in 3 following steps:

* control flow and call graph extraction
* marking the secret variable and do incremental data dependency which marks all the secret dependent branches
* selecting a specific method, extracting the graph in json format and feed to CoCo-Channel

## Taint Analysis

We do our taint analysis using Janalyzer (https://phab-isstac.isis.vanderbilt.edu/diffusion/JAN/) where we mostly use the existing dependency analysis and implemented a simpler incremental version of data dependency instead of using both data and control dependency. Our changes are applied to data-flow-analysis branch (https://phab-isstac.isis.vanderbilt.edu/diffusion/JAN/browse/data-flow-analysis/). Correctness of our taint analysis depends completely on the existing data dependency analysis implemented in Janalyzer. For our analysis, to get the experimental results on benchmarks like DARPA-STAC, Blazer and Themis, our taint analysis can mark the secret dependent branches automatically for most of the part but it happpened that for few examples it required human guided semi-automatic analysis.

## Steps to Extract CFG

You can simply follow the README file from Janalyzer repository (https://phab-isstac.isis.vanderbilt.edu/diffusion/JAN/)

## Steps to Taint Analysis

* You need to go to the specific method's control flow graph view 
* Select a variable from the right side of the view (window) as secret variable
* It will do a forward taint analysis and mark all the nodes affected by incremental data dependency
* Point to be noted here, we did not mark secret dependent branch nodes only (which will be filtered in json generation phase) but also other nodes dependent on secret

## JSON Generation

* You need to go to the specific method's control flow graph view from where you want to extract the json, remeber this can be different from the method where we selected the secret variable
* Then you need to right click on that view and select option "Show JSON" which will generate the json text to a file choosen by you
* This file containing json (representing a graph where all secret dependent branches are marked) will be feed to CoCo Channel 


# CFG Preprocessing 

There are a few steps to consider before running the CoCo-Channel's full decomposition, annotation and querying stages. 

* First, note that Janalyzer does not return a CFG in basic block format. To consolidate the indidivual nodes into basic blocks, use the simplify.py script. This script takes in two arguments: a source JSON file and the name (including the path) of where to save the new JSON file with the CFG in basic block format. 
* Not all control flow structures are supported by CoCo-Channel's analysis. To detect any discrepencies and re-shape in the CFG into a compatible shape, use the conflictFixer.py script. This script also takes in two arguments: a source JSON file and the name (including the path) of where to save the new JSON file with the re-shaped CFG. 

# Running CoCo-Channel

To run CoCo-Channel, use the main.py script. This script has one required argument -- a source JSON file containing a CFG. It also has an additional argument specifying what mode should be run. If nothing is specified, the default mode is used. If "o" is specified, then the optimization queries are formulated and solved. If anything else is specified, then compositional solving is enabled. 
Note that currently CoCo-Channel is still under development. There are certain structures which are not supported. Warning messages will appear when a structure not fully tested or accounted for has been encounted. Be aware that the output from CoCo-Channel may not be reliable in these cases. 

## Paramater Choices

Currently, CoCo-Channel has two parameters of interest. A loop bound parameter and an observation threshold. These parameters are controlled in the "solver" classes found in expr/solver directory. You can control the parameters in the class files for the different kinds of solvers (i.e. optimization solver versus compositional solver versus basic solver). For simplicity, make sure the parameters are set identically in each of the class files to avoid confusion and worry about which solver is used when. 

## Feasibility Testing

CoCo-Channel's functionality can be extended by using SPF to check the feasibility of the returned witness paths. This is done by manually writing SPF drivers. Should this analysis determine that the paths returned by CoCo-Channel are unfeasible, additional constraints can be provided to CoCo-Channel to bar these paths in the future. This is done through adding the conditionals to the "known_conditions" list in main.py. Note that the integration with known_conditions when compositional solving is used is still under test. 

