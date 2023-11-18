
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.util.Random;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;

// @author Jared Scholz
public class SubdivisionDisplay extends JPanel {

    private SubdivisionNode rootNode;
    private final DrawPanel drawPanel;
    private final JLabel valueLabel;
    private final JTextArea subdivisionsList;
    private final JScrollPane listScrollPane;

    public SubdivisionDisplay() {
        super(new BorderLayout());
        rootNode = null;
        drawPanel = new DrawPanel();
        JPanel drawPanelHolder = new JPanel(new BorderLayout());
        drawPanelHolder.setBorder(new EmptyBorder(10, 20, 0, 10));
        drawPanelHolder.add(drawPanel, BorderLayout.CENTER);

        valueLabel = new JLabel();
        valueLabel.setBorder(new EmptyBorder(10, 20, 0, 20));

        subdivisionsList = new JTextArea();
        subdivisionsList.setEditable(false);
        subdivisionsList.setFocusable(false);
        subdivisionsList.setBackground(new Color(0, 0, 0, 0));
        listScrollPane = new JScrollPane(subdivisionsList);
        listScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        listScrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));
        listScrollPane.setPreferredSize(new Dimension(0, 500)); // to be overriden

        JButton recolorButton = new JButton("Recolor");
        recolorButton.setFocusable(false);
        JPanel buttonHolder = new JPanel();
        buttonHolder.setLayout(new BoxLayout(buttonHolder, BoxLayout.X_AXIS));
        buttonHolder.setBorder(new EmptyBorder(4, 20, 10, 20));
        buttonHolder.add(recolorButton);
        recolorButton.addActionListener((ActionEvent evt) -> {
            drawPanel.repaint();
        });

        super.add(drawPanelHolder, BorderLayout.CENTER);
        super.add(valueLabel, BorderLayout.NORTH);
        super.add(listScrollPane, BorderLayout.EAST);
        super.add(buttonHolder, BorderLayout.SOUTH);
    }

    public void display() {
        JFrame frame = new JFrame("Subdivision Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.setMinimumSize(new Dimension(300, 300));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void setRootNode(SubdivisionNode rootNode) {
        subdivisionsList.setText("");
        int numDivisions = traverseTreeRecursive(rootNode); // updates subdivisionsTextArea
        listScrollPane.setPreferredSize(new Dimension(subdivisionsList.getPreferredSize().width + 20, 500));
        valueLabel.setText("Value: $" + rootNode.getValue() + " (" + numDivisions + (numDivisions == 1 ? " Subdivision)" : " Subdivisions)"));
        this.rootNode = rootNode;
        drawPanel.repaint();
    }

    private int traverseTreeRecursive(SubdivisionNode current) {
        int numDivisions = 0;
        if (current.isLeaf()) {
            Dimension dimension = current.getDimension();
            subdivisionsList.append(dimension.width + "x" + dimension.height + " $" + current.getValue() + "\n");
        } else {
            numDivisions = 1;
            numDivisions += traverseTreeRecursive(current.getChildOne());
            numDivisions += traverseTreeRecursive(current.getChildTwo());
        }
        return numDivisions;
    }

    private class DrawPanel extends JPanel {

        private final Random randGen;
        private final Stroke dashedLine;

        public DrawPanel() {
            super.setPreferredSize(new Dimension(500, 500));
            randGen = new Random();
            dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (rootNode != null) {
                Dimension dimension = rootNode.getDimension();
                int width = getWidth();
                int height = getHeight();
                int xInterval = width / dimension.width;
                int yInterval = height / dimension.height;
                drawRecursively(g, rootNode, 0, 0, xInterval, yInterval);

                if (xInterval > 12 && yInterval > 12) { // don't draw lines when too small
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setStroke(dashedLine);
                    g2d.setColor(Color.WHITE);
                    int actualWidth = xInterval * dimension.width;
                    int actualHeight = yInterval * dimension.height;
                    for (int x = xInterval; x < actualWidth; x += xInterval) {
                        g2d.drawLine(x, 0, x, actualHeight);
                    }
                    for (int y = yInterval; y < actualHeight; y += yInterval) {
                        g2d.drawLine(0, y, actualWidth, y);
                    }
                }
            }
        }

        private void drawRecursively(Graphics g, SubdivisionNode current, int x, int y, int w, int h) {
            if (current.isLeaf()) {
                Dimension dimension = current.getDimension();
                g.setColor(getRandomColor());
                g.fillRect(x, y, dimension.width * w, dimension.height * h);
            }

            int splitIndex = current.getSplitIndex();
            if (splitIndex > 0) {
                drawRecursively(g, current.getChildOne(), x, y, w, h);
                drawRecursively(g, current.getChildTwo(), x + (splitIndex * w), y, w, h);
            } else if (splitIndex < 0) {
                drawRecursively(g, current.getChildOne(), x, y, w, h);
                drawRecursively(g, current.getChildTwo(), x, y - (splitIndex * h), w, h);
            }
        }

        private Color getRandomColor() {
            int r = randGen.nextInt(200) + 20;
            int g = randGen.nextInt(200) + 20;
            int b = randGen.nextInt(200) + 20;
            return new Color(r, g, b);
        }
    }
}
