/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author dan
 */
public class SymTypes {

    private final SymLimits valChar;
    private final SymLimits valByte;
    private final SymLimits valShort;
    private final SymLimits valInt;
    private final SymLimits valLong;
    private final SymLimits valDouble;
        
    public SymTypes() {
        // initializes each data type entry to their corresponding min and max allowable limit values.
        valChar   = new SymLimits("0"                                     , "255");
        valByte   = new SymLimits(Byte.toString(Byte.MIN_VALUE)           , Byte.toString(Byte.MAX_VALUE));
        valShort  = new SymLimits(Short.toString(Short.MIN_VALUE)         , Short.toString(Short.MAX_VALUE));
        valInt    = new SymLimits(Integer.toString(Integer.MIN_VALUE)     , Integer.toString(Integer.MAX_VALUE));
        valLong   = new SymLimits(Long.toString(Long.MIN_VALUE)           , Long.toString(Long.MAX_VALUE));
        valDouble = new SymLimits(Double.toString(-1.0 * Double.MAX_VALUE), Double.toString(Double.MAX_VALUE));
    }
        
    /**
     * copys the data values from the passed parameter
     * 
     * @param copy - the symbol limits class to copy from
     */
    public void copyLimits(SymTypes copy) {
        valChar  .setLimits(copy.getLimits("char"));
        valByte  .setLimits(copy.getLimits("byte"));
        valShort .setLimits(copy.getLimits("short"));
        valInt   .setLimits(copy.getLimits("int"));
        valLong  .setLimits(copy.getLimits("long"));
        valDouble.setLimits(copy.getLimits("double"));
    }
        
    /**
     * sets the min and max limit setting for the specified data types
     * 
     * @param dtype - the desired data type
     * @param min - the min value to set for the specified data type
     * @param max - the max value to set for the specified data type
     */
    public void setLimits (String dtype, String min, String max) {
        switch (dtype) {
            default: // fall through...
            case "char":   valChar.setLimits(min, max);   break;
            case "byte":   valByte.setLimits(min, max);   break;
            case "short":  valShort.setLimits(min, max);  break;
            case "int":    valInt.setLimits(min, max);    break;
            case "long":   valLong.setLimits(min, max);   break;
            case "double": valDouble.setLimits(min, max); break;
        }
    }
        
    /**
     * returns the min and max limit setting for the specified data types
     * 
     * @param dtype - the desired data type
     * @return struct containing the min value and max value for that data type
     */
    public SymLimits getLimits (String dtype) {
        switch (dtype) {
            default: // fall through...
            case "char":   return valChar;
            case "byte":   return valByte;
            case "short":  return valShort;
            case "int":    return valInt;
            case "long":   return valLong;
            case "double": return valDouble;
        }
    }
}
