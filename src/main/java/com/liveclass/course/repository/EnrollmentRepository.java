package com.liveclass.course.repository;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    long countByLiveClass_IdAndStatusIn(Long liveClassId, Collection<EnrollmentStatus> statuses);

    @Query("""
            select e.liveClass.id, count(e)
            from Enrollment e
            where e.liveClass.id in :classIds
              and e.status in :statuses
            group by e.liveClass.id
            """)
    List<Object[]> countActiveByLiveClassIds(
            @Param("classIds") List<Long> classIds,
            @Param("statuses") Collection<EnrollmentStatus> statuses);
}
