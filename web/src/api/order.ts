import request from '@/utils/request';

export default {
    list() {
        return request({
            url: '/api/orderInfo/list',
            method: 'get'
        })
    },

    queryOrderStatus(orderNo: string) {
        return request({
            url: '/api/orderInfo/queryOrderStatus/' + orderNo,
            method: 'get'
        });
    }
}