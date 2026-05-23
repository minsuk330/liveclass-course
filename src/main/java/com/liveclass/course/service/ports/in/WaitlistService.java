package com.liveclass.course.service.ports.in;

import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import com.liveclass.course.service.ports.in.command.waitlist.CancelWaitlistCommand;
import com.liveclass.course.service.ports.in.command.waitlist.RegisterWaitlistCommand;
import com.liveclass.course.service.ports.in.result.waitlist.MyWaitlistListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WaitlistService {

    WaitlistEntry register(RegisterWaitlistCommand command);

    Page<MyWaitlistListItem> searchMyWaitlist(Long userId, Pageable pageable);

    void cancel(CancelWaitlistCommand command);
}
