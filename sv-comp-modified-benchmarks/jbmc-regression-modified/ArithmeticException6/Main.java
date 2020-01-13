public class Main {
  public static void main(String[] args) {
    test(5);    
  }

  public static void test(int denom) {
  	try {
      int j = 10 / denom;
    } catch (ArithmeticException exc) {
      assert false;
    }
  }
}
