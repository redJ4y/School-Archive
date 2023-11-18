package Q2;

// @author Jared Scholz
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Trie {

    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    private class TrieNode {

        public final Map<Character, TrieNode> children;
        public String element;

        public TrieNode() {
            // linked map provides fast iteration over values (used in suggestions)
            children = new LinkedHashMap<>();
            element = null;
        }
    }

    public boolean add(String element) {
        if (element == null) {
            return false;
        }
        char[] characters = element.toCharArray();
        TrieNode current = root;
        for (int i = 0; i < characters.length; i++) {
            TrieNode next = current.children.get(characters[i]);
            if (next != null) {
                current = next;
            } else {
                // quickly create a new branch:
                for (int j = i; j < characters.length; j++) {
                    TrieNode newNode = new TrieNode();
                    current.children.put(characters[j], newNode);
                    current = newNode;
                }
                break; // done
            }
        }
        current.element = element;
        return true;
    }

    public boolean remove(String element) {
        if (element == null) {
            return false;
        }
        return removeRecursive(root, element.toCharArray(), 0);
    }

    private boolean removeRecursive(TrieNode current, char[] characters, int index) {
        boolean contained = false;
        if (index < characters.length) {
            TrieNode next = current.children.get(characters[index]);
            if (next != null) { // check contains as we go
                // stack recursive calls for each node in the path
                contained = removeRecursive(next, characters, index + 1);
                // propagate back, removing links to now unused nodes (if the element was found)
                if (contained && next.children.isEmpty() && next.element == null) {
                    current.children.remove(characters[index]);
                }
            }
        } else {
            // the current node is the final node in the path
            current.element = null;
            contained = true;
        } // if a key was missing (element not found), propagate back with contained = false
        return contained;
    }

    public boolean contains(String element) {
        if (element == null) {
            return false;
        }
        char[] characters = element.toCharArray();
        TrieNode current = root;
        for (int i = 0; i < characters.length; i++) {
            current = current.children.get(characters[i]);
            if (current == null) {
                return false; // element not found
            }
        }
        // ensure that the target node contains the element:
        return current.element != null;
    }

    public boolean removeAll(String prefix) {
        if (prefix == null) {
            return false;
        }
        return removeAllRecursive(root, prefix.toCharArray(), 0);
    }

    private boolean removeAllRecursive(TrieNode current, char[] characters, int index) {
        boolean contained = false;
        if (index < characters.length - 1) {
            TrieNode next = current.children.get(characters[index]);
            if (next != null) { // check contains as we go
                // stack recursive calls for each node in the path, except for the last
                contained = removeAllRecursive(next, characters, index + 1);
                // propagate back, removing links to now unused nodes (if the prefix was found)
                if (contained && next.children.isEmpty() && next.element == null) {
                    current.children.remove(characters[index]);
                }
            }
        } else {
            // the current node is the second-to-last node in the path
            if (current.children.remove(characters[index]) != null) { // remove entire subtree
                contained = true; // prefix found
            }
        } // if a key was missing (prefix not found), propagate back with contained = false
        return contained;
    }

    public boolean startsWith(String prefix) {
        if (prefix == null) {
            return false;
        }
        char[] characters = prefix.toCharArray();
        TrieNode current = root;
        for (int i = 0; i < characters.length; i++) {
            current = current.children.get(characters[i]);
            if (current == null) {
                return false; // prefix not found
            }
        }
        // if the prefix has children there will be following elements
        return !current.children.isEmpty();
    }

    public Set<String> suggestions(String prefix) {
        Set<String> result = new LinkedHashSet<>();
        if (prefix == null) {
            return result; // return empty set
        }
        // quickly navigate to the end of the prefix:
        char[] characters = prefix.toCharArray();
        TrieNode current = root;
        for (int i = 0; i < characters.length; i++) {
            current = current.children.get(characters[i]);
            if (current == null) {
                return result; // prefix not found (return empty set)
            }
        } // current is now the last node in the prefix
        // find all elements in the subtree (current becomes the root):
        traverseTrieRecursive(current, result);
        return result;
    }

    private void traverseTrieRecursive(TrieNode current, Set<String> elements) {
        if (current.element != null) {
            elements.add(current.element);
        }
        for (TrieNode child : current.children.values()) {
            traverseTrieRecursive(child, elements);
        } // if the order of the set were important, elements could be stored with weights -
    } // - corresponding to their trie depth, or a breadth-first search could be used

    @Override
    public String toString() {
        return recursiveString(root, 0);
    }

    /* recursiveString method by @author Seth Hall */
    private String recursiveString(TrieNode current, int level) {
        String levelString = "";
        if (current.children.size() > 0) {
            Set<Character> chars = current.children.keySet();
            String tabs = "";
            for (int i = 0; i < level; i++) {
                tabs += "\t";
            }
            for (Character c : chars) {
                TrieNode child = current.children.get(c);
                levelString += tabs + " [" + c + "]";
                if (child.element != null) {
                    levelString += " >> " + child.element;
                }
                levelString += "\n";
                levelString += recursiveString(child, level + 1);
            }
        }
        return levelString;
    }

    /* MAIN FOR TESTING */
    public static void main(String[] args) {
        Trie testTrie = new Trie();
        testTrie.add("the");
        testTrie.add("then");
        testTrie.add("tea");
        testTrie.add("to");
        testTrie.add("him");
        testTrie.add("hit");
        testTrie.add("he");
        testTrie.add("her");
        testTrie.add("a");
        System.out.println("Starting trie:");
        System.out.println(testTrie);
        System.out.println("Contains \"then\": " + testTrie.contains("then"));
        System.out.println("Contains \"thim\": " + testTrie.contains("thim"));
        System.out.println("Contains \"he\": " + testTrie.contains("he"));
        System.out.println("Adding \"hi\", \"there\", and \"seth\"");
        testTrie.add("hi");
        testTrie.add("there");
        testTrie.add("seth");
        System.out.println("New trie:");
        System.out.println(testTrie);
        System.out.println("Something starts with \"th\": " + testTrie.startsWith("th"));
        System.out.println("Something starts with \"tx\": " + testTrie.startsWith("tx"));
        System.out.println("Suggestions for \"th\": " + testTrie.suggestions("th"));
        System.out.println("Suggestions for \"hi\": " + testTrie.suggestions("hi"));
        System.out.println("Suggestions for \"tx\": " + testTrie.suggestions("tx"));
        System.out.println("Attempt to remove \"thim\": " + testTrie.remove("thim"));
        System.out.println("Removing \"he\", \"the\", \"a\", and \"then\"");
        testTrie.remove("he");
        testTrie.remove("the");
        testTrie.remove("a");
        testTrie.remove("then");
        System.out.println("New trie:");
        System.out.println(testTrie);
        System.out.println("Removing \"there\"");
        testTrie.remove("there");
        System.out.println("New trie:");
        System.out.println(testTrie);
        System.out.println("Removing all that start with \"hi\"");
        testTrie.removeAll("hi");
        System.out.println("New trie:");
        System.out.println(testTrie);
    }
}
