package com.liveclass.course.infra.pg;

import com.liveclass.course.infra.pg.dto.PgApproveRequest;
import com.liveclass.course.infra.pg.dto.PgApproveResponse;
import com.liveclass.course.infra.pg.dto.PgReadyRequest;
import com.liveclass.course.infra.pg.dto.PgReadyResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockNicePgClient implements PgClient {

    private static final String PAYMENT_URL_PREFIX = "https://mock-nicepay.test/pay/";

    @Override
    public PgReadyResponse ready(PgReadyRequest request) {
        String tid = "nicepay_" + UUID.randomUUID().toString().replace("-", "");
        return new PgReadyResponse(tid, PAYMENT_URL_PREFIX + tid);
    }

    @Override
    public PgApproveResponse approve(PgApproveRequest request) {
        return PgApproveResponse.ok(request.tid(), request.amount());
    }
}
