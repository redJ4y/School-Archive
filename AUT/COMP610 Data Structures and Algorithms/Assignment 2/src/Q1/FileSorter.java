package Q1;

// @author Jared Scholz
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FileSorter implements Runnable {

    private final int limit; // maximum number of strings in memory at a time
    private final File input;
    private final File output;
    private final Queue<File> tempFiles;
    private final String workingPath; // where to create temp files
    /* Progress tracking variables below */
    private final Object linesSplitLock = new Object();
    private final Object mergesDoneLock = new Object();
    private int numLinesSplit;
    private int numMergesDone;
    private int numLinesToDo;
    private int numMergesToDo;

    public FileSorter(int limit, File input, File output) {
        this.limit = limit;
        this.input = input;
        this.output = output;
        tempFiles = new LinkedList<>();
        // determine the working path (including new temp directory):
        if (output.getParent() != null) {
            workingPath = output.getParent() + "/file_sorter_temp";
        } else {
            workingPath = "file_sorter_temp";
        }
        // initialize progress tracking:
        numLinesSplit = 0;
        numMergesDone = 0;
        // allow for immediate tracking (0/1)...
        numLinesToDo = 1;
        numMergesToDo = 1;
    }

    private void doSplitStage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            String currentLine = reader.readLine(); // fake previous iteration
            while (currentLine != null) {
                List<String> segment = new ArrayList<>(limit);
                while (segment.size() < limit && currentLine != null) {
                    // uses the last currentLine from the previous iteration
                    segment.add(currentLine);
                    currentLine = reader.readLine();
                }
                // segment always has at least one line
                Collections.sort(segment);
                createTempFile(segment);
                // update progress tracking:
                synchronized (linesSplitLock) {
                    numLinesSplit += segment.size();
                }
            }
            reader.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void createTempFile(List<String> segment) {
        File tempFile = new File(workingPath + "/temp_" + tempFiles.size() + ".txt");
        try {
            tempFile.createNewFile();
            tempFiles.add(tempFile); // add to the queue
            PrintWriter writer = new PrintWriter(new FileOutputStream(tempFile));
            for (String currentLine : segment) {
                writer.println(currentLine);
            }
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void doMergeStage(int iteration) { // iteration parameter used in file naming
        if (tempFiles.size() > 2) {
            int numMerges = tempFiles.size() / 2;
            // loop exactly numMerges times (keep using the same queue):
            for (int i = 0; i < numMerges; i++) {
                File newTempFile = new File(workingPath + "/temp_" + iteration + "_" + i + ".txt");
                tempFiles.add(newTempFile); // add to the back of the queue to be merged again in the next iteration
                // merge two temp files into newTempFile and delete them:
                mergeTwoFiles(newTempFile, tempFiles.remove(), tempFiles.remove());
                // update progress tracking:
                synchronized (mergesDoneLock) {
                    numMergesDone++;
                }
            }
            // if there is a leftover file, it will be merged in the next iteration
            doMergeStage(iteration + 1); // do the next iteration: merge the merged files
        } else { // final iteration:
            mergeTwoFiles(output, tempFiles.remove(), tempFiles.remove());
            // update progress tracking:
            synchronized (mergesDoneLock) {
                numMergesDone++;
            }
        }
    }

    /* Creates the destination file, merges two files into it, then deletes used files */
    private void mergeTwoFiles(File destination, File first, File second) {
        try {
            destination.createNewFile();
            PrintWriter writer = new PrintWriter(new FileOutputStream(destination));
            BufferedReader firstReader = new BufferedReader(new FileReader(first));
            BufferedReader secondReader = new BufferedReader(new FileReader(second));

            String firstCurrent = firstReader.readLine();
            String secondCurrent = secondReader.readLine();
            do {
                if (firstCurrent.compareTo(secondCurrent) < 0) {
                    writer.println(firstCurrent);
                    firstCurrent = firstReader.readLine();
                } else {
                    writer.println(secondCurrent);
                    secondCurrent = secondReader.readLine();
                }
            } while (secondCurrent != null && firstCurrent != null);

            if (firstCurrent != null) {
                // add the remaining lines from firstCurrent:
                do {
                    writer.println(firstCurrent);
                } while ((firstCurrent = firstReader.readLine()) != null);
            } else if (secondCurrent != null) {
                // add the remaining lines from secondCurrent:
                do {
                    writer.println(secondCurrent);
                } while ((secondCurrent = secondReader.readLine()) != null);
            }

            writer.flush();
            writer.close();
            firstReader.close();
            secondReader.close();
            // delete used files:
            first.delete();
            second.delete();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void run() {
        File workingDirectory = new File(workingPath);
        workingDirectory.mkdir(); // prepare working area
        synchronized (linesSplitLock) {
            initNumLinesToDo(); // prepare progress tracking divisor (not on GUI thread)
        }
        doSplitStage();
        synchronized (mergesDoneLock) {
            numMergesToDo = tempFiles.size() - 1; // prepare progress tracking divisor
        }
        doMergeStage(0);
        workingDirectory.delete();
    }

    /* Calculates numLinesToDo from the input file */
    private void initNumLinesToDo() {
        numLinesToDo = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            while (reader.readLine() != null) {
                numLinesToDo++;
            }
            reader.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public int getSplitPercent() {
        synchronized (linesSplitLock) {
            return (100 * numLinesSplit) / numLinesToDo;
        }
    }

    public int getMergePercent() {
        synchronized (mergesDoneLock) {
            return (100 * numMergesDone) / numMergesToDo;
        }
    }
}
