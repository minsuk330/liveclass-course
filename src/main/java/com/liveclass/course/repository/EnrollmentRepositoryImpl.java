package com.liveclass.course.repository;

import static com.liveclass.course.domain.enrollment.QEnrollment.enrollment;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.global.dto.EnrollmentSortType;
import com.liveclass.course.global.dto.SortDirection;
import com.liveclass.course.service.ports.in.command.enrollment.SearchMyEnrollmentsCommand;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class EnrollmentRepositoryImpl implements EnrollmentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Enrollment> searchByClass(Long classId, EnrollmentStatus status, Pageable pageable) {
        List<Enrollment> content = queryFactory
                .selectFrom(enrollment)
                .leftJoin(enrollment.user).fetchJoin()
                .where(
                        enrollment.liveClass.id.eq(classId),
                        statusEq(status)
                )
                .orderBy(enrollment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(enrollment.count())
                .from(enrollment)
                .where(
                        enrollment.liveClass.id.eq(classId),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Enrollment> searchByUser(SearchMyEnrollmentsCommand cmd, Pageable pageable) {
        OrderSpecifier<?> order = createOrderSpecifier(cmd.sortDirection(), cmd.sortType());

        List<Enrollment> content = queryFactory
                .selectFrom(enrollment)
                .leftJoin(enrollment.liveClass).fetchJoin()
                .where(
                        enrollment.user.id.eq(cmd.userId()),
                        statusEq(cmd.status())
                )
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(enrollment.count())
                .from(enrollment)
                .where(
                        enrollment.user.id.eq(cmd.userId()),
                        statusEq(cmd.status())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?> createOrderSpecifier(SortDirection sortDirection, EnrollmentSortType sortType) {
        if (sortType == null) {
            return enrollment.createdAt.desc();
        }

        Order direction = sortDirection == SortDirection.ASC ? Order.ASC : Order.DESC;

        return switch (sortType) {
            case CREATED -> new OrderSpecifier<>(direction, enrollment.createdAt);
            case PAID -> new OrderSpecifier<>(direction, enrollment.paidAt);
        };
    }

    private BooleanExpression statusEq(EnrollmentStatus status) {
        return status == null ? null : enrollment.status.eq(status);
    }
}
