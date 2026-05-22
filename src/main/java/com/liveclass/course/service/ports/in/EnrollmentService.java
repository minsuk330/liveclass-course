package com.liveclass.course.service.ports.in;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;

public interface EnrollmentService {

    Enrollment create(CreateEnrollmentCommand command);
}
