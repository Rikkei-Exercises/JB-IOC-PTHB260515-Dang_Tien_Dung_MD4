package com.example.student.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "enrolled_courses", columnDefinition = "bigint[]", nullable = false)
    private List<Long> enrolledCourses = new ArrayList<>();

    public Student(String name, String email) {
        this.name = name;
        this.email = email;
        this.enrolledCourses = new ArrayList<>();
    }

    public List<Long> getEnrolledCourses() {
        if (this.enrolledCourses == null) {
            this.enrolledCourses = new ArrayList<>();
        }
        return this.enrolledCourses;
    }

    @PrePersist
    @PreUpdate
    private void ensureEnrolledCoursesNotNull() {
        if (this.enrolledCourses == null) {
            this.enrolledCourses = new ArrayList<>();
        }
    }
}
