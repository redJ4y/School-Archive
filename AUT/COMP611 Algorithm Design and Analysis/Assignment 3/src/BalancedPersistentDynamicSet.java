
// @author Jared Scholz
public class BalancedPersistentDynamicSet<E> extends PersistentDynamicSet<E> {

    private RedBlackNode insertion; // the node just inserted into the tree (if applicable)

    public BalancedPersistentDynamicSet() {
        super();
    }

    @Override
    public boolean add(E o) {
        if (super.add(o)) {
            insertion.tempParent = (RedBlackNode) previous; // previous still holds reference to the parent
            doInsertFixup();
            return true;
        }
        return false;
    }

    private void doInsertFixup() {
        RedBlackNode current = insertion;
        while (current.tempParent != null && current.tempParent.isRed()) {
            if (current.tempParent == current.tempParent.tempParent.leftChild) {
                RedBlackNode uncle = (RedBlackNode) current.tempParent.tempParent.rightChild;
                if (uncle != null && uncle.isRed()) {
                    current.tempParent.setBlack();
                    uncle.setBlack();
                    current.tempParent.tempParent.setRed();
                    current = current.tempParent.tempParent;
                } else {
                    if (current == current.tempParent.rightChild) {
                        current = current.tempParent;
                        leftRotate(current, false);
                    }
                    current.tempParent.setBlack();
                    current.tempParent.tempParent.setRed();
                    rightRotate(current.tempParent.tempParent, false);
                }
            } else {
                RedBlackNode uncle = (RedBlackNode) current.tempParent.tempParent.leftChild;
                if (uncle != null && uncle.isRed()) {
                    current.tempParent.setBlack();
                    uncle.setBlack();
                    current.tempParent.tempParent.setRed();
                    current = current.tempParent.tempParent;
                } else {
                    if (current == current.tempParent.leftChild) {
                        current = current.tempParent;
                        rightRotate(current, false);
                    }
                    current.tempParent.setBlack();
                    current.tempParent.tempParent.setRed();
                    leftRotate(current.tempParent.tempParent, false);
                }
            }
        }
        ((RedBlackNode) rootNode).setBlack();
    }

    @Override
    protected RedBlackNode makeReplacement(BinaryTreeNode removalNode) {
        RedBlackNode toRemove = (RedBlackNode) removalNode;
        RedBlackNode successor = null;
        RedBlackNode checkNode = null;
        if (toRemove.leftChild != null && toRemove.rightChild != null) {
            successor = visitNode(toRemove.rightChild);
            do {
                successor = visitNode(successor.leftChild);
            } while (successor.leftChild != null);
            successor.tempParent.leftChild = successor.rightChild;
            if (successor.rightChild != null) {
                ((RedBlackNode) successor.rightChild).tempParent = successor.tempParent;
            }
            if (successor.isBlack()) {
                checkNode = visitNode(successor.rightChild);
            }
            successor.leftChild = toRemove.leftChild;
            ((RedBlackNode) toRemove.leftChild).tempParent = successor;
            successor.rightChild = toRemove.rightChild;
            ((RedBlackNode) toRemove.rightChild).tempParent = successor;
            successor.isBlack = toRemove.isBlack;
        } else if (toRemove.leftChild != null || toRemove.rightChild != null) {
            if (toRemove.leftChild != null) {
                successor = visitNode(toRemove.leftChild);
            } else {
                successor = visitNode(toRemove.rightChild);
            }
            if (toRemove.isBlack()) {
                checkNode = successor;
            }
        } else {
            successor = null;
            if (toRemove.isBlack()) {
                checkNode = toRemove.tempParent;
            }
        }
        if (successor != null) {
            successor.tempParent = toRemove.tempParent;
        }
        if (toRemove.tempParent == null) {
            rootNode = successor;
        } else if (toRemove == toRemove.tempParent.leftChild) {
            toRemove.tempParent.leftChild = successor;
        } else {
            toRemove.tempParent.rightChild = successor;
        }
        if (checkNode != null) {
            doDeleteFixup(checkNode);
        }
        return successor;
    }

