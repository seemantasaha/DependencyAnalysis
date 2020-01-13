class Main {
  public static void main(String[] args) {
   test(5);
  }

  public static void test(int i) {
  	if (i > 1000)
      assert false;
  }
}
