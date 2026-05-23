package com.liveclass.course.repository;

import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update WaitlistEntry w
            set w.deletedAt = :now
            where w.liveClass.id = :classId
              and w.deletedAt is null
            """)
    int softDeleteByLiveClassId(@Param("classId") Long classId, @Param("now") LocalDateTime now);
}
