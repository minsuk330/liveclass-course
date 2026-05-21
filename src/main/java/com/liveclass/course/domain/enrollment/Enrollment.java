package com.liveclass.course.domain.enrollment;

import com.liveclass.course.domain.common.BaseEntity;
import com.liveclass.course.domain.common.DomainErrorCode;
import com.liveclass.course.domain.common.DomainException;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Enrollment extends BaseEntity {

    @JoinColumn(name = "class_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private LiveClass liveClass;

    @JoinColumn(name = "user_id",nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public void confirmPayment() {
        if (this.status != EnrollmentStatus.PENDING) {
            throw new DomainException(
                    DomainErrorCode.INVALID_ENROLLMENT_STATUS,
                    "Only PENDING can be confirmed: " + this.status);
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.paidAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == EnrollmentStatus.CANCELLED) {
            throw new DomainException(DomainErrorCode.ENROLLMENT_ALREADY_CANCELLED);
        }
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == EnrollmentStatus.PENDING || status == EnrollmentStatus.CONFIRMED;
    }
}
