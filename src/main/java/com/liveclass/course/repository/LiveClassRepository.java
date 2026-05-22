package com.liveclass.course.repository;

import com.liveclass.course.domain.liveclass.LiveClass;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassRepository
        extends JpaRepository<LiveClass, Long>, LiveClassRepositoryCustom {

    @Query("select c from LiveClass c join fetch c.creator where c.id = :id")
    Optional<LiveClass> findByIdWithCreator(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LiveClass c where c.id = :id")
    Optional<LiveClass> findByIdForUpdate(@Param("id") Long id);
}
