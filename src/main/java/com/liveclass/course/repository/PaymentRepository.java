package com.liveclass.course.repository;

import com.liveclass.course.domain.payment.Payment;
import com.liveclass.course.domain.payment.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTid(String tid);

    boolean existsByEnrollment_IdAndStatusIn(
            Long enrollmentId, Collection<PaymentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
}
