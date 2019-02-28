package drivergen;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import drivergen.Options.Var;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * Class for generating drivers
 * 
 * @author rodykers
 */
public class DriverGenerator {

	private static final String driverTemplate = "resources/DriverTemplate.txt";
	private static final String newLine = System.getProperty("line.separator");
	private static final String tab = "   ";
	
	Options options;
	
    public DriverGenerator(Options options) {
        this.options = options;
    }

    public String generate() throws GenerationException {
    	options.validate();
    	try {
    		Template t = new Template(driverTemplate);
    		t.replaceAll(getTagValuePairs());
    		return t.getContent();
    	} catch (IOException e) {
    		e.printStackTrace();
    		throw new GenerationException("Error loading template file: " + driverTemplate + newLine);
    	}
    }
    
    private Map<String, String> getTagValuePairs() throws GenerationException {
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
        
        String tstClassRawName = Util.toRawClassName(options.testClass);
        String tstClassDotName = Util.toDotName(options.testClass);
        String tstMethName = Util.toRawMethodName(options.testMethod);
        
        // imports
        String imports = "";
        if (tstClassDotName.contains("."))
        	imports += "import " + tstClassDotName + ";" + newLine;
        tagValuePairs.put("sut_imports", imports);
        
        // constants
        String consts = "";
        if (this.hasDataType("String")) {
        	consts += tab + "final int STRLEN = 2;" + newLine;
        }
        tagValuePairs.put("consts", consts);
        
        // initializers
        String init = "";
        boolean testMethodStatic = true;
        for (int i = 0; i < options.parameters.size(); i++) {
        	init += initialize(i);
        	if (options.parameters.get(i).cls.equals(options.testClass))
            	testMethodStatic = false;
        }
//        if (!testclassinitialized) { // only need the test method init if it is not the same as any of the puts
////                String tstTypList = options.testMethodTypeList;
////                init += tab + "final " + tstClassRawName + "<" + tstTypList + "> my" + Util.removePackageFromClassName(tstClassRawName) +
////                                  " = new " + tstClassRawName + "<" + tstTypList + ">();" + newLine;
//                init += tab + "finalTEST " + tstClassRawName + " my" + Util.removePackageFromClassName(tstClassRawName) +
//                        " = new " + tstClassRawName + "();" + newLine;
//        }
        tagValuePairs.put("init", init);
        
        // test method call
        String tstArgList = Util.buildArgValList(options.testParamCount, testMethodStatic) + ";";
        if (testMethodStatic) {
        	tagValuePairs.put("call_testmethod", tab + tstClassRawName + "." + tstMethName + tstArgList + newLine);
        } else {
        	tagValuePairs.put("call_testmethod", tab + "var0" + "." + tstMethName + tstArgList + newLine);
        }
        
        // notes
        String note = "";
        for (String s : tagValuePairs.values()) {
        	if (s.contains("INVALID")) {
        		note = "// TODO: \"INVALID\" Indicates an invalid data type that must be replaced with the proper type!" + newLine;
                note += "//       If the desired value is a primitive type, simply replace INVALID with that type (Integer, String, etc)." + newLine;
                break;
        	}
        }
        tagValuePairs.put("notes", note);
        
    	return tagValuePairs;
    }
    
    /**
     * this browses all the selected argument types in search of the specified
     * data type.
     * 
     * @param value - the String to search for (empty if search for any valid type)
     * @return
     */
    private boolean hasDataType(String value) {
    	if (options.testMethodTypeList.contains(value))
    		return true;
    	for (Options.Parameter m : options.parameters) {
//    		if (m.typeList.contains(value))
//    			return true;
    		if (m.elementType==Options.Type.String)
    			return true;
    	}
    	return false;
    }

    private String initPrimitive (Options.Parameter p, String name, String primtype, String boxtype, String cvalue) {
        if (null!=p.values) {
            switch (p.values) {
                case Concrete:
                    return tab + primtype + " " +  name + " = " + cvalue + ";";
                case Symbolic:
                    return tab + primtype + " " + name + " = Debug.makeSymbolic" + boxtype + "(\"" + name + "\");" + newLine;
                case N:
                    if ("int".equals(primtype) || "long".equals(primtype))
                        return tab + primtype + " " + name + " = N;";
                    else
                        return tab + primtype + " " + name + " = (" + primtype + ") N;";
                default:
                    break;
            }
        }
        return "UNEXPECTED";
    }
    
