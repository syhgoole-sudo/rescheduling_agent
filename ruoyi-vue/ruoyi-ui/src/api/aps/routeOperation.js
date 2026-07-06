import request from '@/utils/request'

export function listRouteOperation(query) {
  return request({
    url: '/aps/routeOperation/list',
    method: 'get',
    params: query
  })
}

export function getRouteOperation(routeOperationId) {
  return request({
    url: '/aps/routeOperation/' + routeOperationId,
    method: 'get'
  })
}

export function addRouteOperation(data) {
  return request({
    url: '/aps/routeOperation',
    method: 'post',
    data: data
  })
}

export function updateRouteOperation(data) {
  return request({
    url: '/aps/routeOperation',
    method: 'put',
    data: data
  })
}

export function delRouteOperation(routeOperationId) {
  return request({
    url: '/aps/routeOperation/' + routeOperationId,
    method: 'delete'
  })
}
