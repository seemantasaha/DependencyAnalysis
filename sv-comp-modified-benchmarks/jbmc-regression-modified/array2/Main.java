public class Main {

  public static void main(String[] args) {
    test(5);
  }

  public static void test(int unknown) {
    int[] arr;
    if (unknown > 0)
      arr = new int[1];
    else
      arr = new int[0];

    if (unknown > 0)
      arr[0] = 1;

    if(unknown > 0)
      assert false;
  }
}
