package com.liveclass.course.infra.pg;

import com.liveclass.course.infra.pg.dto.PgApproveRequest;
import com.liveclass.course.infra.pg.dto.PgApproveResponse;
import com.liveclass.course.infra.pg.dto.PgReadyRequest;
import com.liveclass.course.infra.pg.dto.PgReadyResponse;

public interface PgClient {

    PgReadyResponse ready(PgReadyRequest request);

    PgApproveResponse approve(PgApproveRequest request);
}
