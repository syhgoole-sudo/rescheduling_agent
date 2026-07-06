import request from '@/utils/request'

export function listEquipmentGroup(query) {
  return request({
    url: '/aps/equipmentGroup/list',
    method: 'get',
    params: query
  })
}

export function getEquipmentGroup(equipmentGroupId) {
  return request({
    url: '/aps/equipmentGroup/' + equipmentGroupId,
    method: 'get'
  })
}

export function addEquipmentGroup(data) {
  return request({
    url: '/aps/equipmentGroup',
    method: 'post',
    data: data
  })
}

export function updateEquipmentGroup(data) {
  return request({
    url: '/aps/equipmentGroup',
    method: 'put',
    data: data
  })
}

export function delEquipmentGroup(equipmentGroupId) {
  return request({
    url: '/aps/equipmentGroup/' + equipmentGroupId,
    method: 'delete'
  })
}
