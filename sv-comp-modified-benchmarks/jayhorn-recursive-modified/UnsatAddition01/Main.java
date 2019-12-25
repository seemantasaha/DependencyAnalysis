public class Main {
  static int addition(int m, int n) {
    if (n == 0) {
      return m;
    } else if (n > 0) {
      return addition(m + 1, n - 1);
    } else {
      return addition(m - 1, n + 1);
    }
  }

  public static void test(int m, int n) {
    int result = addition(m, n);
    // if (result == m - n) {
    //   return;
    // } else {
    //   assert false;
    // }
    //assert(result == m - n);
    assert false;
  }

  public static void main(String[] args) {
    test(-18000,-18000); 
    test(18000,18000);   
  }
}
