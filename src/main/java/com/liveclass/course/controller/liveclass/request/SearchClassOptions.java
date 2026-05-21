package com.liveclass.course.controller.liveclass.request;

import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.global.dto.SortDirection;
import com.liveclass.course.global.dto.ClassSortType;
import com.liveclass.course.global.dto.request.PageOptions;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassesCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springdoc.core.annotations.ParameterObject;

@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ParameterObject
public class SearchClassOptions extends PageOptions {
  private ClassStatus status;
  private Long creatorId;
  private ClassSortType classSortType;
  private SortDirection sortDirection;


  public SearchClassesCommand toCommand() {
    return new SearchClassesCommand(this.status, this.creatorId,this.classSortType,this.sortDirection);
  }

}
