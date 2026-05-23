package com.liveclass.course.service.ports.in;

import com.liveclass.course.service.ports.in.command.payment.ConfirmPaymentCommand;
import com.liveclass.course.service.ports.in.command.payment.InitiatePaymentCommand;
import com.liveclass.course.service.ports.in.command.payment.PaymentCallbackCommand;
import com.liveclass.course.service.ports.in.result.payment.ConfirmPaymentResult;
import com.liveclass.course.service.ports.in.result.payment.InitiatePaymentResult;

public interface PaymentService {

    InitiatePaymentResult initiate(InitiatePaymentCommand command);

    void handleCallback(PaymentCallbackCommand command);

    ConfirmPaymentResult confirm(ConfirmPaymentCommand command);
}
