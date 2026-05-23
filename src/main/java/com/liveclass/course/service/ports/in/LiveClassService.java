package com.liveclass.course.service.ports.in;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassEnrollmentsCommand;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassesCommand;
import com.liveclass.course.service.ports.in.result.liveclass.ClassDetail;
import com.liveclass.course.service.ports.in.result.liveclass.ClassListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiveClassService {

    LiveClass create(CreateClassCommand command);

    ClassDetail get(Long classId);

    Page<ClassListItem> search(SearchClassesCommand command, Pageable pageable);

    void changeStatus(ChangeClassStatusCommand command);

    Page<Enrollment> searchEnrollments(SearchClassEnrollmentsCommand command, Pageable pageable);

    void delete(Long classId, Long creatorId);
}
