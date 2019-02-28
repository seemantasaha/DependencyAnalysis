package drivergen;

import gui.RedirectOutputStream;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;


/**
 * @author Rody Kersten
 * @author dmcdermet
 *
 */
public class Util {
    /**
     * Returns the name of the specified driver file to generate.
     * Does not include path or file extension.
     * 
     * @param projectName - name of the project
     * @return the basename (no path) for the specified file
     */
    public static String getDriverBaseName(String projectName) {
        if (projectName == null)
            projectName = "";
        else
            projectName = "_" + projectName;
        return "Driver" + projectName;
    }
    
    /**
     * Returns the name of the specified driver file to generate.
     * Does not include path, but does include file extension.
     * 
     * @param projectName - name of the project
     * @return the name (no path) for the specified file
     */
    public static String getDriverFileName(String projectName) {
        return getDriverBaseName(projectName) + ".java";
    }

    /**
     * Returns the name of the specified config file to generate.
     * Does not include path or file extension or the index value.
     * 
     * @param projectName - name of the project
     * @return the basename (no path) for the specified file
     */
    public static String getConfigBaseName(String projectName) {
        if (projectName == null)
            projectName = "";
        else
            projectName = "_" + projectName;
        return "Config" + projectName + "_";
    }
    
    /**
     * Returns the name of the specified configuration file to generate
     * 
     * @param projectName - name of the project
     * @param id - the id value for the config file
     *             0    if there is only 1 config file
     *             1..N if there are multiple config files ranging from 1 to N
     * @return the name (no path) for the specified file
     */
    public static String getConfigFileName(String projectName, Integer id) {
        return getConfigBaseName(projectName) + id + ".jpf";
    }
    
    public static String toDotName(String cls) {
        // remove the leading L char, if present
        if (cls.startsWith("L"))
            cls = cls.substring(1);
        // replace all '/' with dot
        if (cls.contains("/"))
            return cls.replace("/", ".");
        return cls;
    }
    
    public static String toRawClassName(String cls) {
        // remove the leading L char, if present
        if (cls.startsWith("L"))
            cls = cls.substring(1);
        // return the last field delimited by '/'
        if (cls.contains("/"))
            cls = cls.substring(cls.lastIndexOf('/') + 1);
        return cls;
    }

    /**
     * this fixes class names that are subclasses within a class.
     * These are marked as CLASS$SUBCLASS which doesn't compile.
     * The '$' char is replaced by a '.' to fix this.
     * 
     * @param cls - the class name
     * @return the repaired class name
     */
    public static String fixSubClassName(String cls) {
        if (cls.contains("$"))
            cls = cls.replace("$", ".");
        return cls;
    }
    
    public static String removePackageFromClassName(String cls) {
        if (cls.contains("."))
            return cls.substring(cls.lastIndexOf('.') + 1);
        return cls;
    }
    
    public static String toRawMethodName(String m) {
        if (m.contains("("))
            return m.substring(0, m.indexOf("("));
        return m;
    }

    /**
     * This returns the boxed primitive string representation for the primitive
     * character types as defined by the Java Language Specification (13.1).
     * If the type is a class, it will remove the initial L
     * @param type
     * @return 
     */
    public static String getJavaPrimitiveType (String type) {
        switch(type) {
            case "C":       return "Char";
            case "B":       return "Byte";
            case "S":       return "Short";
            case "I":       return "Integer";
            case "J":       return "Long";
            case "F":       return "Float";
            case "D":       return "Double";
            case "Z":       return "Bool";
            default:
                break;
        }
        return type;
    }

    /**
     * This returns the boxed primitive string representation for the primitive
     * character types as defined by the Java Language Specification (13.1).
     * If the type is a class, it will remove the initial L
     * @param type
     * @return 
     */
    public static String getJavaReferenceType (String type) {
        // if it begins with an 'L', it is a class name.
        // Remove the initial 'L' designation and change name to dot format
        if (type.startsWith("L"))
            type = type.replace('/', '.').substring(1);

        switch(type) {
            case "java.lang.Char":    return "Char";
            case "java.lang.Byte":    return "Byte";
            case "java.lang.Short":   return "Short";
            case "java.lang.Integer": return "Integer";
            case "java.lang.Long":    return "Long";
            case "java.lang.Float":   return "Float";
            case "java.lang.Double":  return "Double";
            case "java.lang.Bool":    return "Bool";
            case "java.lang.String":  return "String";
            case "java.lang.Object":  return "Object";
            default:
                break;
        }
        return type;
    }

