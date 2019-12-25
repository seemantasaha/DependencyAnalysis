public class Main {

  public static void main(String[] args) {
    test(5);
  }
  /* we want to let the user specify that this method should be symbolic */

  public static void test(int x) {
    x = x + 1;

    //	  switch (x) {
    //	  case 0: System.out.println("branch Foo0"); break;
    //	  case 1: System.out.println("branch Foo1"); break;
    //	  default: System.out.println("default1"); break;
    //	  }

    switch (x) {
    case 2:
      System.out.println("branch Foo2");
      break;
    case 3000:
      System.out.println("branch Foo3000");
      break;
    default:
      System.out.println("default2");
      assert false;
      break;
    }
  }
}