    private String initialize(int parameter) {
    	Options.Parameter p = options.parameters.get(parameter);
    	String name = "var" + parameter;
    	switch (p.type) {

            case Int:    return initPrimitive (p, name, "int"    , "Integer", "0");
            case Bool:   return initPrimitive (p, name, "boolean", "Boolean", "0");
            case Byte:   return initPrimitive (p, name, "byte"   , "Byte"   , "0");
            case Short:  return initPrimitive (p, name, "short"  , "Short"  , "0");
            case Long:   return initPrimitive (p, name, "long"   , "Long"   , "0");
            case Double: return initPrimitive (p, name, "double" , "Real"   , "0.0");
            case Char:   return initPrimitive (p, name, "char"   , "Char"   , "'a'");

            case String:
                if (p.values==Var.Concrete)
                    return tab + "String " + name + " = \"test\";";
                else if (p.values==Var.Symbolic)
                    return tab + "String " + name + " = Debug.makeSymbolicString(\"" + name + "\", STRLEN);" + newLine;
                else if (p.values==Var.N)
                    return tab + "String " + name + " = Debug.makeSymbolicString(\"" + name + "\", N);" + newLine;
                break;

            case ItObject:
                String init = "";
                String putClassRawName = Util.toDotName(p.cls);
                init += tab + "final " + putClassRawName + Util.makeGenType(p.typeList) + " " + name +
                              " = new " + putClassRawName + Util.makeGenType(p.typeList) +"();" + newLine;
                // loop header depends on Options.Var
                if (p.size==Options.Var.N) {
                    init += tab + "for (int i = 0; i < N; i++) {" + newLine;
                } else if (p.size==Options.Var.Symbolic) {
                    init += tab + "for (int i = 0; i < Debug.makeSymbolicInteger(\"size"+parameter+"\"); i++) {" + newLine;
                } else if (p.size==Options.Var.Concrete) {
                    init += tab + "for (int i = 0; i < 2; i++) {" + newLine; // TODO let user decide value
                }
                // fill with values of type p.values
                // TODO instead of using the length of argsBoxed, have a #params field
                String argsBoxed[] = Util.getWrapperNames(p.typeList);
                String params = "";
                for (int n = 0; n < argsBoxed.length; n++) {
//                    String argBoxed = argsBoxed[n];
                    String argBoxed = p.elementType.toString();
                    String param = "data" + n;
                    if (p.values==Var.Symbolic) {
                        if (argBoxed.equals("String"))
                            init += tab + tab + argBoxed + " " + param + " = Debug.makeSymbolicString(\"" + name + ":" + n + ":\"+i, STRLEN);"+ newLine;
                        else
                            init += tab + tab + argBoxed + " " + param + " = Debug.makeSymbolic" + argBoxed + "(\"" + name + ":" + n + ":\"+i);"+ newLine;
                    } else if (p.values==Var.N) {
                        if (argBoxed.equals("String"))
                            init += tab + tab + argBoxed + " " + param + " = Debug.makeSymbolicString(\"" + name + ":" + n + ":\"+i, N);"+ newLine;
                        else
                            init += tab + tab + argBoxed + " " + param + " = N"+ newLine;
                    } else if (p.values==Var.Concrete) {
                        if (argBoxed.equals("String"))
                            init += tab + tab + argBoxed + " " + param + " = \"test\""+ newLine;
                        else
                            init += tab + tab + argBoxed + " " + param + " = 0;"+ newLine; // TODO let user decide value
                    }
                    if (n>0)
                        params += ", ";
                    params += param;
                }
                init += tab + tab + name + "." + p.method + "("+ params + ");" + newLine;
                init += tab + "}" + newLine;
                return init;
                
            case OtherObject:
                // TODO check if default constructor exists, otherwise use another simple one
                String otherClassRawName = Util.fixSubClassName(Util.toRawClassName(p.cls));
                return tab + "final " + otherClassRawName + Util.makeGenType(p.typeList) + " " + name +
                             " = new " + otherClassRawName + Util.makeGenType(p.typeList) + "();" + newLine;
                
            case Array:
                String type = p.elementType.toString();
                String initar = tab + "int arsize" + parameter + " = ";
                if (p.size==Options.Var.N) {
                    initar += "N";
                } else if (p.size==Options.Var.Symbolic) {
                    initar += "Debug.makeSymbolicInteger(\"arsize"+parameter+"\")";
                } else if (p.size==Options.Var.Concrete) {
                    initar += "ARRAYSIZE";
                }
                initar += ";" + newLine;
                initar += tab + type + "[]" + name + " = new " + type + "[arsize" + parameter + "];" + newLine;
                initar += tab + "for (int i = 0; i < arsize" + parameter + "; i++) {" + newLine;
                String argBoxed = Util.getWrapperName(type);
                if (p.values==Var.Symbolic) {
                    if (argBoxed.equals("String"))
                        initar += tab + tab + type + " data" + " = Debug.makeSymbolicString(\"ardata:\"+i, STRLEN);"+ newLine;
                    else
                        initar += tab + tab + type + " data" + " = Debug.makeSymbolic" + argBoxed + "(\"ardata:\"+i);"+ newLine;
                } else if (p.values==Var.N) {
                    if (argBoxed.equals("String"))
                        initar += tab + tab + type + " data" + " = Debug.makeSymbolicString(\"ardata:\"+i, N);"+ newLine;
                    else
                        initar += tab + tab + type + " data" + " = N"+ newLine;
                } else if (p.values==Var.Concrete) {
                    initar += tab + tab + type + " data" + " = 0;"+ newLine; // TODO let user decide value
                }
                initar += tab + tab + name + "[i] = data;" + newLine;
                initar += tab + "}" + newLine;
                return initar;

            default:
                break;
        }
        return "UNEXPECTED";
    }

}
