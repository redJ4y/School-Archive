package nz.ac.wgtn.swen301.assignment1;

import nz.ac.wgtn.swen301.studentdb.Student;
import nz.ac.wgtn.swen301.studentdb.Degree;
import nz.ac.wgtn.swen301.studentdb.StudentDB;
import nz.ac.wgtn.swen301.studentdb.NoSuchRecordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for StudentManager, to be extended.
 */
public class TestStudentManager {

    // DO NOT REMOVE THE FOLLOWING -- THIS WILL ENSURE THAT THE DATABASE IS AVAILABLE
    // AND IN ITS INITIAL STATE BEFORE EACH TEST RUNS
    @BeforeEach
    public void init() {
        StudentDB.init();
        StudentManager.reset();
    }
    // DO NOT REMOVE BLOCK ENDS HERE

    @Test
    public void dummyTest() throws Exception {
        Student student = StudentManager.fetchStudent("id42");
        // THIS WILL INITIALLY FAIL !!
        assertNotNull(student);
    }

    @Test
    public void testFetchStudent1() throws Exception {
        String id = "id0";
        Student student = StudentManager.fetchStudent(id);
        assertEquals(id, student.getId());
        assertEquals("James", student.getFirstName());
        assertEquals("Smith", student.getName());
        assertEquals("deg0", student.getDegree().getId());
    }

    @Test
    public void testFetchStudent2() throws Exception {
        String id = "id1";
        Student student1 = StudentManager.fetchStudent(id);
        Student student2 = StudentManager.fetchStudent(id);
        // Verifying that the same instance is returned
        assertSame(student1, student2);
    }

    @Test
    public void testFetchStudent3() {
        // Testing a non-existent student ID
        String id = "id10000";
        assertThrows(NoSuchRecordException.class, () -> StudentManager.fetchStudent(id));
    }

    @Test
    public void testFetchDegree1() throws NoSuchRecordException {
        String degreeId = "deg0";
        Degree degree = StudentManager.fetchDegree(degreeId);
        assertEquals(degreeId, degree.getId());
        assertEquals("BSc Computer Science", degree.getName());
    }

    @Test
    public void testFetchDegree2() throws NoSuchRecordException {
        String degreeId = "deg5";
        Degree degree1 = StudentManager.fetchDegree(degreeId);
        Degree degree2 = StudentManager.fetchDegree(degreeId);
        // Verifying that the same instance is returned
        assertSame(degree1, degree2);
    }

    @Test
    public void testFetchDegree3() {
        // Testing a non-existent degree ID
        String degreeId = "deg999";
        assertThrows(NoSuchRecordException.class, () -> StudentManager.fetchDegree(degreeId));
    }

    @Test
    public void testRemove() throws NoSuchRecordException {
        // Just one test for this because comment did not say "followed by optional numbers if multiple tests are used"
        String id = "id420";
        Student studentToRemove = StudentManager.fetchStudent(id);
        StudentManager.remove(studentToRemove);
        assertThrows(NoSuchRecordException.class, () -> StudentManager.fetchStudent(id), "Student should not be found after removal");
        // Testing removing a Student that does not exist in the database
        Student nonExistentStudent = new Student("id10000", "NonExistent", "Student", new Degree("deg0", "NonExistent"));
        assertThrows(NoSuchRecordException.class, () -> StudentManager.remove(nonExistentStudent), "Should throw exception for non-existent student");
    }

    @Test
    public void testUpdate1() throws Exception {
        String id = "id69";
        String newFirstName = "Updated";
        String newName = "Name";
        String newDegreeId = "deg9";
        Student student = StudentManager.fetchStudent(id);
        student.setFirstName(newFirstName);
        student.setName(newName);
        student.setDegree(StudentManager.fetchDegree(newDegreeId));
        StudentManager.update(student);
        Student updatedStudent = StudentManager.fetchStudent(id);
        assertEquals(newFirstName, updatedStudent.getFirstName());
        assertEquals(newName, updatedStudent.getName());
        assertEquals(newDegreeId, updatedStudent.getDegree().getId());
    }

    @Test
    public void testUpdate2() {
        Student student = new Student("id10000", "NonExistent", "Student", new Degree("deg0", "NonExistent"));
        // Testing updating a Student that does not exist in the database
        assertThrows(NoSuchRecordException.class, () -> StudentManager.update(student));
    }

    @Test
    public void testNewStudent1() throws Exception {
        String lastName = "Doe";
        String firstName = "John";
        String degreeId = "deg3";
        Student student = StudentManager.newStudent(lastName, firstName, StudentManager.fetchDegree(degreeId));
        assertEquals(lastName, student.getName());
        assertEquals(firstName, student.getFirstName());
        assertEquals(degreeId, student.getDegree().getId());
        Student fetchedStudent = StudentManager.fetchStudent(student.getId());
        assertSame(student, fetchedStudent);
    }

    @Test
    public void testNewStudent2() throws Exception {
        String lastName = "Sara";
        String firstName = "Lee";
        Degree degree = StudentManager.fetchDegree("deg5");
        Student student1 = StudentManager.newStudent(lastName, firstName, degree);
        Student student2 = StudentManager.newStudent(lastName, firstName, degree);
        // Verifying that two different instances with unique IDs are created
        assertNotEquals(student1.getId(), student2.getId());
    }

    @Test
    public void testFetchAllStudentIds() {
        int expectedNumIds = 10000;
        Collection<String> studentIds = StudentManager.fetchAllStudentIds();
        assertEquals(expectedNumIds, studentIds.size());
        for (int i = 0; i < expectedNumIds; i++) {
            assertTrue(studentIds.contains("id" + i));
        }
    }

    @Test
    public void testPerformance() throws NoSuchRecordException {
        int numQueries = 10000;
        Random random = new Random();
        List<String> studentIdList = new ArrayList<>(StudentManager.fetchAllStudentIds());
        int numStudents = studentIdList.size();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numQueries; i++) {
            StudentManager.fetchStudent(studentIdList.get(random.nextInt(numStudents)));
        }
        long endTime = System.currentTimeMillis();

        double totalTimeInSeconds = (endTime - startTime) / 1000.0;
        double randomQueriesPerSecond = numQueries / totalTimeInSeconds;
        assertTrue(randomQueriesPerSecond >= 500.0, "Performance requirement not met (at "
                + randomQueriesPerSecond + " random queries per second)");
    }
}
