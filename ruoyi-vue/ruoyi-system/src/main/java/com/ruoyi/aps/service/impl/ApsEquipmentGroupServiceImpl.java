package com.ruoyi.aps.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.aps.mapper.ApsEquipmentGroupMapper;
import com.ruoyi.aps.domain.ApsEquipmentGroup;
import com.ruoyi.aps.service.IApsEquipmentGroupService;

/**
 * 设备组Service业务层处理
 *
 * @author aps
 */
@Service
public class ApsEquipmentGroupServiceImpl implements IApsEquipmentGroupService
{
    @Autowired
    private ApsEquipmentGroupMapper apsEquipmentGroupMapper;

    @Override
    public ApsEquipmentGroup selectApsEquipmentGroupById(Long equipmentGroupId)
    {
        return apsEquipmentGroupMapper.selectApsEquipmentGroupById(equipmentGroupId);
    }

    @Override
    public List<ApsEquipmentGroup> selectApsEquipmentGroupList(ApsEquipmentGroup apsEquipmentGroup)
    {
        return apsEquipmentGroupMapper.selectApsEquipmentGroupList(apsEquipmentGroup);
    }

    @Override
    public int insertApsEquipmentGroup(ApsEquipmentGroup apsEquipmentGroup)
    {
        return apsEquipmentGroupMapper.insertApsEquipmentGroup(apsEquipmentGroup);
    }

    @Override
    public int updateApsEquipmentGroup(ApsEquipmentGroup apsEquipmentGroup)
    {
        return apsEquipmentGroupMapper.updateApsEquipmentGroup(apsEquipmentGroup);
    }

    @Override
    public int deleteApsEquipmentGroupByIds(Long[] equipmentGroupIds)
    {
        return apsEquipmentGroupMapper.deleteApsEquipmentGroupByIds(equipmentGroupIds);
    }

    @Override
    public int deleteApsEquipmentGroupById(Long equipmentGroupId)
    {
        return apsEquipmentGroupMapper.deleteApsEquipmentGroupById(equipmentGroupId);
    }
}
