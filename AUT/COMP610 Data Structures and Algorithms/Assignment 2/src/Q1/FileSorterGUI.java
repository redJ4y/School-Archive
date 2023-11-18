package Q1;

// @author Jared Scholz
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;

public class FileSorterGUI extends JPanel {

    private final Queue<FileSorter> tasks;
    private FileSorter runningTask;
    private int currentMaxStrings;
    private File currentInputFile;
    private File currentOutputFile;

    private JLabel numInQueueLabel;
    private JFormattedTextField maxStringsField;
    private JFileChooser fileChooser;
    private JButton chooseInputFile;
    private JButton chooseOutputFile;
    private JLabel inputFileLabel;
    private JLabel outputFileLabel;
    private JProgressBar mergeBar;
    private JProgressBar splitBar;
    private JButton processTask;
    private JButton enqueueTask;

    public FileSorterGUI() {
        super(new GridLayout(7, 1));
        super.setPreferredSize(new Dimension(640, 360));
        super.setBorder(new EmptyBorder(6, 6, 6, 6));
        initComponents();
        addListeners();

        tasks = new LinkedList<>();
        runningTask = null;
        currentMaxStrings = -1;
        currentInputFile = null;
        currentOutputFile = null;

        Timer progressTimer = new Timer(50, (ActionEvent evt) -> {
            updateProgress();
        });
        progressTimer.start();
    }

    private void updateProgress() {
        if (runningTask != null) {
            int mergePercent = runningTask.getMergePercent();
            mergeBar.setValue(mergePercent);
            splitBar.setValue(runningTask.getSplitPercent());
            if (mergePercent > 99) {
                runningTask = null;
                mergeBar.setValue(0);
                splitBar.setValue(0);
                if (!tasks.isEmpty()) {
                    processTask.setEnabled(true);
                }
            }
        }
    }

    private void updateMaxStringsField() {
        Object currentValue = maxStringsField.getValue();
        if (currentValue != null) {
            currentMaxStrings = ((Number) currentValue).intValue();
        } else {
            currentMaxStrings = -1;
        }
        tryEnableEnqueTaskButton();
    }

    private void updateInputFile() {
        File currentValue = fileChooser.getSelectedFile();
        if (currentValue.exists() && currentValue.getName().toUpperCase().endsWith(".TXT")) {
            currentInputFile = currentValue;
            inputFileLabel.setText(currentInputFile.getName());
        } else {
            currentInputFile = null;
            inputFileLabel.setText("No .txt file selected...");
        }
        tryEnableEnqueTaskButton();
    }

    private void updateOutputFile() {
        File currentValue = fileChooser.getSelectedFile();
        if (currentValue.getName().toUpperCase().endsWith(".TXT")) {
            currentOutputFile = currentValue;
            outputFileLabel.setText(currentOutputFile.getName());
        } else {
            currentOutputFile = null;
            outputFileLabel.setText("No .txt file selected...");
        }
        tryEnableEnqueTaskButton();
    }

    private void processTaskPressed() {
        processTask.setEnabled(false);
        FileSorter toDo = tasks.poll();
        if (toDo != null) {
            runningTask = toDo;
            Thread sorterThread = new Thread(runningTask);
            sorterThread.start();
        }
        numInQueueLabel.setText("Number of tasks in queue: " + tasks.size());
    }

    private void enqueueTaskPressed() {
        if (currentMaxStrings > 0 && currentInputFile != null && currentOutputFile != null) {
            tasks.add(new FileSorter(currentMaxStrings, currentInputFile, currentOutputFile));
            numInQueueLabel.setText("Number of tasks in queue: " + tasks.size());
            if (runningTask == null) {
                processTask.setEnabled(true);
            }
        }
    }

    private void tryEnableEnqueTaskButton() {
        if (currentMaxStrings > 0 && currentInputFile != null && currentOutputFile != null) {
            enqueueTask.setEnabled(true);
        } else {
            enqueueTask.setEnabled(false);
        }
    }

    private void addListeners() {
        maxStringsField.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            updateMaxStringsField();
        });
        chooseInputFile.addActionListener((ActionEvent evt) -> {
            fileChooser.setCurrentDirectory(new File("./files"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                updateInputFile();
            }
        });
        chooseOutputFile.addActionListener((ActionEvent evt) -> {
            fileChooser.setCurrentDirectory(new File("./files"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                updateOutputFile();
            }
        });
        processTask.addActionListener((ActionEvent evt) -> {
            processTaskPressed();
        });
        enqueueTask.addActionListener((ActionEvent evt) -> {
            enqueueTaskPressed();
        });
    }

    private void initComponents() {
        // init numInQueueLabel:
        numInQueueLabel = new JLabel("Number of tasks in queue: 0");
        super.add(numInQueueLabel);
        // init maxStringsField:
        NumberFormatter maxStringsFormat = new NumberFormatter(NumberFormat.getInstance());
        maxStringsFormat.setValueClass(Integer.class);
        maxStringsFormat.setMinimum(1);
        maxStringsFormat.setMaximum(1000000);
        maxStringsFormat.setCommitsOnValidEdit(true);
        maxStringsField = new JFormattedTextField(maxStringsFormat);
        maxStringsField.setBorder(new TitledBorder("Maximum number of strings in memory:"));
        super.add(maxStringsField);
        // init fileChooser:
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("*.txt", "txt"));
        // init chooseInputFile, inputFileLabel:
        JPanel inputFileHolder = new JPanel(new BorderLayout());
        inputFileHolder.setBorder(new TitledBorder("Input file (.txt):"));
        chooseInputFile = new JButton("Open File");
        inputFileHolder.add(chooseInputFile, BorderLayout.WEST);
        inputFileLabel = new JLabel("No file selected...");
        inputFileLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        inputFileHolder.add(inputFileLabel, BorderLayout.CENTER);
        super.add(inputFileHolder);
        // init chooseOutputFile, outputFileLabel:
        JPanel outputFileHolder = new JPanel(new BorderLayout());
        outputFileHolder.setBorder(new TitledBorder("Output file (.txt):"));
        chooseOutputFile = new JButton("Open File");
        outputFileHolder.add(chooseOutputFile, BorderLayout.WEST);
        outputFileLabel = new JLabel("No file selected...");
        outputFileLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        outputFileHolder.add(outputFileLabel, BorderLayout.CENTER);
        super.add(outputFileHolder);
        // init mergeBar:
        mergeBar = new JProgressBar();
        mergeBar.setValue(0);
        mergeBar.setBorder(new TitledBorder("Merge progress:"));
        super.add(mergeBar);
        // init splitBar:
        splitBar = new JProgressBar();
        splitBar.setValue(0);
        splitBar.setBorder(new TitledBorder("Split progress:"));
        super.add(splitBar);
        // init processTask, enqueueTask:
        JPanel taskButtonHolder = new JPanel();
        processTask = new JButton("Process Task");
        processTask.setEnabled(false);
        taskButtonHolder.add(processTask);
        enqueueTask = new JButton("Enqueue Task");
        enqueueTask.setEnabled(false);
        taskButtonHolder.add(enqueueTask);
        super.add(taskButtonHolder);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("File Sorter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new FileSorterGUI());
        frame.setMinimumSize(new Dimension(360, 360));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
