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
    int result = fibonacci(x);
    // if (x != 5 || result == 3) {
    //   return;
    // } else {
    //   assert false;
    // }
    //assert(x != 5 || result == 3);
    assert false;
  }

  static void main(String[] args) {
    test(5);
  }
}
