package Q1;

// @author Jared Scholz
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class HotplateGUI extends JPanel implements ActionListener, ChangeListener {

    private final int GRID_SIZE = 25;

    private Element[][] elements;
    private int tempSetting;
    private JSlider tempSlider, heatConstSlider;
    private TitledBorder tempLabel, heatConstLabel; // save slider titles for modification
    private DrawPanel drawPanel;
    private Timer repaintTimer;

    public HotplateGUI() {
        super(new BorderLayout());
        tempSetting = 500;
        Element.HEAT_CONSTANT = 0.5;
        initializeElements();

        initializeSliders();
        JPanel sliderPanel = new JPanel();
        sliderPanel.add(tempSlider);
        sliderPanel.add(heatConstSlider);
        super.add(sliderPanel, BorderLayout.SOUTH);

        drawPanel = new DrawPanel();
        super.add(drawPanel, BorderLayout.CENTER);

        repaintTimer = new Timer(30, this);
        repaintTimer.start();
    }

    private void initializeElements() {
        elements = new Element[GRID_SIZE][GRID_SIZE];
        // fill 2D array with elements
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                elements[i][j] = new Element(0);
            }
        }

        // add neighbors and start threads
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                // only called for initialization, doesn't need to be fast
                if (i + 1 < GRID_SIZE) {
                    elements[i][j].addNeighbor(elements[i + 1][j]);
                }
                if (i > 0) {
                    elements[i][j].addNeighbor(elements[i - 1][j]);
                }
                if (j + 1 < GRID_SIZE) {
                    elements[i][j].addNeighbor(elements[i][j + 1]);
                }
                if (j > 0) {
                    elements[i][j].addNeighbor(elements[i][j - 1]);
                }
                elements[i][j].start();
            }
        }
    }

    private void initializeSliders() {
        tempSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, tempSetting);
        tempLabel = new TitledBorder("Temperature (" + tempSetting + ")");
        tempLabel.setTitleJustification(2);
        tempSlider.setBorder(tempLabel);
        tempSlider.addChangeListener(this);

        heatConstSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, (int) (Element.HEAT_CONSTANT * 100));
        // slider range is [1, 100], to be scaled to [0.01, 1] before using
        heatConstLabel = new TitledBorder("Heat Constant (" + Element.HEAT_CONSTANT + ")");
        heatConstLabel.setTitleJustification(2);
        heatConstSlider.setBorder(heatConstLabel);
        heatConstSlider.addChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        drawPanel.repaint();
    }

    @Override
    public void stateChanged(ChangeEvent e) { // listening for slider changes
        Object source = e.getSource();
        if (source == tempSlider) {
            tempSetting = tempSlider.getValue();
            tempLabel.setTitle("Temperature (" + tempSetting + ")");
        } else if (source == heatConstSlider) {
            Element.HEAT_CONSTANT = (double) heatConstSlider.getValue() / 100.0;
            heatConstLabel.setTitle("Heat Constant (" + Element.HEAT_CONSTANT + ")");
        }
    }

    private class DrawPanel extends JPanel implements MouseListener, MouseMotionListener {

        private ApplyHeatThread applyHeatThread;
        private Element selectedElement;
        private final Object selectedElementLock; // lock for synchronizing selectedElement
        private int rectWidth;
        private int rectHeight;
        private int xOffset;
        private int yOffset;

        public DrawPanel() {
            super.setPreferredSize(new Dimension(500, 500));
            selectedElementLock = new Object();
            rectWidth = 1;
            rectHeight = 1;
            xOffset = 0;
            yOffset = 0;
            addListeners();
        }

        private void addListeners() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            // also used by updateSelectedElement:
            rectWidth = getWidth() / GRID_SIZE;
            rectHeight = getHeight() / GRID_SIZE;
            xOffset = (getWidth() % GRID_SIZE) / 2;
            yOffset = (getHeight() % GRID_SIZE) / 2;

            int xCoordinate;
            int yCoordinate = yOffset; // offsets added to center the grid in the draw panel
            // x, y are for indexing elements, xCoord., yCoord. refer to panel position
            for (int y = 0; y < GRID_SIZE; y++) {
                xCoordinate = xOffset;
                for (int x = 0; x < GRID_SIZE; x++) {
                    g.setColor(getElementColor(x, y));
                    g.fillRect(xCoordinate, yCoordinate, rectWidth, rectHeight);
                    xCoordinate += rectWidth;
                }
                yCoordinate += rectHeight;
            }
        }

        private Color getElementColor(int x, int y) {
            int temperature = (int) elements[x][y].getTemperature();
            int r = 0;
            int g = 0;
            int b = 0;
            if (temperature < 256) {
                // 0 - 255 (blue to red)
                r = Math.max(temperature, 0);
                b = 255 - r;
            } else if (temperature < 512) {
                // 256 - 511 (red to yellow)
                r = 255;
                g = temperature - 256;
            } else if (temperature < 768) {
                // 512 - 767 (yellow to white)
                r = 255;
                g = 255;
                b = temperature - 512;
            } else {
                // 768+ (white)
                r = 255;
                g = 255;
                b = 255;
            }
            return new Color(r, g, b);
        } // I decided it would look better ranging from blue to white!

        @Override
        public void mousePressed(MouseEvent e) {
            updateSelectedElement(e);
            // apply heat in a new thread:
            applyHeatThread = new ApplyHeatThread();
            Thread thread = new Thread(applyHeatThread);
            thread.start();
        } // applying heat in a new thread increases responsiveness (and coding fun) but can cause flickering
        // this is because repainting and heat application intervals are not synced (non-issue)

        @Override
        public void mouseReleased(MouseEvent e) {
            applyHeatThread.requestStop(); // end current applyHeatThread within ~10ms
        } // a mouseReleased event must occur before another mousePressed event

        @Override
        public void mouseDragged(MouseEvent e) {
            updateSelectedElement(e);
        }

        private void updateSelectedElement(MouseEvent e) {
            int x = Math.max(Math.min((e.getX() - xOffset) / rectWidth, GRID_SIZE - 1), 0);
            int y = Math.max(Math.min((e.getY() - yOffset) / rectHeight, GRID_SIZE - 1), 0);
            // corrected for mouse moving off of the panel
            synchronized (selectedElementLock) {
                selectedElement = elements[x][y];
            }
        }

        private class ApplyHeatThread implements Runnable {

            private boolean stopRequested;

            public void requestStop() {
                stopRequested = true;
            }

            @Override
            public void run() {
                stopRequested = false;
                while (!stopRequested) {
                    synchronized (selectedElementLock) {
                        selectedElement.applyTempToElement(tempSetting);
                    } // tempSetting is not synchronized, but cannot be modified while this thread is running
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // IGNORE
                    }
                }
            } // runs while mouse is pressed, selectedElement may actively change
        }

        /* Unused interface methods below */
        @Override
        public void mouseMoved(MouseEvent e) {
        } // unused from MouseMotionListener

        @Override
        public void mouseEntered(MouseEvent e) {
        } // unused from MouseListener

        @Override
        public void mouseExited(MouseEvent e) {
        } // unused from MouseListener

        @Override
        public void mouseClicked(MouseEvent e) {
        } // unused from MouseListener
        /* End unused interface methods */
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Hotplate");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new HotplateGUI());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