    private void doDeleteFixup(RedBlackNode checkNode) {
        RedBlackNode current = checkNode;
        while (current != rootNode && current.isBlack()) {
            if (current == current.tempParent.leftChild) {
                RedBlackNode sibling = (RedBlackNode) current.tempParent.rightChild;
                if (sibling != null && sibling.isRed()) {
                    sibling.setBlack();
                    current.tempParent.setRed();
                    leftRotate(current.tempParent, false);
                    sibling = (RedBlackNode) current.tempParent.rightChild;
                }
                if (sibling == null
                        || ((sibling.leftChild == null || ((RedBlackNode) sibling.leftChild).isBlack())
                        && (sibling.rightChild == null || ((RedBlackNode) sibling.rightChild).isBlack()))) {
                    if (sibling != null) {
                        sibling.setRed();
                    }
                    current = current.tempParent;
                } else {
                    if (sibling.rightChild == null || ((RedBlackNode) sibling.rightChild).isBlack()) {
                        ((RedBlackNode) sibling.rightChild).setBlack();
                        sibling.setRed();
                        rightRotate(sibling.duplicateKeepParentUpdateChildren(), true); // operate on a clone of the sibling
                        sibling = (RedBlackNode) current.tempParent.rightChild;
                    }
                    sibling.isBlack = current.tempParent.isBlack;
                    current.tempParent.setBlack();
                    ((RedBlackNode) sibling.rightChild).setBlack();
                    leftRotate(current.tempParent, false);
                    current = (RedBlackNode) rootNode;
                }
            } else {
                RedBlackNode sibling = (RedBlackNode) current.tempParent.leftChild;
                if (sibling != null && sibling.isRed()) {
                    sibling.setBlack();
                    current.tempParent.setRed();
                    rightRotate(current.tempParent, false);
                    sibling = (RedBlackNode) current.tempParent.leftChild;
                }
                if (sibling == null
                        || ((sibling.rightChild == null || ((RedBlackNode) sibling.rightChild).isBlack())
                        && (sibling.leftChild == null || ((RedBlackNode) sibling.leftChild).isBlack()))) {
                    if (sibling != null) {
                        sibling.setRed();
                    }
                    current = current.tempParent;
                } else {
                    if (sibling.leftChild == null || ((RedBlackNode) sibling.leftChild).isBlack()) {
                        ((RedBlackNode) sibling.leftChild).setBlack();
                        sibling.setRed();
                        leftRotate(sibling.duplicateKeepParentUpdateChildren(), true); // operate on a clone of the sibling
                        sibling = (RedBlackNode) current.tempParent.leftChild;
                    }
                    sibling.isBlack = current.tempParent.isBlack;
                    current.tempParent.setBlack();
                    ((RedBlackNode) sibling.leftChild).setBlack();
                    rightRotate(current.tempParent, false);
                    current = (RedBlackNode) rootNode;
                }
            }
        }
        current.setBlack();
    }

    protected void leftRotate(RedBlackNode node, boolean forceDuplication) { // forceDuplication is necessary when using duplicateKeepParentUpdateChildren()
        RedBlackNode parent = node.tempParent;
        RedBlackNode toPromote = (RedBlackNode) node.rightChild;
        if (forceDuplication || toPromote.tempParent != node) { // check if the promotee is outside of the current path (not yet cloned)
            toPromote = duplicateNode(toPromote); // operate on a clone so as not to disturb previous versions
            if (toPromote.rightChild != null) {
                ((RedBlackNode) toPromote.rightChild).tempParent = toPromote; // update child to point towards the clone
            }
        }
        if (parent == null) { // special case for modifying the root:
            rootNode = toPromote;
            versionRoots.put(nextVersion - 1, rootNode); // fix version entry
        } else {
            // update parent to point towards the promotee:
            if (node == parent.leftChild) {
                parent.leftChild = toPromote;
            } else { // node == parent.rightChild
                parent.rightChild = toPromote;
            }
        }
        toPromote.tempParent = parent; // update the promotee's tempParent reference
        node.rightChild = toPromote.leftChild;
        toPromote.leftChild = node;
        node.tempParent = toPromote; // update the target's tempParent reference
        if (node.rightChild != null) {
            ((RedBlackNode) node.rightChild).tempParent = node; // update affected child's tempParent reference
        }
    }

