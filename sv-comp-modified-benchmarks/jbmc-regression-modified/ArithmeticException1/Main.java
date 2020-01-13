public class Main {
  public static void main(String args[]) {
    test(5);
  }

  public static void test(int i) {
  	try {
      int j = 10 / i;
    } catch (ArithmeticException exc) {
      assert false;
    }
  }
}
