public class Node {

    int elem;
    Node next;
    static Node static_next;
    Node swapNode() {
      if (next != null)
        if (elem > next.elem) {
          assert false;
          Node t = next;
          next = t.next;
          t.next = this;
          return t;
        }

      return this;
    }
  }

