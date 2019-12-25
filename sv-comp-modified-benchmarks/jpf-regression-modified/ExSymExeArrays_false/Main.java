public class Main {
  static int[] a;
  public static void main(String[] args) {
    a = new int[1];
    test(-3);
  }

  public static void test(int x) {
    a[0] = x;
    if (a[0] >= 0)
      System.out.println("branch1 >=0");
    else {
      assert false;
      System.out.println("branch2 <0");
    }
    //	  if(x>=0)
    //		  System.out.println("branch3 >=0");
    //	  else
    //		  System.out.println("branch4 <0");
  }
}
