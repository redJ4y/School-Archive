package Q3;

// @author Jared Scholz
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

public class DocumentReader {

    public static void main(String[] args) {
        List<String> uniqueWords = new SelfOrganizingArrayList<>();
        File targetFile = new File("./misc/DocumentReader/text.txt");

        try {
            Scanner scanner = new Scanner(new BufferedInputStream(new FileInputStream(targetFile)));
            while (scanner.hasNext()) {
                String currentWord = scanner.next().toLowerCase().replaceAll("[^a-z'-]", "");
                if (currentWord.length() > 0 && !uniqueWords.contains(currentWord)) {
                    uniqueWords.add(currentWord);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File \"./misc/DocumentReader/text.txt\" not found!");
            System.exit(0);
        }

        System.out.println("Unique words with frequencies in text:");
        System.out.println(uniqueWords);

        System.out.println("\nTesting removes:");
        System.out.println("(Removing \"the\" and \"self-organizing\")");
        uniqueWords.remove("the");
        uniqueWords.remove("self-organizing");
        System.out.println(uniqueWords);

        System.out.println("\nTesting ListIterator:");
        // ListIterator extends Iterator, so this tests both
        ListIterator iterator = uniqueWords.listIterator();
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        System.out.println();

        System.out.println("\nTesting ListIterator in reverse:");
        while (iterator.hasPrevious()) {
            System.out.print(iterator.previous() + " ");
        }
        System.out.println();

        System.out.println("\nTesting retainAll/subList:");
        System.out.println("(Retaining first 6 words)");
        // note that subList does not maintain access counters
        uniqueWords.retainAll(uniqueWords.subList(0, 5));
        System.out.println(uniqueWords);
    }
}
