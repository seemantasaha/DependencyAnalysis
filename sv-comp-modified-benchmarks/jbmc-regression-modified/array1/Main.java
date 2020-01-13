class Main {
  public static void main(String[] args) {
    test(5);
  }

  public static void test(int size) {
    if (size < 8)
      return;

    int int_array[] = new int[size];

    for (int i = 0; i < size; i++)
      int_array[i] = i;

    assert false;
  }
}
