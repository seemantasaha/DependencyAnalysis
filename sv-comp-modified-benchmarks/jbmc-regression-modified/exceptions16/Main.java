class A extends RuntimeException {}
class B extends A {}
class C extends B {}

public class Main {
  public static void main(String[] args) {
    test(5);
  }

  public static void test(int x) {
    try {
      x++;
    } catch (A exc) {
      assert false;
    }

    // try {
    //   throw new B();
    // } catch (B exc) {
    //   assert false;
    // }
  }
}
