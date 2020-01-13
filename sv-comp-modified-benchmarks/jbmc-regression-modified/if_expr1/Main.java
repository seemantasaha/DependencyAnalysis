class Main {
  public static void main(String[] args) {
    test(5);
  }

  public static void test(int x) {
  	int y = x == 10 ? 11 : 9;
    if (x == 10)
      assert false;
      //assert y == 11;
    // else
    //   assert y == 9;
  }
};
