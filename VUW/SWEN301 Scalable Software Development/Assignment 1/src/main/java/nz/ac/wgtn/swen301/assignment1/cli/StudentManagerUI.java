package nz.ac.wgtn.swen301.assignment1.cli;

import nz.ac.wgtn.swen301.assignment1.StudentManager;
import nz.ac.wgtn.swen301.studentdb.Student;
import nz.ac.wgtn.swen301.studentdb.NoSuchRecordException;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.Collection;

public class StudentManagerUI {

    // THE FOLLOWING METHOD MUST BE IMPLEMENTED

    /**
     * Executable: the user will provide argument(s) and print details to the console as described in the assignment brief,
     * E.g. a user could invoke this by running "java -cp <someclasspath> <arguments></arguments>"
     *
     * @param args The command-line arguments, which may include options for
     *             fetching one student, fetching all students, or exporting all students to a CSV file.
     */
    public static void main(String[] args) {
        // Define command-line options:
        Options options = new Options();
        Option fetchOneOption = new Option("fetchone", true, "fetch the student record with id");
        fetchOneOption.setArgs(1);
        options.addOption(fetchOneOption);
        options.addOption("fetchall", false, "fetch all student records");
        Option exportOption = new Option("export", false, "export all student records to a file");
        options.addOption(exportOption);
        Option fileOption = new Option("f", true, "file name for export");
        fileOption.setArgs(1);
        options.addOption(fileOption);

        // Parse command-line arguments:
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("fetchone")) {
                String id = cmd.getOptionValue("fetchone");
                Student student = StudentManager.fetchStudent(id);
                System.out.println(student.getId() + ", " + student.getFirstName() + ", " + student.getName());
            } else if (cmd.hasOption("fetchall")) {
                Collection<String> allIds = StudentManager.fetchAllStudentIds();
                for (String id : allIds) {
                    Student student = StudentManager.fetchStudent(id);
                    System.out.print("(" + student.getId() + ", " + student.getFirstName() + ", " + student.getName() + "), ");
                }
                System.out.println();
            } else if (cmd.hasOption("export")) {
                if (!cmd.hasOption("f")) {
                    System.err.println("File name must be specified with -f option when using -export");
                    return;
                }
                String fileName = cmd.getOptionValue("f");
                // Write CSV file assuming no student properties contain commas, newlines, or double quotes
                try (PrintWriter writer = new PrintWriter(fileName)) {
                    writer.println("id,first_name,name,degree");
                    Collection<String> allIds = StudentManager.fetchAllStudentIds();
                    for (String id : allIds) {
                        Student student = StudentManager.fetchStudent(id);
                        writer.println(student.getId() + "," + student.getFirstName() + "," + student.getName() + "," + student.getDegree().getId());
                    }
                } catch (FileNotFoundException e) {
                    System.err.println("File not found: " + e.getMessage());
                }
            }
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
        } catch (NoSuchRecordException e) {
            System.err.println("No such record found: " + e.getMessage());
        }
    }
}
