package escape.test;

public class BasicTest {
    
    private EscapingObject obj;
    private EscapingObject[] objArr;
    
    public BasicTest() {
        this.objArr = new EscapingObject[10];
    }
    
    public void createEscapingObject() {
        EscapingObject eo = new EscapingObject();
        this.obj = eo;
    }
    
    public void createEscapingObjectInArray() {
        this.objArr[0] = new EscapingObject();
    }
    
    public void createEscapingObjectArray() {
        EscapingObject[] arr = new EscapingObject[5];
        this.objArr = arr;
    }
    
    // Shouldn't report anything escapes from trickyTest
    public void trickyTest() {
        BasicTest test = new BasicTest();
        test.obj = new EscapingObject();
    }
    
    public void createObjectsNoEscape() {
        EscapingObject[] arr = new EscapingObject[100];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new EscapingObject();
            arr[i].name = "" + i;
        }
    }
    
    public static void main(String[] args) {
        BasicTest test = new BasicTest();
        
        test.createEscapingObject();
        test.createEscapingObjectInArray();
        test.createEscapingObjectArray();
        
        test.createObjectsNoEscape();
        test.trickyTest();
    }
    
    private static class EscapingObject {        
        String name = "";
        
        public EscapingObject() {}
    }
}
