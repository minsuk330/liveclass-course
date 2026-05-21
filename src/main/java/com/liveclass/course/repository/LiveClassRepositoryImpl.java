package com.liveclass.course.repository;

import static com.liveclass.course.domain.liveclass.QLiveClass.liveClass;

import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.global.dto.SortDirection;
import com.liveclass.course.global.dto.ClassSortType;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassesCommand;
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
public class LiveClassRepositoryImpl implements LiveClassRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<LiveClass> search(SearchClassesCommand cmd, Pageable pageable) {
        OrderSpecifier<?> order = createOrderSpecifier(cmd.sortDirection(), cmd.classSortType());

        List<LiveClass> content = queryFactory
                .selectFrom(liveClass)
                .where(statusEq(cmd.status()), creatorIdEq(cmd.creatorId()))
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(liveClass.count())
                .from(liveClass)
                .where(statusEq(cmd.status()), creatorIdEq(cmd.creatorId()))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?> createOrderSpecifier(SortDirection sortDirection, ClassSortType classSortType) {
        if (classSortType == null) {
            return liveClass.createdAt.desc();
        }

        Order direction = sortDirection == SortDirection.ASC ? Order.ASC : Order.DESC;

        return switch (classSortType) {
            case CREATED -> new OrderSpecifier<>(direction, liveClass.createdAt);
            case UPDATED -> new OrderSpecifier<>(direction, liveClass.updatedAt);
            case PRICE -> new OrderSpecifier<>(direction, liveClass.price);
            case START_DATE -> new OrderSpecifier<>(direction, liveClass.startDate);
            case END_DATE -> new OrderSpecifier<>(direction, liveClass.endDate);
            case TITLE -> new OrderSpecifier<>(direction, liveClass.title);
        };
    }

    private BooleanExpression statusEq(ClassStatus status) {
        return status == null ? null : liveClass.status.eq(status);
    }

    private BooleanExpression creatorIdEq(Long creatorId) {
        return creatorId == null ? null : liveClass.creator.id.eq(creatorId);
    }
}
