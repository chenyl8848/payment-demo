package com.cyl.payment.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WxNotifyUrl {

    /**
     * 支付通知
     */
    NATIVE_NOTIFY("/wx-pay/native/notify"),

    /**
     * 支付通知
     */
    NATIVE_NOTIFY_V2("/wx-pay-v2/native/notify"),


    /**
     * 退款结果通知
     */
    REFUND_NOTIFY("/wx-pay/native/refund/notify");

    /**
     * 类型
     */
    private final String url;
}
