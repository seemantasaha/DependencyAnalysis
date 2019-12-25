public class Main {

  static int fibonacci(int n) {
    if (n < 1) {
      return 0;
    } else if (n == 1) {
      return 1;
    } else {
      return fibonacci(n - 1) + fibonacci(n - 2);
    }
  }

  public static void test(int x) {
    if (x > 46) {
      return;
    }
    int result = fibonacci(x);
    // if (x < 9 || result >= 34) {
    //   return;
    // } else {
    //   assert false;
    // }
    //asser(x < 9 || result >= 34);
    assert false;
  }

  public static void main(String[] args) {
    int x = 5;
    test(x);
  }
}
