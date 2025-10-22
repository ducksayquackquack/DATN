import entity.Student;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.StudentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StudentServiceTest {
    protected static Student student;
    private static StudentService studentService;

    @BeforeAll
    public static void setUp() {
        studentService = new StudentService();
    }
    @AfterAll
    public static void tearDown() {
        studentService = null;
    }
    //Normal: Update thành công
    @Test
    public void testUpdateAvgMark() {
        //Stub dữ liệu sinh viên mới
        student = new Student(
                "SV00001",
                "Nguyễn Tường Minh",
                20,
                7.5F,
                3,
                "Công nghệ thông tin"
        );
        studentService.updateStudent(student);
        assertEquals(7.5F, student.getAvgMark());
    }
    //Abnormal: Update với điểm < 0
    @Test
    public void testUpdateAvgMarkLessThan0() {
        //Stub dữ liệu sinh viên mới
        student = new Student(
                "SV00001",
                "Nguyễn Tường Minh",
                20,
                -1.0F,
                3,
                "Công nghệ thông tin"
        );
        Exception ex = assertThrows(IllegalArgumentException.class, () -> studentService.updateStudent(student));
        assertEquals("Điểm phải nằm trong phạm vi từ 0 đến 10.",ex.getMessage());
    }
    //Abnormal: Update với điểm < 0
    @Test
    public void testUpdateAvgMarkGreaterThan10() {
        //Stub dữ liệu sinh viên mới
        student = new Student(
                "SV00001",
                "Nguyễn Tường Minh",
                20,
                11.0F,
                3,
                "Công nghệ thông tin"
        );
        Exception ex = assertThrows(IllegalArgumentException.class, () -> studentService.updateStudent(student));
        assertEquals("Điểm phải nằm trong phạm vi từ 0 đến 10.",ex.getMessage());
    }
    @Test
    public void testUpdateAvgMarkEqual0() {
        //Stub dữ liệu sinh viên mới
        student = new Student(
                "SV00001",
                "Nguyễn Tường Minh",
                20,
                0.0F,
                3,
                "Công nghệ thông tin"
        );
        studentService.updateStudent(student);
        assertEquals(0.0F, student.getAvgMark());
    }
    @Test
    public void testUpdateAvgMarkEqual10() {
        //Stub dữ liệu sinh viên mới
        student = new Student(
                "SV00001",
                "Nguyễn Tường Minh",
                20,
                10.0F,
                3,
                "Công nghệ thông tin"
        );
        studentService.updateStudent(student);
        assertEquals(10.0F, student.getAvgMark());
    }
}
