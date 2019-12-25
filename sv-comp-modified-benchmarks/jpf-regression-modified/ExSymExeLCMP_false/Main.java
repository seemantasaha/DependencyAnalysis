public class Main {

  public static void main(String[] args) {
    test(5, 5);
  }

  public static void test(int x, int y) {

    int res = x;
    if (res + 1 > res + 2)
      System.out.println("x >0");
    else {
      assert false;
      System.out.println("x <=0");
    }
  }
}
