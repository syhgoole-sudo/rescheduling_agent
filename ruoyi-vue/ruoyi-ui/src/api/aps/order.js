import request from '@/utils/request'

export function listOrder(query) {
  return request({
    url: '/aps/order/list',
    method: 'get',
    params: query
  })
}

export function getOrder(orderId) {
  return request({
    url: '/aps/order/' + orderId,
    method: 'get'
  })
}

export function addOrder(data) {
  return request({
    url: '/aps/order',
    method: 'post',
    data: data
  })
}

export function updateOrder(data) {
  return request({
    url: '/aps/order',
    method: 'put',
    data: data
  })
}

export function delOrder(orderId) {
  return request({
    url: '/aps/order/' + orderId,
    method: 'delete'
  })
}
