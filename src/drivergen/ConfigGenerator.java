package drivergen;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import drivergen.Options.ConfigType;
import drivergen.Options.Parameter;
import drivergen.Options.Type;

/**
 *
 * Class for generating JPF config files
 * 
 * @author rodykers
 */
public class ConfigGenerator {

	private static final String configTemplateWCA = "resources/WCAConfigTemplate.txt";
	private static final String configTemplateSC = "resources/SCConfigTemplate.txt";
	
	Options options;
	
    public ConfigGenerator(Options options) {
        this.options = options;
    }

    public String generate() throws GenerationException {
    	String templateFile = options.configType == ConfigType.WCA ? configTemplateWCA : configTemplateSC;
    	try {
    		Template t = new Template(templateFile);
    		t.replaceAll(getTagValuePairs());
        	return t.getContent();
    	} catch (IOException e) {
    		throw new GenerationException("Error loading template file: " + templateFile);
    	}
    }
    
    private Map<String, String> getTagValuePairs() {
    	Map<String,String> tagValuePairs = new HashMap<String,String>();

        // file type
        String ftype = (options.configType == Options.ConfigType.WCA) ? "Worst Case Analyzer" : "Side Channel";
        tagValuePairs.put("driver_type", ftype);

    	// date
    	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tagValuePairs.put("date", dateFormat.format(new Date()));
        
        // version
        tagValuePairs.put("version", "1.0.0");
        
        // driver name
        String drivername = Util.getDriverFileName(options.projectName);
        drivername = drivername.substring(0, drivername.lastIndexOf('.'));
        tagValuePairs.put("driver_name", drivername);
    	
        // basic options
        tagValuePairs.put("base_dir", options.baseDir);
        tagValuePairs.put("src_dir", options.srcDir);
        tagValuePairs.put("out_dir", options.outDir);
        tagValuePairs.put("solver", options.solver);
        tagValuePairs.put("bvlen", String.valueOf(options.bvlen));
        tagValuePairs.put("debug", String.valueOf(options.debug));
        tagValuePairs.put("no_solver_heuristic", String.valueOf(options.heuristicNoSolver));
        tagValuePairs.put("history_size", String.valueOf(options.historySize));
        tagValuePairs.put("policy_at", String.valueOf(options.policyAt));
        tagValuePairs.put("input_max", String.valueOf(options.inputMax));
        tagValuePairs.put("costmodel", options.costModel);
        tagValuePairs.put("jarsclasspath", options.jarsclasspath);
        
        // side channel
        if (options.sideChanType != null)
        	tagValuePairs.put("sctype", options.sideChanType.toString());
        
        // min max values
        tagValuePairs.put("min_char", options.minValChar);
        tagValuePairs.put("max_char", options.maxValChar);
        tagValuePairs.put("min_byte", options.minValByte);
        tagValuePairs.put("max_byte", options.maxValByte);
        tagValuePairs.put("min_short", options.minValShort);
        tagValuePairs.put("max_short", options.maxValShort);
        tagValuePairs.put("min_int", options.minValInt);
        tagValuePairs.put("max_int", options.maxValInt);
        tagValuePairs.put("min_long", options.minValLong);
        tagValuePairs.put("max_long", options.maxValLong);
        tagValuePairs.put("min_double", options.minValDouble);
        tagValuePairs.put("max_double", options.maxValDouble);
        
        // test method
        String tstConList = Util.buildArgConList(options.testParamCount);
        String testMethodSPF = Util.toDotName(options.testClass) + "." + Util.toRawMethodName(options.testMethod) + tstConList;
        tagValuePairs.put("testmethod_spf", testMethodSPF);
        
        String symbMethods = testMethodSPF;
        for (Parameter p : options.parameters) {
        	if (p.type==Type.ItObject) {
        		// We probably only want methods in the analysis class here
        		if (p.cls.equals(options.testClass)) {
        			// TODO calling getWrapperNames is not a nice way to do this...
        			String meth = Util.toDotName(p.cls) + "." + p.method
        					+ Util.buildArgConList(Util.getWrapperNames(p.typeList).length);
        			if (!symbMethods.contains(meth)) {
        				symbMethods += "," + meth;
        			}
        		}
        	}
        }
        tagValuePairs.put("symb_methods", symbMethods);
        
    	return tagValuePairs;
    }
}
