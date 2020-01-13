public class Main {
  public static void do_stuff(String a, String b) {
    try {
      int x = Integer.parseInt(a);
      int y = Integer.parseInt(b);
      //assert Integer.parseInt(a) != Integer.parseInt(b) || x == y;
      assert false;
    } catch (java.lang.NumberFormatException e) {
    }
  }

  public static void main(String[] args) {
    test(5,"hello", "world");
  }

  public static void test(int size, String s1, String s2) {
    if (size < 2) {
      System.out.println("need two arguments");
      return;
    }
    String[] args = new String[size];
    args[0] = s1;
    args[1] = s2;
    do_stuff(args[0], args[1]);
  }
}
