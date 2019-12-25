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
    if (x > 46 || x == 32767) {
      return;
    }
    int result = fibonacci(x);
    // if (result >= x - 1) {
    //   return;
    // } else {
    //   assert false;
    // }
    //assert(result >= x - 1);
    assert false;
  }

  public static void main(String[] args) {
    int x = 5; 
    test(x);
  }
}
