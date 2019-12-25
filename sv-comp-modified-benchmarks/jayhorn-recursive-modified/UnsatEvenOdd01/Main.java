public class Main {

  static int isOdd(int n) {
    if (n == 0) {
      return 0;
    } else if (n == 1) {
      return 1;
    } else {
      return isEven(n - 1);
    }
  }

  static int isEven(int n) {
    if (n == 0) {
      return 1;
    } else if (n == 1) {
      return 0;
    } else {
      return isOdd(n - 1);
    }
  }

  public static void test(int n) {
    if (n < 0) {
      return;
    }
    int result = isEven(n);
    int mod = n % 2;
    // if (result < 0 || result == mod) {
    //   return;
    // } else {
    //   assert false;
    // }
    //assert(result < 0 || result == mod);
    assert false;
  }

  public static void main(String[] args) {
    test(-24000);
    test(24000);
  }
}
