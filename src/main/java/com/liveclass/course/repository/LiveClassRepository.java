package com.liveclass.course.repository;

import com.liveclass.course.domain.liveclass.LiveClass;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassRepository extends JpaRepository<LiveClass, Long> {

    @Query("select c from LiveClass c join fetch c.creator where c.id = :id")
    Optional<LiveClass> findByIdWithCreator(@Param("id") Long id);
}
