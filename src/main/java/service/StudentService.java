package service;

import entity.Student;

public class StudentService {
    //Stub dữ liệu sinh viên
    protected Student student = new Student(
            "SV00001",
            "Nguyễn Tường Minh",
            20,
            8.5F,
            3,
            "Công nghệ thông tin"
    );

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
    public void updateStudent(Student newStudent){
        if (newStudent == null){
            throw new IllegalArgumentException("Đối tượng sinh viên không được rỗng");
        }
        if (newStudent.getId() == null || newStudent.getId().isEmpty()
        || newStudent.getName() == null || newStudent.getName().isEmpty()
        || newStudent.getMajor() == null || newStudent.getMajor().isEmpty()){
            throw new IllegalArgumentException("Các thuộc tính không được để trống");
        }
        if (newStudent.getAge() < 18){
            throw new IllegalArgumentException("Tuổi phải lớn hơn hoặc bằng 18.");
        }
        if (newStudent.getAvgMark() < 0 || newStudent.getAvgMark() > 10){
            throw new IllegalArgumentException("Điểm phải nằm trong phạm vi từ 0 đến 10.");
        }
        student.setId(newStudent.getId());
        student.setName(newStudent.getName());
        student.setMajor(newStudent.getMajor());
        student.setAge(newStudent.getAge());
        student.setAvgMark(newStudent.getAvgMark());
    }
}
