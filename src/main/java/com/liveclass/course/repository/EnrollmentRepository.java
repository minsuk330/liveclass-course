package com.liveclass.course.repository;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    long countByLiveClass_IdAndStatusIn(Long liveClassId, Collection<EnrollmentStatus> statuses);
}
