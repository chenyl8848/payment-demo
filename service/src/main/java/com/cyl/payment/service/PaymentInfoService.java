package com.cyl.payment.service;

public interface PaymentInfoService {

    /**
     * 记录支付日志
     * @param plainText
     */
    void createPaymentInfo(String plainText);
}
