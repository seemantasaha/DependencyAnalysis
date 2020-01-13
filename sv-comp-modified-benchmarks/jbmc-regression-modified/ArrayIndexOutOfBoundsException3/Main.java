public class Main {
  public static void main(String args[]) {
    test(5);
  }

  public static void test(int size) {
  	try {
      int[] a = new int[4];
      a[size] = 0;
    } catch (Exception exc) {
      assert false;
    }
  }
}
