package com.liveclass.course.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveclass.course.domain.common.DomainErrorCode;
import com.liveclass.course.domain.common.DomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private Payment.PaymentBuilder baseBuilder() {
        return Payment.builder()
                .amount(new BigDecimal("49000"))
                .tid("nicepay_test_tid")
                .method(PaymentMethod.CARD);
    }

    @Test
    void 기본_상태는_READY다() {
        Payment p = baseBuilder().build();

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.READY);
    }

    @Test
    void READY에서_IN_PROGRESS_전환_authToken_저장() {
        Payment p = baseBuilder().build();

        p.markInProgress("auth_token_123");

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
        assertThat(p.getAuthToken()).isEqualTo("auth_token_123");
    }

    @Test
    void READY가_아니면_IN_PROGRESS_전환_불가() {
        Payment p = baseBuilder().status(PaymentStatus.IN_PROGRESS).build();

        assertThatThrownBy(() -> p.markInProgress("token"))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.INVALID_PAYMENT_STATUS);
    }

    @Test
    void IN_PROGRESS에서_approve로_PAID_전환_approvedAt_설정() {
        Payment p = baseBuilder().status(PaymentStatus.IN_PROGRESS).build();

        p.approve();

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(p.getApprovedAt()).isNotNull();
    }

    @Test
    void READY에서_바로_approve_불가() {
        Payment p = baseBuilder().build();

        assertThatThrownBy(p::approve)
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.INVALID_PAYMENT_STATUS);
    }

    @Test
    void PAID는_재approve_불가() {
        Payment p = baseBuilder().status(PaymentStatus.PAID).build();

        assertThatThrownBy(p::approve)
                .isInstanceOf(DomainException.class);
    }

    @Test
    void fail은_사유_저장하고_FAILED_전환() {
        Payment p = baseBuilder().status(PaymentStatus.IN_PROGRESS).build();

        p.fail("카드 한도 초과");

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(p.getFailedReason()).isEqualTo("카드 한도 초과");
    }

    @Test
    void PAID는_fail_불가() {
        Payment p = baseBuilder().status(PaymentStatus.PAID).build();

        assertThatThrownBy(() -> p.fail("불가"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void READY는_cancel_가능() {
        Payment p = baseBuilder().build();

        p.cancel();

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void CANCELLED_재취소_불가() {
        Payment p = baseBuilder().status(PaymentStatus.CANCELLED).build();

        assertThatThrownBy(p::cancel)
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.PAYMENT_ALREADY_CANCELLED);
    }

    @Test
    void PAID는_환불_절차_필요_cancel_불가() {
        Payment p = baseBuilder().status(PaymentStatus.PAID).build();

        assertThatThrownBy(p::cancel)
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.INVALID_PAYMENT_STATUS);
    }
}
