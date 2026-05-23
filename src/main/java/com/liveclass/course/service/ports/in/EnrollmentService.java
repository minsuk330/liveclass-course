package com.liveclass.course.service.ports.in;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.enrollment.SearchMyEnrollmentsCommand;
import com.liveclass.course.service.ports.in.result.enrollment.MyEnrollmentListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService {

    Enrollment create(CreateEnrollmentCommand command);

    Page<MyEnrollmentListItem> searchMyEnrollments(SearchMyEnrollmentsCommand command, Pageable pageable);
}
