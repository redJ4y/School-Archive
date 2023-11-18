package nz.ac.wgtn.swen301.assignment1;

import nz.ac.wgtn.swen301.studentdb.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A student manager providing basic CRUD operations for instances of Student, and a read operation for instances of Degree.
 *
 * @author jens dietrich, jared scholz
 */
public class StudentManager {

    // DO NOT REMOVE THE FOLLOWING -- THIS WILL ENSURE THAT THE DATABASE IS AVAILABLE
    // AND THE APPLICATION CAN CONNECT TO IT WITH JDBC
    static {
        StudentDB.init();
    }
    // DO NOT REMOVE BLOCK ENDS HERE

    private static final String JDBC_URL = "jdbc:derby:memory:studentdb";
    private static final Map<String, Student> studentCache = new HashMap<>();
    private static final Map<String, Degree> degreeCache = new HashMap<>();

    /**
     * Package-private utility method used in test fixtures to clear memory resources such as caches between tests.
     */
    static void reset() {
        studentCache.clear();
        degreeCache.clear();
    }

    // THE FOLLOWING METHODS MUST BE IMPLEMENTED :

    /**
     * Return a student instance with values from the row with the respective id in the database.
     * If an instance with this id already exists, return the existing instance and do not create a second one.
     * This functionality is to be tested in nz.ac.wgtn.swen301.assignment1.TestStudentManager::testFetchStudent (followed by optional numbers if multiple tests are used).
     *
     * @param id An id used to identify a Student.
     * @return A Student instance with the given id.
     * @throws NoSuchRecordException if no record with such an id exists in the database.
     */
    public static Student fetchStudent(String id) throws NoSuchRecordException {
        Student existingStudent = studentCache.get(id);
        if (existingStudent != null) {
            return existingStudent;
        }

        String query = "SELECT s.id, s.first_name, s.name, s.degree, d.name AS degree_name FROM STUDENTS s JOIN DEGREES d ON s.degree = d.id WHERE s.id = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String studentId = resultSet.getString("id");
                    String firstName = resultSet.getString("first_name");
                    String name = resultSet.getString("name");
                    String degreeId = resultSet.getString("degree");
                    String degreeName = resultSet.getString("degree_name");

                    Degree degree = degreeCache.get(degreeId);
                    if (degree == null) {
                        degree = new Degree(degreeId, degreeName);
                        degreeCache.put(degreeId, degree);
                    }

                    Student student = new Student(studentId, name, firstName, degree);
                    studentCache.put(studentId, student);
                    return student;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching student", e);
        }
        throw new NoSuchRecordException("No student found with id " + id);
    }

    /**
     * Return a degree instance with values from the row with the respective id in the database.
     * If an instance with this id already exists, return the existing instance and do not create a second one.
     * This functionality is to be tested in nz.ac.wgtn.swen301.assignment1.TestStudentManager::testFetchDegree (followed by optional numbers if multiple tests are used).
     *
     * @param id An id used to identify a Degree.
     * @return A Degree instance with the given id.
     * @throws NoSuchRecordException if no record with such an id exists in the database.
     */
    public static Degree fetchDegree(String id) throws NoSuchRecordException {
        Degree existingDegree = degreeCache.get(id);
        if (existingDegree != null) {
            return existingDegree;
        }

        String query = "SELECT d.id, d.name FROM DEGREES d WHERE d.id = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String degreeId = resultSet.getString("id");
                    String degreeName = resultSet.getString("name");

                    Degree degree = new Degree(degreeId, degreeName);
                    degreeCache.put(degreeId, degree);
                    return degree;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching degree", e);
        }
        throw new NoSuchRecordException("No degree found with id " + id);
    }

    /**
     * Delete a student instance from the database.
     * I.e., after this, trying to read a student with this id will result in a NoSuchRecordException.
     * This functionality is to be tested in nz.ac.wgtn.swen301.assignment1.TestStudentManager::testRemove
     *
     * @param student An existing Student instance to delete from the database.
     * @throws NoSuchRecordException if no record corresponding to this student instance exists in the database.
     */
    public static void remove(Student student) throws NoSuchRecordException {
        String query = "DELETE FROM STUDENTS WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, student.getId());
            if (statement.executeUpdate() == 0) {
                throw new NoSuchRecordException("No student found with id " + student.getId());
            }
            studentCache.remove(student.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting student", e);
        }
    }

    /**
     * Update (synchronize) a student instance with the database.
     * The id will not be changed, but the values for first names or degree in the database might be changed by this operation.
     * After executing this command, the attribute values of the object and the respective database value are consistent.
     * Note that names and first names can only be max 1o characters long.
     * There is no special handling required to enforce this, just ensure that tests only use values with < 10 characters.
     * This functionality is to be tested in nz.ac.wgtn.swen301.assignment1.TestStudentManager::testUpdate (followed by optional numbers if multiple tests are used).
     *
     * @param student An existing Student instance to update (synchronize) with the database.
     * @throws NoSuchRecordException if no record corresponding to this student instance exists in the database.
     */
    public static void update(Student student) throws NoSuchRecordException {
        String query = "UPDATE STUDENTS SET first_name = ?, name = ?, degree = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, student.getFirstName());
            statement.setString(2, student.getName());
            statement.setString(3, student.getDegree().getId());
            statement.setString(4, student.getId());
            if (statement.executeUpdate() == 0) {
                throw new NoSuchRecordException("No student found with id " + student.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating student", e);
        }
    }

    /**
     * Create a new student with the values provided, and save it to the database.
     * The student must have a new id that is not being used by any other Student instance or STUDENTS record (row).
     * Note that names and first names can only be max 1o characters long.
     * There is no special handling required to enforce this, just ensure that tests only use values with < 10 characters.
     * This functionality is to be tested in nz.ac.wgtn.swen301.assignment1.TestStudentManager::testNewStudent (followed by optional numbers if multiple tests are used).
     *
     * @param name      The last name of the new student.
     * @param firstName The first name of the new student.
     * @param degree    The Degree of the new student.
     * @return A freshly created student instance.
     */
    public static Student newStudent(String name, String firstName, Degree degree) {
        // Find the maximum student ID and increment by one:
        String maxIdQuery = "SELECT MAX(CAST(SUBSTR(id, 3) AS INT)) AS max_id FROM STUDENTS";
        int newIdValue = 0;
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(maxIdQuery);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                newIdValue = resultSet.getInt("max_id") + 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching max student id", e);
        }
        String newId = "id" + newIdValue;

        String insertQuery = "INSERT INTO STUDENTS (id, first_name, name, degree) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(insertQuery)) {
            statement.setString(1, newId);
            statement.setString(2, firstName);
            statement.setString(3, name);
            statement.setString(4, degree.getId());
            statement.executeUpdate();
            Student student = new Student(newId, name, firstName, degree);
            studentCache.put(newId, student);
            return student;
        } catch (SQLException e) {
            throw new RuntimeException("Error creating new student", e);
        }
    }

    /**
     * Get all student ids currently being used in the database.
     * This functionality is to be tested in nz.ac.wgtn.swen301.assignment1.TestStudentManager::testFetchAllStudentIds (followed by optional numbers if multiple tests are used).
     *
     * @return A Collection of student ids as Strings.
     */
    public static Collection<String> fetchAllStudentIds() {
        Collection<String> ids = new ArrayList<>();
        String query = "SELECT id FROM STUDENTS";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getString("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching student ids", e);
        }
        return ids;
    }
}
