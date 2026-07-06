import request from '@/utils/request'

export function listScheduleTask(query) {
  return request({
    url: '/aps/scheduleTask/list',
    method: 'get',
    params: query
  })
}

export function listScheduleTaskByPlan(planId) {
  return request({
    url: '/aps/scheduleTask/listByPlan/' + planId,
    method: 'get'
  })
}

export function getScheduleTaskGantt(planId) {
  return request({
    url: '/aps/scheduleTask/gantt/' + planId,
    method: 'get'
  })
}

export function getScheduleTaskGanttCompare(newPlanId) {
  return request({
    url: '/aps/scheduleTask/ganttCompare/' + newPlanId,
    method: 'get'
  })
}

export function getScheduleTask(taskId) {
  return request({
    url: '/aps/scheduleTask/' + taskId,
    method: 'get'
  })
}

export function addScheduleTask(data) {
  return request({
    url: '/aps/scheduleTask',
    method: 'post',
    data: data
  })
}

export function updateScheduleTask(data) {
  return request({
    url: '/aps/scheduleTask',
    method: 'put',
    data: data
  })
}

export function delScheduleTask(taskId) {
  return request({
    url: '/aps/scheduleTask/' + taskId,
    method: 'delete'
  })
}
