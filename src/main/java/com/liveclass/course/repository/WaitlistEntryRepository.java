package com.liveclass.course.repository;

import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

}
