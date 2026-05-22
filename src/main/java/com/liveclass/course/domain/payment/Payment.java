package com.liveclass.course.domain.payment;

import com.liveclass.course.domain.common.BaseEntity;
import com.liveclass.course.domain.common.DomainErrorCode;
import com.liveclass.course.domain.common.DomainException;
import com.liveclass.course.domain.enrollment.Enrollment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Payment extends BaseEntity {

    @JoinColumn(name = "enrollment_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Enrollment enrollment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.READY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.CARD;

    @Column(nullable = false, length = 100, unique = true)
    private String tid;

    @Column(name = "auth_token", length = 200)
    private String authToken;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "failed_reason", length = 500)
    private String failedReason;

    public void markInProgress(String authToken) {
        if (this.status != PaymentStatus.READY) {
            throw new DomainException(
                    DomainErrorCode.INVALID_PAYMENT_STATUS,
                    "READY 상태에서만 IN_PROGRESS 전환 가능: " + this.status);
        }
        this.status = PaymentStatus.IN_PROGRESS;
        this.authToken = authToken;
    }

    public void approve() {
        if (this.status != PaymentStatus.IN_PROGRESS) {
            throw new DomainException(
                    DomainErrorCode.INVALID_PAYMENT_STATUS,
                    "IN_PROGRESS 상태에서만 승인 가능: " + this.status);
        }
        this.status = PaymentStatus.PAID;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (this.status == PaymentStatus.PAID || this.status == PaymentStatus.CANCELLED) {
            throw new DomainException(
                    DomainErrorCode.INVALID_PAYMENT_STATUS,
                    "PAID/CANCELLED 상태에서 실패 처리 불가: " + this.status);
        }
        this.status = PaymentStatus.FAILED;
        this.failedReason = reason;
    }

    public void cancel() {
        if (this.status == PaymentStatus.CANCELLED) {
            throw new DomainException(DomainErrorCode.PAYMENT_ALREADY_CANCELLED);
        }
        if (this.status == PaymentStatus.PAID) {
            throw new DomainException(
                    DomainErrorCode.INVALID_PAYMENT_STATUS,
                    "PAID 상태는 환불 절차 필요 (취소 불가)");
        }
        this.status = PaymentStatus.CANCELLED;
    }
}
