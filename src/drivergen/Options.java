package drivergen;

import java.util.List;

/**
 * Class for storing all the options for the driver and config generator.
 * 
 * @author Rody Kersten
 *
 */
public class Options {

	// general options
	public String   projectName;
	public String   baseDir;
        public String   srcDir;
        public String   outDir;
	public String   solver;
	public int      bvlen;
	public boolean  debug;
	public boolean  heuristicNoSolver;
	public int      historySize;
	public int      policyAt;
	public int      inputMax;
	public String   costModel;
        public SideChannelType sideChanType;
	public ConfigType configType;
	
	// Methods
	public String   testClass;
	public String   testMethod;
	public String   testMethodTypeList;
        public int      testParamCount;
	public List<Parameter> parameters;
//	public List<Parameter> otherObjects;

	// min/max values
	public String   minValChar;
	public String   maxValChar;
	public String   minValByte;
	public String   maxValByte;
	public String   minValShort;
	public String   maxValShort;
	public String   minValInt;
	public String   maxValInt;
	public String   minValLong;
	public String   maxValLong;
	public String   minValDouble;
	public String   maxValDouble;

	public String   jarsclasspath;
	
	public class Parameter {
		public Type type;
		public String cls;
		public String method;
		public Var size;
		public Var values;
		public String typeList;
		public Type elementType;
	}
	
	public enum ConfigType {
		WCA, SideChannel;

                @Override
		public String toString() {
		    switch(this) {
                      default:  // fall through...
		      case WCA:         return "WCA";
		      case SideChannel: return "SideChannel";
                    }
                }
	}
	
	public enum SideChannelType {
		Time, Memory, File, Socket;
		@Override
		public String toString() {
		    switch(this) {
		      case Memory: return "Memory";
		      case File:   return "File";
		      case Socket: return "Socket";
		      default:     return "Time";
		    }
                }
	}
	
        public static SideChannelType cvtCboxToSideChannelType(String type) {
            switch(type) {
                default:  // fall through...
                case "Time":    return SideChannelType.Time;
                case "Memory":  return SideChannelType.Memory;
                case "File":    return SideChannelType.File;
                case "Socket":  return SideChannelType.Socket;
            }
        }
	
	public enum Var {
		Concrete, Symbolic, N;

                @Override
		public String toString() {
		    switch(this) {
                      default:  // fall through...
		      case Symbolic:     return "Symbolic";
		      case Concrete:     return "Concrete";
		      case N:            return "N";
                    }
                }
	}
	
        public static Var cvtCboxToVar(String type) {
            switch(type) {
                default:  // fall through...
                case "Symbolic":  return Var.Symbolic;
                case "Concrete":  return Var.Concrete;
                case "N":         return Var.N;
            }
	}
	
	public enum Type {
		Bool, Char, Int, Byte, Short, Long, Double, String, ItObject, OtherObject, Array;

                @Override
		public String toString() {
		    switch(this) {
		      case Char:     return "Char";
		      case Byte:     return "Byte";
		      case Short:    return "Short";
		      case Int:      return "Integer";
		      case Long:     return "Long";
		      case Double:   return "Double";
		      case Bool:     return "Bool";
		      case String:   return "String";
		      case ItObject: return "ItObject";
		      case Array:    return "Array";
                      default:
		      case OtherObject: return "Other";
		    }
                }
	}
	
        public static Type cvtCboxToType (String type) {
            switch(type) {
                case "char":   return Type.Char;
                case "byte":   return Type.Byte;
                case "short":  return Type.Short;
                case "int":    return Type.Int;
                case "long":   return Type.Long;
                case "double": return Type.Double;
                case "bool":   return Type.Bool;
                case "String": return Type.String;
                default:
                case "Other":  return Type.OtherObject;
            }
	}

        public void validate() throws GenerationException {
		if (testClass==null || testMethod==null)
			throw new GenerationException("Error in options. No test class/method.");
//		if (parameters.size() < 1 || (parameters.get(0).type!=Type.ItObject && parameters.get(0).type!=Type.OtherObject))
//			throw new GenerationException("Error in options. First parameter must be object on which test method is invoked.");
		// TODO check other things?
	}
}