    /**
     * This returns the corresponding SPF-valid type for the data type.
     * If it is not a convertible type it returns "INVALID".
     *
     * @param type = the data type of a parameter in one of the methods
     * @return corresponding SPF-valid data type
     */
    public static String getWrapperName(String type) {
        String typeName;
        switch (type) {
            case "Char":   typeName = "String";  break; // use string
            case "Byte":   typeName = "Byte";    break;
            case "Short":  typeName = "Short";   break;
            case "Integer":typeName = "Integer"; break;
            case "Long":   typeName = "Long";    break;
            case "Float":  typeName = "Double";  break; // use double
            case "Double": typeName = "Double";  break;
            case "Bool":   typeName = "Integer"; break; // use integer
            case "String": typeName = "String";  break;
            default:       typeName = "INVALID"; break;
        }
        return typeName;
    }
    
    // Assumes types in typeList are separated by commas
    public static String[] getWrapperNames(String typeList) {
    	String ar[] = typeList.split(",");
    	String ret[] = new String[ar.length];
    	for (int i = 0; i < ar.length; i++) {
    		ret[i] = getWrapperName(ar[i].trim());
    	}
    	return ret;
    }
    
    /**
     * converts the Primitive type returned by getJavaPrimitiveType or getJavaReferenceType
     * to the Primitive comboBox selection that best fits it.
     * 
     * @param dtype - Primitive+ values (stripped of array brackets, etc.)
     * @return string value to set comboBox to or "" if none
     */
    public static String cvtTypeRefToPrimitiveCbox (String dtype) {
        switch (dtype) {
            case "Char":    return "char";
            case "Byte":    return "byte";
            case "Short":   return "short";
            case "Integer": return "int";
            case "Long":    return "long";
            case "Float":   return "double";    // select double
            case "Double":  return "double";
            case "Bool":    return "int";       // select int
            default:
                break;
        }
        return "";
    }

    /**
     * converts the Primitive type returned by getJavaPrimitiveType or getJavaReferenceType
     * to the Element comboBox selection that best fits it.
     * 
     * @param dtype - Primitive+ values (stripped of array brackets, etc.)
     * @return string value to set comboBox to or "" if none
     */
    public static String cvtTypeRefToElementCbox (String dtype) {
        switch (dtype) {
            case "Char":    return "char";
            case "Byte":    return "byte";
            case "Short":   return "short";
            case "Integer": return "int";
            case "Long":    return "long";
            case "Float":   return "double";    // select double
            case "Double":  return "double";
            case "Bool":    return "bool";
            case "String":  return "String";
            case "Object":  return "String";    // select String
            default:
                break;
        }
        return "";
    }

    /**
     * generates an argument string for the SPF method meta-field.
     * this consists of a '#' delineated list of values "con" (one for each
     * argument) and enclosed in "()".
     *
     * @param count = the number of arguments
     * @return the argument list.
     */
    public static String buildArgConList (int count) {
        String argList = "(";
        boolean first = true;
        for (int ix = 1; ix <= count; ix++) {
            if (!first)
                argList += "#";
            argList += "con";
            first = false;
        }
        argList += ")";
        return argList;
    }
    
    /**
     * generates an argument string for the Driver.
     * this consists of a comma delineated list of values "argN" (where N is the
     * argument index number starting at 1) and enclosed in "()".
     *
     * @param count = the number of arguments
     * @return the argument list.
     */
    public static String buildArgValList (int count, boolean isStatic) {
        String argList = "(";
        boolean first = true;
        for (int ix = 0; ix < count; ix++) {
            if (!first)
                argList += ", ";
            argList += "var" + (isStatic ? ix : ix+1);
            first = false;
        }
        argList += ")";
        return argList;
    }
    
    /**
     * Adds '<' and '>' around generic types if needed.
     * @param typeList
     * @return
     */
    public static String makeGenType(String typeList) {
    	// for now...
    	return "";
    	
    	// when we can actually guess them we can use the code below
//    	if (typeList.isEmpty())
//    		return "";
//    	else return "<" + typeList + ">";
    }

    public enum FontType {
        Normal, Bold, Italic, BoldItalic;
    }
    
