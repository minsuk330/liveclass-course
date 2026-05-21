package com.liveclass.course.service.ports.in;

import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.result.liveclass.ClassDetail;

public interface LiveClassService {

    LiveClass create(CreateClassCommand command);

    ClassDetail get(Long classId);
}
