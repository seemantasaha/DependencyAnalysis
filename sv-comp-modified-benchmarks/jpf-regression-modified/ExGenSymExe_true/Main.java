public class Main {

  public static void main(String[] args) {
    Node n = new Node();
    Node m = new Node();
    n.next = m;
    n.next.next = n;
    n.elem = 5;
    m.elem = 6;
    Node na = n.swapNode();
    // Debug.printSymbolicRef(n);
    // Debug.printPC("\nPC");
    // Debug.printHeapPC("Heap PC");
  }
}
