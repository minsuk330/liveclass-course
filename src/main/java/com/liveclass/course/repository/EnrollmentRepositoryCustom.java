package com.liveclass.course.repository;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.service.ports.in.command.enrollment.SearchMyEnrollmentsCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentRepositoryCustom {

    Page<Enrollment> searchByClass(Long classId, EnrollmentStatus status, Pageable pageable);

    Page<Enrollment> searchByUser(SearchMyEnrollmentsCommand cmd, Pageable pageable);
}
