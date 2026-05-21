package com.liveclass.course.repository;

import com.liveclass.course.domain.enrollment.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {


}
