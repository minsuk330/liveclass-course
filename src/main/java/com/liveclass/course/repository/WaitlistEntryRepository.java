package com.liveclass.course.repository;

import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {
}
