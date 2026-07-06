import request from '@/utils/request'

export function listSchedulePlan(query) {
  return request({
    url: '/aps/schedulePlan/list',
    method: 'get',
    params: query
  })
}

export function getSchedulePlan(planId) {
  return request({
    url: '/aps/schedulePlan/' + planId,
    method: 'get'
  })
}

export function addSchedulePlan(data) {
  return request({
    url: '/aps/schedulePlan',
    method: 'post',
    data: data
  })
}

export function updateSchedulePlan(data) {
  return request({
    url: '/aps/schedulePlan',
    method: 'put',
    data: data
  })
}

export function delSchedulePlan(planId) {
  return request({
    url: '/aps/schedulePlan/' + planId,
    method: 'delete'
  })
}

export function generateInitialSchedule() {
  return request({
    url: '/aps/schedulePlan/generateInitial',
    method: 'post'
  })
}

export function compareSchedulePlan(planId) {
  return request({
    url: '/aps/schedulePlan/compare/' + planId,
    method: 'get'
  })
}

export function confirmSchedulePlan(planId) {
  return request({
    url: '/aps/schedulePlan/confirm/' + planId,
    method: 'post'
  })
}

export function rejectSchedulePlan(planId) {
  return request({
    url: '/aps/schedulePlan/reject/' + planId,
    method: 'post'
  })
}
