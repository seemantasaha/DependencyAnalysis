/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import drivergen.Util;

/**
 *
 * @author dmcd2356
 */
class FontEntry {
    private int   index;        // index of start of meta chars
    private Util.FontType type; // font mode for this section of text
    
    FontEntry (int offset, Util.FontType newtype) {
        index = offset;
        type = newtype;
    }
        
    public Util.FontType changeBold (int offset) {
        // set the new type value based on the current value and what we are changing
        switch(type) {
            default:
            case Normal:     type = Util.FontType.Bold;         break;
            case Bold:       type = Util.FontType.Normal;       break;
            case Italic:     type = Util.FontType.BoldItalic;   break;
            case BoldItalic: type = Util.FontType.Italic;       break;
        }
        index = offset;
        return type;
    }

    public Util.FontType changeItalic (int offset) {
        // set the new type value based on the current value and what we are changing
        switch(type) {
            default:
            case Normal:     type = Util.FontType.Italic;       break;
            case Bold:       type = Util.FontType.BoldItalic;   break;
            case Italic:     type = Util.FontType.Normal;       break;
            case BoldItalic: type = Util.FontType.Bold;         break;
        }
        index = offset;
        return type;
    }

    public Util.FontType getFontType(){
        return type;
    }

    public int getIndex(){
        return index;
    }
}
