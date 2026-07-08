import request from '@/utils/request'

export function listInsertEvent(query) {
  return request({
    url: '/aps/insertEvent/list',
    method: 'get',
    params: query
  })
}

export function getInsertEvent(eventId) {
  return request({
    url: '/aps/insertEvent/' + eventId,
    method: 'get'
  })
}

export function addInsertEvent(data) {
  return request({
    url: '/aps/insertEvent',
    method: 'post',
    data: data
  })
}

export function createAndAnalyzeInsertEvent(insertOrderId) {
  return request({
    url: '/aps/insertEvent/createAndAnalyze/' + insertOrderId,
    method: 'post'
  })
}

export function recommendStrategy(eventId) {
  return request({
    url: '/aps/insertEvent/recommendStrategy/' + eventId,
    method: 'post'
  })
}

export function generateLocalReschedule(eventId, algorithmType, randomSeed) {
  const seed = randomSeed === undefined || randomSeed === null || randomSeed === '' ? 42 : randomSeed
  return request({
    url: '/aps/insertEvent/generateLocalReschedule/' + eventId,
    method: 'post',
    data: {
      algorithmType: algorithmType || 'RULE',
      randomSeed: algorithmType === 'GA' ? seed : undefined
    }
  })
}

export function generateExplanationReport(eventId) {
  return request({
    url: '/aps/insertEvent/explain/' + eventId,
    method: 'get'
  })
}

export function generateAiExplanationReport(eventId) {
  return request({
    url: '/aps/insertEvent/aiExplain/' + eventId,
    method: 'get',
    timeout: 35000
  })
}

export function updateInsertEvent(data) {
  return request({
    url: '/aps/insertEvent',
    method: 'put',
    data: data
  })
}

export function delInsertEvent(eventId) {
  return request({
    url: '/aps/insertEvent/' + eventId,
    method: 'delete'
  })
}
