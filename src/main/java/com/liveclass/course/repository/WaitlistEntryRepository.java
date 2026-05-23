package com.liveclass.course.repository;

import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
          update WaitlistEntry w
          set w.deletedAt = :now,
              w.activeUserId = null
          where w.liveClass.id = :classId
            and w.deletedAt is null
          """)
    int softDeleteByLiveClassId(@Param("classId") Long classId, @Param("now") LocalDateTime now);

    boolean existsByLiveClass_IdAndUser_Id(Long liveClassId, Long userId);

    long countByLiveClass_Id(Long liveClassId);

    @Query(value = """
            select coalesce(max(position), 0)
            from waitlist_entry
            where class_id = :classId
            """, nativeQuery = true)
    int findMaxPositionByLiveClassId(@Param("classId") Long classId);

    Optional<WaitlistEntry> findFirstByLiveClass_IdOrderByPositionAsc(Long liveClassId);

    @Query("""
            select w
            from WaitlistEntry w
            join fetch w.liveClass
            where w.user.id = :userId
            order by w.createdAt asc
            """)
    Page<WaitlistEntry> findByUserIdWithClass(@Param("userId") Long userId, Pageable pageable);
}
