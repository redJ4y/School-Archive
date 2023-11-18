package view;

// @author jared
import model.entity.Item;
import model.map.Merchant;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;

public class MerchantView extends javax.swing.JPanel {

    private final Color HIGHLIGHT_COLOR = new Color(255, 153, 153); // to highlight text
    private final Color DEFAULT_COLOR = new Color(187, 187, 187); // to reset text

    private final ViewManager viewManager;

    private final ItemDisplay selectedItem;
    private final DefaultListModel inventoryModel; // model behind inventoryList
    private List<Item> inventoryItems; // store for display purposes
    private List<Integer> inventoryPrices; // store for validation purposes
    private int playerCoins; // store for validation purposes
    private boolean playerInvFull;

    /**
     * Creates new form MerchantView
     */
    public MerchantView(ViewManager viewManager) {
        this.viewManager = viewManager;
        initComponents();
        playerCoins = -1; // to be overwritten
        playerInvFull = true; // to be overwritten
        selectedItem = new ItemDisplay(null);
        selectedItemHolder.setLayout(new BorderLayout());
        selectedItemHolder.add(selectedItem, BorderLayout.CENTER);
        selectedItem.setAsInvisible();
        inventoryModel = new DefaultListModel();
        inventoryList.setModel(inventoryModel);
        inventoryItems = new ArrayList<>(1); // default value to avoid errors
        inventoryPrices = new ArrayList<>(1); // default value to avoid errors
    }

    /* Prepare the panel with new information */
    public void prepPanel(Merchant merchant, int coins, boolean invFull) {
        inventoryItems = merchant.getItems();
        inventoryPrices = merchant.getPrices();
        playerCoins = coins;
        playerInvFull = invFull;

        nameText.setText("Phew... It's a: " + merchant.getName());
        descText.setText(merchant.getDescription());
        numCoinsText.setText("You check your pockets and find that you have " + coins + " coins.");
        offeringText.setText("The " + merchant.getName() + " offers you a selection of items:");
        if (playerInvFull) {
            invFullLabel.setVisible(true);
        } else {
            invFullLabel.setVisible(false);
        }
        updateInventoryList();
        numCoinsText.setForeground(DEFAULT_COLOR);
        purchaseButton.setEnabled(false);
        selectedItem.setAsInvisible();
    }

    public void invNotFull() { // the player has dropped something
        int selectedIndex = inventoryList.getSelectedIndex();
        if (selectedIndex >= 0) { // enable button IF something is selected
            if (inventoryPrices.get(selectedIndex) > playerCoins) {
                numCoinsText.setForeground(HIGHLIGHT_COLOR);
                purchaseButton.setEnabled(false);
            } else {
                numCoinsText.setForeground(DEFAULT_COLOR);
                purchaseButton.setEnabled(true);
            }
        }
        invFullLabel.setVisible(false);
    }

    private void updateInventoryList() {
        inventoryModel.clear();
        int index = 0;
        for (Item current : inventoryItems) {
            // MUST maintain indexing of the inventory
            inventoryModel.add(index, current.getName() + " ~ " + inventoryPrices.get(index) + " Coins");
            index++;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        nameText = new javax.swing.JLabel();
        descText = new javax.swing.JLabel();
        numCoinsText = new javax.swing.JLabel();
        offeringText = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        inventoryList = new javax.swing.JList<>();
        selectedItemHolder = new javax.swing.JPanel();
        purchaseButton = new javax.swing.JButton();
        leaveButton = new javax.swing.JButton();
        invFullLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setMaximumSize(new java.awt.Dimension(378, 32767));

        jLabel1.setFont(new java.awt.Font("Palatino Linotype", 0, 11)); // NOI18N
        jLabel1.setText(". . .");

        nameText.setFont(new java.awt.Font("Palatino Linotype", 0, 11)); // NOI18N
        nameText.setText("Phew... It's a: Village Dweller");

        descText.setFont(new java.awt.Font("Palatino Linotype", 0, 11)); // NOI18N
        descText.setText("Maybe we can help eachother out.");

        numCoinsText.setFont(new java.awt.Font("Palatino Linotype", 0, 11)); // NOI18N
        numCoinsText.setText("You check your pockets and find that you have 325 coins.");

        offeringText.setFont(new java.awt.Font("Palatino Linotype", 0, 11)); // NOI18N
        offeringText.setText("The Village Dweller offers you a selection of items:");

        inventoryList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        inventoryList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                inventoryListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(inventoryList);

        selectedItemHolder.setMaximumSize(new java.awt.Dimension(366, 58));
        selectedItemHolder.setMinimumSize(new java.awt.Dimension(366, 58));
        selectedItemHolder.setPreferredSize(new java.awt.Dimension(366, 58));

        javax.swing.GroupLayout selectedItemHolderLayout = new javax.swing.GroupLayout(selectedItemHolder);
        selectedItemHolder.setLayout(selectedItemHolderLayout);
        selectedItemHolderLayout.setHorizontalGroup(
            selectedItemHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 366, Short.MAX_VALUE)
        );
        selectedItemHolderLayout.setVerticalGroup(
            selectedItemHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 58, Short.MAX_VALUE)
        );

