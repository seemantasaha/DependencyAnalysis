public class Main {
  public static void main(String[] args) {
    test("hello");
  }

  public static void test(String arg) {
    if (arg.length() < 1)
      return;

    StringBuilder buffer1 = new StringBuilder();
    StringBuilder buffer2 = new StringBuilder(10);
    StringBuilder buffer3 = new StringBuilder(arg);

    assert false;
    //assert buffer2.length() == 0;
    //assert buffer3.length() > 0;
  }
}
