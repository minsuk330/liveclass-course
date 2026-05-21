package com.liveclass.course.domain.liveclass;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClassStatusTest {

    static Stream<Arguments> transitions() {
        return Stream.of(
                Arguments.of(ClassStatus.DRAFT, ClassStatus.OPEN, true),
                Arguments.of(ClassStatus.OPEN, ClassStatus.CLOSED, true),

                Arguments.of(ClassStatus.DRAFT, ClassStatus.CLOSED, false),
                Arguments.of(ClassStatus.DRAFT, ClassStatus.DRAFT, false),

                Arguments.of(ClassStatus.OPEN, ClassStatus.DRAFT, false),
                Arguments.of(ClassStatus.OPEN, ClassStatus.OPEN, false),

                Arguments.of(ClassStatus.CLOSED, ClassStatus.DRAFT, false),
                Arguments.of(ClassStatus.CLOSED, ClassStatus.OPEN, false),
                Arguments.of(ClassStatus.CLOSED, ClassStatus.CLOSED, false)
        );
    }

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @MethodSource("transitions")
    void canTransitionTo(ClassStatus from, ClassStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }
}