    protected void rightRotate(RedBlackNode node, boolean forceDuplication) { // forceDuplication is necessary when using duplicateKeepParentUpdateChildren()
        RedBlackNode parent = node.tempParent;
        RedBlackNode toPromote = (RedBlackNode) node.leftChild;
        if (forceDuplication || toPromote.tempParent != node) { // check if the promotee is outside of the current path (not yet cloned)
            toPromote = duplicateNode(toPromote); // operate on a clone so as not to disturb previous versions
            if (toPromote.leftChild != null) {
                ((RedBlackNode) toPromote.leftChild).tempParent = toPromote; // update child to point towards the clone
            }
        }
        if (parent == null) { // special case for modifying the root:
            rootNode = toPromote;
            versionRoots.put(nextVersion - 1, rootNode); // fix version entry
        } else {
            // update parent to point towards the promotee:
            if (node == parent.leftChild) {
                parent.leftChild = toPromote;
            } else { // node == parent.rightChild
                parent.rightChild = toPromote;
            }
        }
        toPromote.tempParent = parent; // update the promotee's tempParent reference
        node.leftChild = toPromote.rightChild;
        toPromote.rightChild = node;
        node.tempParent = toPromote; // update the target's tempParent reference
        if (node.leftChild != null) {
            ((RedBlackNode) node.leftChild).tempParent = node; // update affected child's tempParent reference
        }
    }

    @Override
    protected RedBlackNode visitNode(BinaryTreeNode current) { // excludes the root
        RedBlackNode parent = (RedBlackNode) previous; // get the parent before calling super.visitNode
        RedBlackNode clone = (RedBlackNode) super.visitNode(current);
        clone.tempParent = parent; // update tempParent references as you go
        return clone;
    }

    @Override
    protected RedBlackNode createNode(E o) {
        insertion = new RedBlackNode(o); // keep track of the inserted node
        return insertion; // make the underlying tree operate on RedBlackNodes
    }

    @Override
    protected RedBlackNode duplicateNode(BinaryTreeNode node) {
        return new RedBlackNode((RedBlackNode) node); // duplicate RedBlackNodes
    }

    protected class RedBlackNode extends BinaryTreeNode {

        protected boolean isBlack;
        public RedBlackNode tempParent; // only an accurate parent reference in the current path!

        public RedBlackNode(E element) {
            super(element);
            isBlack = false;
            tempParent = null;
        }

        public RedBlackNode(RedBlackNode node) { // doesn't keep tempParent
            super(node);
            isBlack = node.isBlack;
            tempParent = null;
        }

        public RedBlackNode duplicateKeepParentUpdateChildren() { // bestNameICouldThinkOf
            RedBlackNode clone = new RedBlackNode(this);
            clone.tempParent = tempParent;
            ((RedBlackNode) clone.rightChild).tempParent = clone;
            ((RedBlackNode) clone.leftChild).tempParent = clone;
            return clone;
        }

        public void setBlack() {
            isBlack = true;
        }

        public void setRed() {
            isBlack = false;
        }

        public boolean isBlack() {
            return isBlack;
        }

        public boolean isRed() {
            return !isBlack;
        }

        @Override
        public String getText() {
            return element.toString() + " (" + (isBlack ? "B" : "R") + ")";
        }
    }
}
