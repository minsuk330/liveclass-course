package com.liveclass.course.domain.waitlistentry;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWaitlistEntry is a Querydsl query type for WaitlistEntry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWaitlistEntry extends EntityPathBase<WaitlistEntry> {

    private static final long serialVersionUID = 647074506L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWaitlistEntry waitlistEntry = new QWaitlistEntry("waitlistEntry");

    public final com.liveclass.course.domain.common.QBaseEntity _super = new com.liveclass.course.domain.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.liveclass.course.domain.liveclass.QLiveClass liveClass;

    public final NumberPath<Integer> position = createNumber("position", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.liveclass.course.domain.user.QUser user;

    public QWaitlistEntry(String variable) {
        this(WaitlistEntry.class, forVariable(variable), INITS);
    }

    public QWaitlistEntry(Path<? extends WaitlistEntry> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWaitlistEntry(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWaitlistEntry(PathMetadata metadata, PathInits inits) {
        this(WaitlistEntry.class, metadata, inits);
    }

    public QWaitlistEntry(Class<? extends WaitlistEntry> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.liveClass = inits.isInitialized("liveClass") ? new com.liveclass.course.domain.liveclass.QLiveClass(forProperty("liveClass"), inits.get("liveClass")) : null;
        this.user = inits.isInitialized("user") ? new com.liveclass.course.domain.user.QUser(forProperty("user")) : null;
    }

}

