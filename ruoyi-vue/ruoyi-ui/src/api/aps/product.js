import request from '@/utils/request'

export function listProduct(query) {
  return request({
    url: '/aps/product/list',
    method: 'get',
    params: query
  })
}

export function getProduct(productId) {
  return request({
    url: '/aps/product/' + productId,
    method: 'get'
  })
}

export function addProduct(data) {
  return request({
    url: '/aps/product',
    method: 'post',
    data: data
  })
}

export function updateProduct(data) {
  return request({
    url: '/aps/product',
    method: 'put',
    data: data
  })
}

export function delProduct(productId) {
  return request({
    url: '/aps/product/' + productId,
    method: 'delete'
  })
}
