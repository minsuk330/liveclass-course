package com.liveclass.course.domain.liveclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveclass.course.domain.common.DomainErrorCode;
import com.liveclass.course.domain.common.DomainException;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.user.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LiveClassTest {

    private User creator() {
        return User.builder().name("강사").role(UserRole.CREATOR).build();
    }

    private LiveClass.LiveClassBuilder baseBuilder() {
        return LiveClass.builder()
                .title("강의")
                .description("설명")
                .price(new BigDecimal("10000"))
                .capacity(10)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .creator(creator());
    }

    @Test
    void 기본_상태는_DRAFT다() {
        LiveClass cls = baseBuilder().build();

        assertThat(cls.getStatus()).isEqualTo(ClassStatus.DRAFT);
    }

    @Test
    void DRAFT에서_OPEN으로_전환_가능() {
        LiveClass cls = baseBuilder().status(ClassStatus.DRAFT).build();

        cls.changeStatus(ClassStatus.OPEN);

        assertThat(cls.getStatus()).isEqualTo(ClassStatus.OPEN);
    }

    @Test
    void OPEN에서_CLOSED로_전환_가능() {
        LiveClass cls = baseBuilder().status(ClassStatus.OPEN).build();

        cls.changeStatus(ClassStatus.CLOSED);

        assertThat(cls.getStatus()).isEqualTo(ClassStatus.CLOSED);
    }

    @Test
    void DRAFT에서_CLOSED로_바로_전환_불가() {
        LiveClass cls = baseBuilder().status(ClassStatus.DRAFT).build();

        assertThatThrownBy(() -> cls.changeStatus(ClassStatus.CLOSED))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.INVALID_CLASS_STATUS_TRANSITION);
    }

    @Test
    void OPEN에서_DRAFT로_역방향_전환_불가() {
        LiveClass cls = baseBuilder().status(ClassStatus.OPEN).build();

        assertThatThrownBy(() -> cls.changeStatus(ClassStatus.DRAFT))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void CLOSED는_terminal_어떤_전환도_불가() {
        LiveClass cls = baseBuilder().status(ClassStatus.CLOSED).build();

        assertThatThrownBy(() -> cls.changeStatus(ClassStatus.DRAFT))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> cls.changeStatus(ClassStatus.OPEN))
                .isInstanceOf(DomainException.class);
    }
}