        purchaseButton.setText("Purchase");
        purchaseButton.setFocusable(false);
        purchaseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                purchaseButtonActionPerformed(evt);
            }
        });

        leaveButton.setText("Leave");
        leaveButton.setFocusable(false);
        leaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leaveButtonActionPerformed(evt);
            }
        });

        invFullLabel.setFont(new java.awt.Font("Segoe UI", 2, 12)); // NOI18N
        invFullLabel.setForeground(new java.awt.Color(255, 153, 153));
        invFullLabel.setText("Inventory Full");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 2, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Merchant");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(32, 32, 32)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel1)
                                    .addComponent(offeringText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(numCoinsText, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                                    .addComponent(descText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(nameText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(selectedItemHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(94, 94, 94)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(invFullLabel)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(purchaseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(leaveButton, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel2)
                .addGap(12, 12, 12)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nameText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(descText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(numCoinsText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(offeringText)
                .addGap(24, 24, 24)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(selectedItemHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(leaveButton)
                    .addComponent(purchaseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(invFullLabel)
                .addContainerGap(34, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void purchaseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_purchaseButtonActionPerformed
        int selectedIndex = inventoryList.getSelectedIndex();
        purchaseButton.setEnabled(false);
        if (selectedIndex >= 0 && selectedIndex < inventoryItems.size()) { // validate
            if (inventoryPrices.get(selectedIndex) <= playerCoins) { // further validate
                viewManager.purchaseItem(selectedIndex);
            }
        }
    }//GEN-LAST:event_purchaseButtonActionPerformed

    private void leaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_leaveButtonActionPerformed
        viewManager.leavePressed(GameAreaOptions.MERCHANT);
    }//GEN-LAST:event_leaveButtonActionPerformed

    private void inventoryListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_inventoryListValueChanged
        if (!evt.getValueIsAdjusting()) {
            int selectedIndex = inventoryList.getSelectedIndex();
            if (selectedIndex < 0) {
                // no selection...
                numCoinsText.setForeground(DEFAULT_COLOR);
                purchaseButton.setEnabled(false);
                selectedItem.setAsInvisible();
            } else if (selectedIndex < inventoryItems.size()) { // validate
                selectedItem.setItem(inventoryItems.get(selectedIndex));
                if (!playerInvFull) { // only (maybe) enable button if inventory has room
                    if (inventoryPrices.get(selectedIndex) > playerCoins) {
                        numCoinsText.setForeground(HIGHLIGHT_COLOR);
                        purchaseButton.setEnabled(false);
                    } else {
                        numCoinsText.setForeground(DEFAULT_COLOR);
                        purchaseButton.setEnabled(true);
                    }
                }
            }
        }
    }//GEN-LAST:event_inventoryListValueChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel descText;
    private javax.swing.JLabel invFullLabel;
    private javax.swing.JList<String> inventoryList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton leaveButton;
    private javax.swing.JLabel nameText;
    private javax.swing.JLabel numCoinsText;
    private javax.swing.JLabel offeringText;
    private javax.swing.JButton purchaseButton;
    private javax.swing.JPanel selectedItemHolder;
    // End of variables declaration//GEN-END:variables
}
