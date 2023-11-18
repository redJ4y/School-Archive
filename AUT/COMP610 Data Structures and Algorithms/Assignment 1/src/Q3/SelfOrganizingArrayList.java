package Q3;

// @author Jared Scholz
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class SelfOrganizingArrayList<E> implements List<E> {

    private static final int INITIAL_CAPACITY = 20;

    private SOALElement<E>[] elements;
    private int size;

    /* Private class to wrap elements with access counters */
    private class SOALElement<E> {

        public E element;
        public int accesses;

        public SOALElement(E element) {
            this.element = element;
            accesses = 1;
        }
    }

    public SelfOrganizingArrayList() {
        elements = new SOALElement[INITIAL_CAPACITY];
        size = 0;
    }

    public SelfOrganizingArrayList(Collection<? extends E> c) {
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
        return indexOf(o) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new SOALIterator();
    }

    private class SOALIterator implements Iterator<E> {

        protected int current;

        public SOALIterator() {
            current = 0;
        }

        public SOALIterator(int index) {
            current = index;
        }

        @Override
        public boolean hasNext() {
            return current < size - 1;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return elements[current++].element;
        }
    }

    @Override
    public Object[] toArray() {
        E[] result = (E[]) new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = elements[i].element;
        }
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray(); // implementation always returns a new array
    }

    @Override
    public boolean add(E e) {
        if (size >= elements.length) {
            expandCapacity();
        }
        elements[size] = new SOALElement<>(e);
        size++;
        return true;
    }

    private void expandCapacity() {
        elements = Arrays.copyOf(elements, elements.length * 2);
        /* Alternative (manual) expandCapacity:
        SOALElement<E>[] newElements = new SOALElement[elements.length * 2];
        for (int i = 0; i < size; i++) {
            newElements[i] = elements[i];
        }
        elements = newElements; */
    }

    @Override
    public boolean remove(Object o) {
        int index = 0;
        while (index < size) { // linear search
            if (elements[index].element.equals(o)) {
                break;
            }
            index++;
        }
        if (index == size) {
            return false;
        }
        // shift elements down to fill gap:
        while (index < size - 1) {
            elements[index] = elements[++index];
        }
        elements[index] = null;
        size--;
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object current : c) {
            if (indexOf(current) < 0) {
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
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("addAll(index) not supported");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean containsAll = true;
        for (Object current : c) {
            if (!remove(current)) {
                containsAll = false;
            }
        }
        return containsAll; // return whether or not everything could be removed
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c.isEmpty()) {
            clear();
        } else {
            for (int i = size - 1; i >= 0; i--) { // start at right for less shifting
                if (!c.contains(elements[i].element)) {
                    remove(i);
                }
            }
        }
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            elements[i] = null;
        }
        size = 0;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("index out of bounds");
        }
        return elements[index].element;

    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("set(index) not supported");
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("add(index) not supported");
    }

    @Override
    public E remove(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("index out of bounds");
        }
        E removedElement = elements[index].element;
        // shift elements down to fill gap:
        while (index < size - 1) {
            elements[index] = elements[++index];
        }
        elements[index] = null;
        size--;
        return removedElement;
    }

    @Override
    public int indexOf(Object o) {
        int index = 0;
        while (index < size) { // linear search
            if (elements[index].element.equals(o)) {
                break;
            }
            index++;
        }
        if (index == size) {
            return -1;
        }
        elements[index].accesses++;
        return bubbleElement(index);
    }

    private int bubbleElement(int index) {
        while (index > 0 && elements[index].accesses >= elements[index - 1].accesses) {
            SOALElement<E> temp = elements[index];
            elements[index] = elements[index - 1];
            elements[index - 1] = temp;
            index--;
        }
        return index;
    } // would be cool to execute bubble in a new thread

    @Override
    public int lastIndexOf(Object o) {
        int index = size - 1;
        while (index >= 0) { // linear search
            if (elements[index].element.equals(o)) {
                break;
            }
            index--;
        }
        if (index < 0) {
            return -1;
        }
        return index; // does not increase element access counter...
    } // doing so could result in a "leapfrog" effect, where bubbling makes it no longer the last o

    @Override
    public ListIterator<E> listIterator() {
        return new SOALListIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("index out of bounds");
        }
        return new SOALListIterator(index);
    }

    private class SOALListIterator extends SOALIterator implements ListIterator<E> {

        public SOALListIterator(int index) {
            super(index);
        }

        @Override
        public boolean hasPrevious() {
            return current > 0;
        }

        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return elements[--current].element;
        }

        @Override
        public int nextIndex() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current + 1;
        }

        @Override
        public int previousIndex() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return current - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException("set not supported");
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException("add not supported");
        }
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= size) {
            throw new IllegalArgumentException("fromIndex out of bounds");
        }
        if (toIndex < 0 || toIndex >= size) {
            throw new IllegalArgumentException("toIndex out of bounds");
        }
        if (fromIndex >= toIndex) {
            throw new IllegalArgumentException("toIndex not greater than fromIndex");
        }
        List<E> subList = new SelfOrganizingArrayList<>();
        for (int i = fromIndex; i <= toIndex; i++) {
            subList.add(elements[i].element);
        }
        return subList; // does not maintain access counters
    }

    @Override
    public String toString() {
        String output = "[";
        for (int i = 0; i < size; i++) {
            output += elements[i].element + "(" + elements[i].accesses + ")";
            if (i < size - 1) {
                output += ", ";
            }
        }
        output += "]";
        return output;
    }

    /* MAIN FOR TESTING:
    public static void main(String[] args) {
        SelfOrganizingArrayList<String> test = new SelfOrganizingArrayList<>();
        test.add("A");
        test.add("B");
        test.add("C");
        test.add("D");
        test.add("E");
        System.out.println(test);
        System.out.println("Contains C: " + test.contains("C"));
        System.out.println(test);
        System.out.println("Index of B: " + test.indexOf("B"));
        System.out.println(test);
        System.out.println("Index of B: " + test.indexOf("B"));
        System.out.println("C Removed: " + test.remove("C"));
        System.out.println("Contains A: " + test.contains("A"));
        System.out.println(test);
    } */
}
