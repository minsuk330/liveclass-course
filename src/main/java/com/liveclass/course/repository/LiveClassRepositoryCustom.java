package com.liveclass.course.repository;

import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassesCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiveClassRepositoryCustom {

    Page<LiveClass> search(SearchClassesCommand cmd, Pageable pageable);
}