    public enum TextColor {
        Black, DkGrey, DkRed, Red, LtRed, Orange, Brown, Gold, Green, Cyan, LtBlue, Blue, Violet, DkVio;
    }
    
    /**
     * generates the specified text color for the debug display.
     * 
     * @param colorName - name of the color to generate
     * @return corresponding Color value representation
     */
    public static Color generateColor (TextColor colorName) {
        float hue, sat, bright;
        switch (colorName) {
            default:
            case Black:  return Color.BLACK;
            case DkGrey: return Color.DARK_GRAY;
            case DkRed:  hue = (float)0;   sat = (float)100; bright = (float)66;  break;
            case Red:    hue = (float)0;   sat = (float)100; bright = (float)90;  break;
            case LtRed:  hue = (float)0;   sat = (float)60;  bright = (float)100; break;
            case Orange: hue = (float)20;  sat = (float)100; bright = (float)100; break;
            case Brown:  hue = (float)20;  sat = (float)80;  bright = (float)66;  break;
            case Gold:   hue = (float)40;  sat = (float)100; bright = (float)90;  break;
            case Green:  hue = (float)128; sat = (float)100; bright = (float)45;  break;
            case Cyan:   hue = (float)190; sat = (float)80;  bright = (float)45;  break;
            case LtBlue: hue = (float)210; sat = (float)100; bright = (float)90;  break;
            case Blue:   hue = (float)240; sat = (float)100; bright = (float)100; break;
            case Violet: hue = (float)267; sat = (float)100; bright = (float)100; break;
            case DkVio:  hue = (float)267; sat = (float)100; bright = (float)66;  break;
        }
        hue /= (float)360.0;
        sat /= (float)100.0;
        bright /= (float) 100.0;
        return Color.getHSBColor(hue, sat, bright);
    }

    /**
     * A generic function for appending formatted text to a JTextPane.
     * 
     * @param color - color of text
     * @param font  - the font selection
     * @param size  - the font point size
     * @param ftype - type of font style
     * @return the attribute set
     */
    public static AttributeSet setTextAttr(TextColor color, String font, int size, FontType ftype)
    {
        boolean bItalic = false;
        boolean bBold = false;
        if (ftype == FontType.Italic || ftype == FontType.BoldItalic)
            bItalic = true;
        if (ftype == FontType.Bold || ftype == FontType.BoldItalic)
            bBold = true;

        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, generateColor(color));

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, font);
        aset = sc.addAttribute(aset, StyleConstants.FontSize, size);
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
        aset = sc.addAttribute(aset, StyleConstants.Italic, bItalic);
        aset = sc.addAttribute(aset, StyleConstants.Bold, bBold);
        return aset;
    }

    /**
     * Formats and runs the specified command.
     * 
     * @param command - an array of the command and each argument it requires
     * @param workingdir - the directory to execute the command from
     * @param outTextArea - the JTextArea to redirect stdout and stderr to
     * 
     * @return the status of the command (0 = success) 
     * 
     * @throws java.lang.InterruptedException 
     * @throws java.io.IOException 
     */
    public static int runCommand(String[] command, String workingdir, JTextArea outTextArea) throws InterruptedException, IOException {
        int retcode;

        // build up the command and argument string
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingdir != null) {
            File workdir = new File(workingdir);
            if (workdir.isDirectory())
                builder.directory(workdir);
        }
        
        PrintStream standardOut = System.out;
        PrintStream standardErr = System.err;         

        // re-direct stdout and stderr to the text window
        // merge stderr into stdout so both go to the specified text area
        builder.redirectErrorStream(true);
        PrintStream printStream = new PrintStream(new RedirectOutputStream(outTextArea)); 
        System.setOut(printStream);
        System.setErr(printStream);
            
        // run the command
        System.out.println("------------------------------------------------------------");
        System.out.println("Executing command: " + String.join(" ", command));
        Process p = builder.start();

        String status;
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while (p.isAlive()) {
            while ((status = br.readLine()) != null) {
                System.out.println(status);
            }
            Thread.sleep(100);
        }

        retcode = p.exitValue();
        System.err.println("exit code = " + retcode);
        p.destroy();

        // restore the stdout and stderr
        System.setOut(standardOut);
        System.setErr(standardErr);

        return retcode;
    }
    
}

