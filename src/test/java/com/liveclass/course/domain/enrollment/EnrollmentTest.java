package com.liveclass.course.domain.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveclass.course.domain.common.DomainErrorCode;
import com.liveclass.course.domain.common.DomainException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EnrollmentTest {

    @Test
    void 기본_상태_PENDING() {
        Enrollment e = Enrollment.builder().build();

        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    void PENDING_즉시_취소_가능() {
        Enrollment e = Enrollment.builder().build();

        e.cancel();

        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(e.getCancelledAt()).isNotNull();
    }

    @Test
    void CONFIRMED_paidAt_7일_이내_취소_가능() {
        Enrollment e = Enrollment.builder()
                .status(EnrollmentStatus.CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(3))
                .build();

        e.cancel();

        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void CONFIRMED_paidAt_7일_경계_정확히_7일_취소_가능() {
        Enrollment e = Enrollment.builder()
                .status(EnrollmentStatus.CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(7).plusSeconds(1))
                .build();

        e.cancel();

        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void CONFIRMED_paidAt_7일_초과_취소_불가() {
        Enrollment e = Enrollment.builder()
                .status(EnrollmentStatus.CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(8))
                .build();

        assertThatThrownBy(e::cancel)
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.ENROLLMENT_CANCELLATION_PERIOD_EXPIRED);
    }

    @Test
    void CANCELLED_재취소_불가() {
        Enrollment e = Enrollment.builder()
                .status(EnrollmentStatus.CANCELLED)
                .cancelledAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(e::cancel)
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.ENROLLMENT_ALREADY_CANCELLED);
    }

    @Test
    void PENDING에서_confirmPayment_CONFIRMED_paidAt_설정() {
        Enrollment e = Enrollment.builder().build();

        e.confirmPayment();

        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(e.getPaidAt()).isNotNull();
    }

    @Test
    void isActive_PENDING_true_CONFIRMED_true_CANCELLED_false() {
        assertThat(Enrollment.builder().status(EnrollmentStatus.PENDING).build().isActive()).isTrue();
        assertThat(Enrollment.builder().status(EnrollmentStatus.CONFIRMED).build().isActive()).isTrue();
        assertThat(Enrollment.builder().status(EnrollmentStatus.CANCELLED).build().isActive()).isFalse();
    }
}
