package com.liveclass.course.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  /** PK (auto increment) */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 레코드 생성 시각 */
  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  /** 레코드 최종 수정 시각 */
  @LastModifiedDate
  private LocalDateTime updatedAt;

  /** 논리 삭제 시각 (null이면 활성) */
  private LocalDateTime deletedAt;

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }
}
