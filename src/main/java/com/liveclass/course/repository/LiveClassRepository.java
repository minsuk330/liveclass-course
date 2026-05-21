package com.liveclass.course.repository;

import com.liveclass.course.domain.liveclass.LiveClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveClassRepository extends JpaRepository<LiveClass, Long> {

}
