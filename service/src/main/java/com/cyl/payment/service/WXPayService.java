package com.cyl.payment.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * @author cyl
 * @date 2022-11-27 17:46
 * @description
 */
public interface WXPayService {
    /**
     * 发起支付请求
     *
     * @param productId 商品id
     * @return 支付地址 + 订单
     */
    Map<String, Object> nativePay(Long productId) throws Exception;

    /**
     * 处理订单
     *
     * @param map
     */
    void processOrderInfo(Map<String, Object> map) throws GeneralSecurityException;

    /**
     * 微信支付用户取消订单
     *
     * @param orderNo
     */
    void nativeCancel(String orderNo) throws Exception;

    /**
     * 查询订单
     *
     * @param orderNo 订单号
     * @return
     */
    String queryOrder(String orderNo) throws Exception;

    /**
     * 根据订单号查询微信支付订单状态，核实订单状态
     * 如订单已支付，更新商户端订单状态
     * 如果订单未支付，则调用关闭订单接口，并更新商户端订单状态
     *
     * @param orderNo 订单号
     */
    void checkOrderStatus(String orderNo) throws Exception;

    /**
     * 微信支付申请退款
     *
     * @param orderNo 订单号
     * @param reason  退款原因
     */
    void refund(String orderNo, String reason) throws IOException;

    /**
     * 微信支付查询退款
     *
     * @param refundNo 退款单号
     * @return
     */
    String queryRefund(String refundNo) throws Exception;

    /**
     * 处理退款单
     *
     * @param bodyMap
     */
    void processRefund(Map<String, Object> bodyMap) throws GeneralSecurityException;

    /**
     * 微信支付查询账单
     *
     * @param type     账单类型：交易账单、资金账单
     * @param billDate 交易日期
     * @return
     */
    String queryBill(String type, String billDate) throws Exception;

    /**
     * 下载账单
     *
     * @param type     账单类型：交易账单、资金账单
     * @param billDate 交易日期
     * @return
     */
    String downloadBill(String type, String billDate) throws Exception;
}
