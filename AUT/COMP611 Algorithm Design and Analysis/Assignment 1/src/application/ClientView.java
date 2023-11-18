package application;

// @author Jared Scholz
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ClientView extends JPanel {

    private final Client controller;
    private File currentInputFile;
    private File currentOutputFile;

    private JFileChooser fileChooser;
    private JButton chooseInputFileButton;
    private JButton chooseOutputFileButton;
    private JLabel inputFileLabel;
    private JLabel outputFileLabel;
    private ImagePanel inputImage;
    private ImagePanel outputImage;
    private JProgressBar progressBar;
    private JButton processButton;

    public ClientView(Client controller) {
        super(new BorderLayout());
        this.controller = controller;
        currentInputFile = null;
        currentOutputFile = null;

        super.setBorder(new EmptyBorder(6, 6, 6, 6));
        initComponents();
        addListeners();
    }

    public void updateOutput(BufferedImage output) {
        outputImage.setImage(output);
        try {
            ImageIO.write(output, "jpg", currentOutputFile);
        } catch (IOException e) {
            System.err.println("Could not write output image to file: " + e);
        }
        currentOutputFile = null;
        outputFileLabel.setText("No new destination selected...");
    }

    public void updateProgress(int progress) {
        progressBar.setValue(progress);
    }

    private void processPressed() {
        processButton.setEnabled(false);
        try {
            controller.sendImage(ImageIO.read(currentInputFile));
        } catch (IOException e) {
            System.err.println("Could not send image to server:" + e);
        }
    }

    private void updateOutputFile() {
        File currentValue = fileChooser.getSelectedFile();
        if (!currentValue.exists() && currentValue.getName().toUpperCase().endsWith(".JPG")) {
            currentOutputFile = currentValue;
            outputFileLabel.setText(currentOutputFile.getName());
        } else {
            currentOutputFile = null;
            outputFileLabel.setText("No new destination selected...");
        }
        tryEnableProcessButton();
    }

    private void updateInputFile() {
        File currentValue = fileChooser.getSelectedFile();
        if (currentValue.exists() && currentValue.getName().toUpperCase().endsWith(".JPG")) {
            currentInputFile = currentValue;
            inputFileLabel.setText(currentInputFile.getName());
            try {
                inputImage.setImage(ImageIO.read(currentInputFile));
            } catch (IOException e) {
                currentInputFile = null;
                System.err.println("Could not read image:" + e);
            }
        } else {
            currentInputFile = null;
            inputFileLabel.setText("No .jpg file selected...");
        }
        tryEnableProcessButton();
    }

    private void tryEnableProcessButton() {
        if (currentInputFile != null && currentOutputFile != null) {
            processButton.setEnabled(true);
        } else {
            processButton.setEnabled(false);
        }
    }

    public void display() {
        JFrame frame = new JFrame("Image Thing Doer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.setMinimumSize(new Dimension(360, 360));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addListeners() {
        chooseInputFileButton.addActionListener((ActionEvent evt) -> {
            fileChooser.setCurrentDirectory(new File("./files"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                updateInputFile();
            }
        });
        chooseOutputFileButton.addActionListener((ActionEvent evt) -> {
            fileChooser.setCurrentDirectory(new File("./files"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                updateOutputFile();
            }
        });
        processButton.addActionListener((ActionEvent evt) -> {
            processPressed();
        });
    }

    private void initComponents() {
        JPanel stuffHolder = new JPanel(new BorderLayout());
        // init fileChooser:
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("*.jpg", "jpg"));
        // init chooseInputFileButton, inputFileLabel:
        JPanel inputFileHolder = new JPanel(new BorderLayout());
        inputFileHolder.setBorder(new TitledBorder("Input image (.jpg):"));
        chooseInputFileButton = new JButton("Open Image");
        inputFileHolder.add(chooseInputFileButton, BorderLayout.WEST);
        inputFileLabel = new JLabel("No image selected...");
        inputFileLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        inputFileHolder.add(inputFileLabel, BorderLayout.CENTER);
        stuffHolder.add(inputFileHolder, BorderLayout.NORTH);
        // init chooseOutputFileButton, outputFileLabel:
        JPanel outputFileHolder = new JPanel(new BorderLayout());
        outputFileHolder.setBorder(new TitledBorder("Output location (.jpg):"));
        chooseOutputFileButton = new JButton("Set Location");
        outputFileHolder.add(chooseOutputFileButton, BorderLayout.WEST);
        outputFileLabel = new JLabel("No location selected...");
        outputFileLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        outputFileHolder.add(outputFileLabel, BorderLayout.CENTER);
        stuffHolder.add(outputFileHolder, BorderLayout.CENTER);
        // init processButton:
        processButton = new JButton("Process");
        processButton.setEnabled(false);
        stuffHolder.add(processButton, BorderLayout.EAST);
        // init progressBar:
        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setBorder(new TitledBorder("Server progress:"));
        stuffHolder.add(progressBar, BorderLayout.SOUTH);
        // add stuffHolder to view:
        super.add(stuffHolder, BorderLayout.NORTH);
        // init inputImage, outputImage:
        inputImage = new ImagePanel();
        inputImage.setPreferredSize(new Dimension(320, 180));
        super.add(inputImage, BorderLayout.WEST);
        outputImage = new ImagePanel();
        outputImage.setPreferredSize(new Dimension(320, 180));
        super.add(outputImage, BorderLayout.EAST);
    }

    private class ImagePanel extends JPanel {

        private Image image = null;

        public void setImage(BufferedImage image) {
            int scaledWidth = 320;
            int scaledHeight = (scaledWidth * image.getHeight()) / image.getWidth();
            this.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
            this.image = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_FAST);
            this.repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, this);
            } else { // show "proxy"
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, getPreferredSize().width, getPreferredSize().height);
            }
        }
    }
}
