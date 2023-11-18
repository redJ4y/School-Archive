package Q3;

// @author Jared Scholz
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class HashSetWithChaining<E> implements Set<E> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;

    private Node<E>[] hashTable;
    private double loadFactor;
    private int size;

    private class Node<E> {

        public E element;
        public Node<E> next;

        public Node(E element) {
            this.element = element;
            next = null;
        }
    }

    public HashSetWithChaining(int initialCapacity, double loadFactor) {
        hashTable = new Node[initialCapacity];
        this.loadFactor = loadFactor;
        size = 0;
    }

    public HashSetWithChaining(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public HashSetWithChaining() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public HashSetWithChaining(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false; // null unsupported
        }
        Node<E> target = hashTable[Math.abs(o.hashCode()) % hashTable.length];
        // check the entire chain for a match:
        while (target != null) {
            if (target.element.equals(o)) {
                return true;
            }
            target = target.next;
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return new HSWCIterator();
    }

    private class HSWCIterator implements Iterator<E> {

        private int currentIndex;
        private Node<E> nextNode;
        private int numPassed;

        public HSWCIterator() {
            currentIndex = 0;
            nextNode = null;
            numPassed = 0;
        }

        @Override
        public boolean hasNext() {
            return numPassed < size;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            while (nextNode == null) { // move to the next head if necessary
                nextNode = hashTable[currentIndex++];
            }
            E element = nextNode.element;
            nextNode = nextNode.next; // shift nextNode
            numPassed++;
            return element;
        }
    }

    @Override
    public Object[] toArray() {
        E[] result = (E[]) new Object[size];
        int index = 0;
        for (Node<E> current : hashTable) {
            if (current != null) {
                result[index++] = current.element;
                while ((current = current.next) != null) {
                    result[index++] = current.element;
                }
            }
        }
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray(); // implementation always returns a new array
    }

    @Override
    public boolean add(E e) {
        if (e == null) {
            throw new NullPointerException("null unsupported");
        }
        int index = Math.abs(e.hashCode()) % hashTable.length;
        Node<E> existing = hashTable[index];
        if (existing != null) {
            // check the entire chain to ensure element is unique...
            if (existing.element.equals(e)) {
                return false; // element is already contained
            } // (first element checked outside so loop can look forward)
            while (existing.next != null) {
                existing = existing.next;
                if (existing.element.equals(e)) {
                    return false; // element is already contained
                }
            }
            existing.next = new Node(e); // add to tail (we already have reference)
        } else {
            hashTable[index] = new Node(e);
        }
        size++;
        if ((double) size / hashTable.length > loadFactor) {
            expandCapacity();
        }
        return true;
    }

    private void expandCapacity() {
        Node<E>[] oldTable = hashTable;
        // transfer elements over directly
        hashTable = new Node[oldTable.length * 2];
        size = 0;

        for (Node<E> current : oldTable) {
            if (current != null) {
                add(current.element);
                // also add any chained elements:
                while ((current = current.next) != null) {
                    add(current.element);
                }
            }
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false; // null unsupported
        }
        int index = Math.abs(o.hashCode()) % hashTable.length;
        Node<E> target = hashTable[index];
        // check the entire chain for a match...
        if (target.element.equals(o)) {
            // remove the first node at index
            hashTable[index] = target.next; // target.next may be null
            size--;
            return true;
        } else {
            while (target.next != null) {
                if (target.next.element.equals(o)) {
                    // remove link from chain
                    target.next = target.next.next;
                    size--;
                    return true;
                }
                target = target.next;
            }
            return false;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object current : c) {
            if (!contains(current)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return false;
        }
        for (E element : c) {
            add(element);
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c.isEmpty()) {
            clear();
            return true;
        }
        boolean modified = false;
        for (Object current : toArray()) {
            if (!c.contains(current)) {
                remove(current);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (Object current : c) {
            if (remove(current)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        for (int i = 0; i < hashTable.length; i++) {
            if (hashTable[i] != null) {
                // do not wait for the garbage collector...
                deleteChainRecursive(hashTable[i]);
                hashTable[i] = null; // delete head
            }
        }
    }

    /* Deletes a chain (excluding the head) */
    private void deleteChainRecursive(Node<E> current) {
        if (current.next != null) {
            deleteChainRecursive(current.next);
        }
        // propagate back removing links
        current.next = null;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < hashTable.length; i++) {
            result += "[" + i + "] ";
            if (hashTable[i] != null) {
                Node<E> existing = hashTable[i];

                result += existing.element.toString();
                while (existing.next != null) {
                    existing = existing.next;
                    result += " -> " + existing.element.toString();
                }
            }
            result += "\n";
        }
        return result;
    }

    /* MAIN FOR TESTING */
    public static void main(String[] args) {
        Set<String> testSet = new HashSetWithChaining<>(6);
        System.out.println("Testing add");
        testSet.add("Seth");
        testSet.add("Seth"); // ensure it functions as a set
        testSet.add("Bob");
        testSet.add("Adam");
        testSet.add("Ian");
        System.out.println("----------\n" + testSet + "----------");
        System.out.println("Size is: " + testSet.size());
        System.out.println("Testing addAll");
        testSet.addAll(Arrays.asList("Jill", "Amy", "Nat", "Seth", "Bob", "Simon", "Andy"));
        System.out.println("----------\n" + testSet + "----------");
        System.out.println("Size is: " + testSet.size());
        System.out.println("Contains Seth: " + testSet.contains("Seth"));
        System.out.println("Contains Nat: " + testSet.contains("Nat"));
        System.out.println("Contains Gary: " + testSet.contains("Gary"));
        System.out.print("Iterator: ");
        Iterator testIterator = testSet.iterator();
        while (testIterator.hasNext()) {
            System.out.print(testIterator.next() + ", ");
        }
        System.out.println();
        System.out.println("Testing remove");
        testSet.remove("Seth");
        testSet.remove("Adam");
        testSet.remove("Bob");
        System.out.println("----------\n" + testSet + "----------");
        System.out.println("Size is: " + testSet.size());
        System.out.println("Try removing Jimbo: " + testSet.remove("Jimbo"));
        System.out.println("Testing retainAll (Amy, Simon, Andy, and Nat)");
        testSet.retainAll(Arrays.asList("Amy", "Simon", "Andy", "Nat"));
        System.out.println("----------\n" + testSet + "----------");
        System.out.println("Size is: " + testSet.size());
        System.out.println("Testing removeAll (Simon and Nat)");
        testSet.removeAll(Arrays.asList("Simon", "Nat"));
        System.out.println("----------\n" + testSet + "----------");
        System.out.println("Size is: " + testSet.size());
        System.out.println("Testing removing at end of chain");
        System.out.println("(Adding Adam -> Jill, removing Jill)");
        testSet.add("Adam");
        testSet.add("Jill");
        testSet.remove("Jill");
        System.out.println("----------\n" + testSet + "----------");
    }
}
