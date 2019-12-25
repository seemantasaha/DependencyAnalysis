public class Main {

  public static void main(String[] args) {
    test(5);
  }

  public static void test(int x) {
    System.out.println("Testing FNEG");
    int y = -x;
    if (y > 0)
      System.out.println("branch -x > 0");
    else {
      assert false;
      System.out.println("branch -x <= 0");
    }
  }
}
