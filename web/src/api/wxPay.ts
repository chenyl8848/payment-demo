import request from '@/utils/request';

export default {
    nativePay(productId: number) {
        return request({
            url: '/api/wx-pay/native/' + productId,
            method: 'post'
        })
    },

    nativeCancel(orderNo: string) {
        return request({
            url: '/api/wx-pay/native/cancel/' + orderNo,
            method: 'post'
        });
    },

    nativeRefund(orderNo: string, reason: string) {
        return request({
            url: '/api/wx-pay/native/refund/' + orderNo + '/' + reason,
            method: 'post'
        });
    }
}