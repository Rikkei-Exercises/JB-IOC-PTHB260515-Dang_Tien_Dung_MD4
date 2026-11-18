package com.example.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Modifying
    @Query("update Course c set c.capacity = c.capacity - 1 where c.id = :courseId and c.capacity > 0")
    int reserveSeat(@Param("courseId") Long courseId);

    @Modifying
    @Query("update Course c set c.capacity = c.capacity + 1 where c.id = :courseId")
    int releaseSeat(@Param("courseId") Long courseId);
}
