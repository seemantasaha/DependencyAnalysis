public class Main {
  private static int f(int n) {
    if (n > 100)
      return n - 10;
    else
      return f(f(n + 11));
  }

  public static void test(int x) {
    int y = f(x);
    //assert(x > 101 || y < 90); // does not hold
    assert false;
  }

  public static void main(String[] args) {
    test(30000);
  }
}
