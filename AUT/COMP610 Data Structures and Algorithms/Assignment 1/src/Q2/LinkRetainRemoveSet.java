package Q2;

// @author Jared Scholz
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class LinkRetainRemoveSet<E extends Comparable<E>> extends LinkedSet<E> {

    public LinkRetainRemoveSet() {
        super();
    }

    public LinkRetainRemoveSet(Collection<? extends E> c) {
        super(c);
    }

    @Override
    public boolean add(E o) {
        Node<E> newNode = new Node<>(o);
        if (firstNode == null) {
            firstNode = newNode; // empty set, create the head
        } else if (o.compareTo(firstNode.element) < 1) {
            if (o.compareTo(firstNode.element) == 0) { // rare, not worth saving first compareTo result
                return false; // EXIT - o already exists
            }
            // newNode belongs before firstNode (new head)
            newNode.next = firstNode;
            firstNode = newNode;
        } else {
            Node<E> current = firstNode.next;
            Node<E> previous = firstNode;
            while (current != null && o.compareTo(current.element) > 0) {
                previous = current;
                current = current.next;
            }
            if (current != null && o.compareTo(current.element) == 0) { // speed up loop by checking again
                return false; // EXIT - o already exisis
            }
            // current.element > o and o does not already exist, or previous is the tail
            newNode.next = current;
            previous.next = newNode;
        }
        return true;
    }

    public Set retain(E start, E end) throws IllegalArgumentException, NoSuchElementException {
        if (start != null && end != null && start.compareTo(end) >= 0) {
            throw new IllegalArgumentException("End argument not greater than start argument");
        }
        if (start != null && start.compareTo(firstNode.element) < 0) {
            throw new NoSuchElementException("Start argument not in range of set");
        } else if (start == null && end != null && end.compareTo(firstNode.element) < 0) {
            throw new NoSuchElementException("End argument not in range of set");
        }

        Set<E> removedSet = new LinkedHashSet<>(); // O(1) add operation
        Node<E> current = firstNode;
        Node<E> previous = null;

        if (start != null) {
            // go through elements less than start:
            while (current != null && current.element.compareTo(start) < 0) {
                removedSet.add(current.element);
                previous = current;
                current = current.next;
            }
            if (current == null) {
                throw new NoSuchElementException("Start argument not in range of set");
            }
            // current is now the first element to be retained
            if (previous != null) { // if previous == null, the very first element is to be retained
                firstNode = current; // retained set now starts at current
            }
        }

        if (end != null) {
            // skip through the elements to be retained:
            while (current != null && current.element.compareTo(end) < 0) {
                previous = current;
                current = current.next;
            }
            if (current == null) {
                throw new NoSuchElementException("End argument not in range of set");
            }
            // previous is now the last element to be retained
            if (previous != null) { // if previous == null, everything is to be retained
                previous.next = null; // sever the link
                // quickly add the remaining elements to removedSet:
                while (current != null) {
                    removedSet.add(current.element);
                    current = current.next;
                }
            }
        }
        return removedSet;
    }

    public Set remove(E start, E end) throws IllegalArgumentException, NoSuchElementException {
        if (start != null && end != null && start.compareTo(end) >= 0) {
            throw new IllegalArgumentException("End argument not greater than start argument");
        }
        if (start != null && start.compareTo(firstNode.element) < 0) {
            throw new NoSuchElementException("Start argument not in range of set");
        } else if (start == null && end != null && end.compareTo(firstNode.element) < 0) {
            throw new NoSuchElementException("End argument not in range of set");
        }

        Set<E> removedSet = new LinkedHashSet<>(); // O(1) add operation
        Node<E> relink = null;
        Node<E> current = firstNode;
        Node<E> previous = null;

        if (start != null) {
            // skip through elements less than start:
            while (current != null && current.element.compareTo(start) < 0) {
                previous = current;
                current = current.next;
            }
            if (current == null) {
                throw new NoSuchElementException("Start argument not in range of set");
            }
            // previous is now the last element to be kept
            if (previous != null) { // if previous == null, the very first element is to be removed
                relink = previous; // remember the last kept node to relink later
            }
        }
        previous = null; // previous pointer no longer needed

        // go through the elements to be removed:
        if (end != null) {
            while (current != null && current.element.compareTo(end) < 0) {
                removedSet.add(current.element);
                current = current.next;
            }
            if (current == null) {
                throw new NoSuchElementException("End argument not in range of set");
            }
        } else {
            // all of the remaining elements are to be removed
            while (current != null) {
                removedSet.add(current.element);
                current = current.next;
            }
        }
        // current is now the next element to be kept
        if (relink != null) {
            relink.next = current; // re-link both sides (middle removed)
        } else {
            firstNode = current; // the previous head has been removed
        }
        return removedSet;
    }

    /* MAIN FOR TESTING */
    public static void main(String[] args) {
        LinkRetainRemoveSet<String> testSetString = new LinkRetainRemoveSet<>(Arrays.asList("C", "F", "I", "P"));
        System.out.println(testSetString);
        System.out.println("Remove (null, null)");
        System.out.println("Returned set: " + testSetString.remove(null, null));
        System.out.println("New set: " + testSetString + "\n");

        LinkRetainRemoveSet<Integer> testSet;

        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Retain (2, 6)");
        System.out.println("Returned set: " + testSet.retain(2, 6));
        System.out.println("New set: " + testSet + "\n");

        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Remove (2, 6)");
        System.out.println("Returned set: " + testSet.remove(2, 6));
        System.out.println("New set: " + testSet + "\n");

        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Remove (4, 5)");
        System.out.println("Returned set: " + testSet.remove(4, 5));
        System.out.println("New set: " + testSet + "\n");

        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Retain (6, 7)");
        System.out.println("Returned set: " + testSet.retain(6, 7));
        System.out.println("New set: " + testSet + "\n");

        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Retain (null, 4)");
        System.out.println("Returned set: " + testSet.retain(null, 4));
        System.out.println("New set: " + testSet + "\n");

        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Retain (4, null)");
        System.out.println("Returned set: " + testSet.retain(4, null));
        System.out.println("New set: " + testSet + "\n");

        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Remove (4, null)");
        System.out.println("Returned set: " + testSet.remove(4, null));
        System.out.println("New set: " + testSet + "\n");

        // new test case:
        testSet = new LinkRetainRemoveSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        System.out.println(testSet);
        System.out.println("Remove (null, 4)");
        System.out.println("Returned set: " + testSet.remove(null, 4));
        System.out.println("New set: " + testSet + "\n");
    }
}
