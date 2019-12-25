public class Main {
  public void test(int x, int y) {

    int v = method_a(x, y);

    if (v > 0) {

      System.out.println("branch taken");

      int tmp = method_b(x); // orig method_b(x)

      if (tmp == x) // added

        System.out.println("inner branch taken"); // added

    } else
      assert false;
  }

  public int method_a(int x, int y) {

    if (x > 10)

      return x;

    if (y > 10)

      return y;

    return 0;
  }
  public int method_b(int z) {

    if (z > 10)
      return z++;
    else
      return z--;
  }

  public static void main(String[] args) {

    Main ex = new Main();

    ex.test(5, 6);
    // test(0,0);
  }
}
