package com.cyl.payment.service;

import com.cyl.payment.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RefundInfoService extends IService<RefundInfo> {

    /**
     * 根据订单编号创建退款记录
     *
     * @param orderNo 订单号
     * @param reason 退款原因
     * @return
     */
    RefundInfo createRefundByOrderNo(String orderNo, String reason);

    /**
     * 更新退款单
     * @param content
     */
    void updateRefund(String content);
}
