
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// @author Jared Scholz
public class PersistentDynamicSet<E> extends BinarySearchTree<E> {

    protected final Map<Integer, BinaryTreeNode> versionRoots;
    protected int nextVersion;
    protected BinaryTreeNode previous;

    public PersistentDynamicSet() {
        super();
        versionRoots = new HashMap();
        nextVersion = 0;
    }

    @Override
    public boolean add(E o) {
        if (rootNode == null) { // handle the first addition
            rootNode = createNode(o);
            previous = null;
            versionRoots.put(nextVersion++, rootNode);
            return true;
        } else {
            rootNode = duplicateNode(rootNode);
            previous = rootNode;
            if (super.add(o)) { // only save the new version if a change was made
                versionRoots.put(nextVersion++, rootNode);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (rootNode != null) {
            rootNode = duplicateNode(rootNode);
            previous = rootNode;
            if (super.remove(o)) { // only save the new version if a change was made
                versionRoots.put(nextVersion++, rootNode);
                return true;
            }
        }
        return false;
    }

    @Override
    protected BinaryTreeNode visitNode(BinaryTreeNode current) { // excludes the root
        if (current != null) {
            BinaryTreeNode newNode = duplicateNode(current);
            if (current == previous.leftChild) {
                previous.leftChild = newNode;
            } else { // current == previous.rightChild
                previous.rightChild = newNode;
            }
            previous = newNode;
            return newNode;
        }
        return null;
    }

    protected BinaryTreeNode duplicateNode(BinaryTreeNode node) { // hook method
        return new BinaryTreeNode(node);
    }

    public boolean setVersion(int version) {
        BinaryTreeNode savedRoot = versionRoots.get(version);
        if (savedRoot != null) {
            rootNode = savedRoot;
            previous = rootNode;
            return true;
        }
        return false;
    }

    public Set<Integer> getVersions() {
        return versionRoots.keySet();
    }
}
