package com.liveclass.course.domain.liveclass;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLiveClass is a Querydsl query type for LiveClass
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLiveClass extends EntityPathBase<LiveClass> {

    private static final long serialVersionUID = 1290882980L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLiveClass liveClass = new QLiveClass("liveClass");

    public final com.liveclass.course.domain.common.QBaseEntity _super = new com.liveclass.course.domain.common.QBaseEntity(this);

    public final NumberPath<Integer> capacity = createNumber("capacity", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.liveclass.course.domain.user.QUser creator;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath description = createString("description");

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final EnumPath<ClassStatus> status = createEnum("status", ClassStatus.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public QLiveClass(String variable) {
        this(LiveClass.class, forVariable(variable), INITS);
    }

    public QLiveClass(Path<? extends LiveClass> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLiveClass(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLiveClass(PathMetadata metadata, PathInits inits) {
        this(LiveClass.class, metadata, inits);
    }

    public QLiveClass(Class<? extends LiveClass> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.creator = inits.isInitialized("creator") ? new com.liveclass.course.domain.user.QUser(forProperty("creator")) : null;
    }

}

