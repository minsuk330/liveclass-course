package com.liveclass.course.domain.waitlistentry;

import com.liveclass.course.domain.common.BaseEntity;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "waitlist_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_waitlist_class_user",
                columnNames = {"class_id", "user_id"}
        ),
        indexes = @Index(name = "idx_waitlist_class", columnList = "class_id, position")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class WaitlistEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private LiveClass liveClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer position;
}
