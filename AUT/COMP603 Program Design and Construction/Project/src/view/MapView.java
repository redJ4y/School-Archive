package view;

// @author jared
import controller.GameDriver;
import model.entity.TravelMap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MapView extends javax.swing.JPanel {

    private final DrawPanel mapDrawPanel;
    private TravelMap map;
    private Point currentPosition;

    /**
     * Creates new form MapView
     */
    public MapView() {
        initComponents();
        // add the compass icon:
        ImageIcon compassIcon = new ImageIcon(new ImageIcon("icons/compass.png").getImage().getScaledInstance(62, 62, Image.SCALE_SMOOTH));
        JLabel iconWrapper = new JLabel(compassIcon, JLabel.CENTER);
        compassHolder.setLayout(new BorderLayout());
        compassHolder.add(iconWrapper, BorderLayout.CENTER);
        // prepare the map zone:
        mapHolder.setLayout(new BorderLayout());
        mapDrawPanel = new DrawPanel();
        mapHolder.add(mapDrawPanel, BorderLayout.CENTER);
    }

    /* Re-draws an updated map */
    public void updateMap(TravelMap map, Point currentPosition) {
        this.map = map;
        this.currentPosition = currentPosition;
        mapDrawPanel.repaint();
    }

    /* Custom class to draw the map of ovals */
    private class DrawPanel extends JPanel {

        private final int GRID_SIZE = GameDriver.MAP_SIZE;
        private final Color BASE_COLOR = new Color(100, 100, 100);
        private final Color VISITED_COLOR = new Color(187, 187, 187);
        private final Color DEFEATED_COLOR = new Color(201, 105, 91);
        private final Color CURRENT_COLOR = new Color(91, 148, 201);

        private char[][] mapArray;

        public DrawPanel() {
            super.setPreferredSize(new Dimension(320, 306));
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (map != null) { // (allows viewing before proper initialization for development)
                mapArray = map.getArray(); // get the underlying 2D char array
                int rectWidth = getWidth() / GRID_SIZE;
                int rectHeight = getHeight() / GRID_SIZE;
                int xOffset = (getWidth() % GRID_SIZE) / 2; // x offset to center map horizontally
                int yCoordinate = 0;
                for (int y = 0; y < GRID_SIZE; y++) {
                    int xCoordinate = xOffset;
                    for (int x = 0; x < GRID_SIZE; x++) {
                        g.setColor(getDotColor(x, y)); // get color based on mapArray
                        g.fillOval(xCoordinate, yCoordinate, rectWidth - 3, rectHeight - 3); // add gaps
                        xCoordinate += rectWidth;
                    }
                    yCoordinate += rectHeight;
                }
            }
        }

        /* Returns a color according to what is in the TravelMap */
        private Color getDotColor(int x, int y) {
            if (y == currentPosition.x && x == currentPosition.y) {
                return CURRENT_COLOR;
            } else {
                switch (mapArray[y][x]) {
                    case ' ':
                        return BASE_COLOR;
                    case 'O':
                        return VISITED_COLOR;
                    case 'X':
                        return DEFEATED_COLOR;
                }
            }
            return BASE_COLOR; // default if there is a problem
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
        compassHolder = new javax.swing.JPanel();
        mapHolder = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 3, 12)); // NOI18N
        jLabel1.setText("Your tattered map:");

        compassHolder.setMaximumSize(new java.awt.Dimension(74, 74));
        compassHolder.setMinimumSize(new java.awt.Dimension(74, 74));
        compassHolder.setPreferredSize(new java.awt.Dimension(74, 74));

        javax.swing.GroupLayout compassHolderLayout = new javax.swing.GroupLayout(compassHolder);
        compassHolder.setLayout(compassHolderLayout);
        compassHolderLayout.setHorizontalGroup(
            compassHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 74, Short.MAX_VALUE)
        );
        compassHolderLayout.setVerticalGroup(
            compassHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 74, Short.MAX_VALUE)
        );

        mapHolder.setMaximumSize(new java.awt.Dimension(320, 320));
        mapHolder.setMinimumSize(new java.awt.Dimension(320, 320));

        javax.swing.GroupLayout mapHolderLayout = new javax.swing.GroupLayout(mapHolder);
        mapHolder.setLayout(mapHolderLayout);
        mapHolderLayout.setHorizontalGroup(
            mapHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 320, Short.MAX_VALUE)
        );
        mapHolderLayout.setVerticalGroup(
            mapHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 320, Short.MAX_VALUE)
        );

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(201, 105, 91));
        jLabel2.setText("Defeated");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(91, 148, 201));
        jLabel3.setText("Current");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(mapHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel2))
                            .addComponent(jLabel1))
                        .addGap(130, 130, 130)
                        .addComponent(compassHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(28, 28, 28))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(jLabel1)
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(compassHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mapHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(50, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel compassHolder;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel mapHolder;
    // End of variables declaration//GEN-END:variables
}