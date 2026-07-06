package com.ruoyi.aps.service;

import java.util.List;
import com.ruoyi.aps.domain.ApsEquipmentGroup;

/**
 * 设备组Service接口
 *
 * @author aps
 */
public interface IApsEquipmentGroupService
{
    public ApsEquipmentGroup selectApsEquipmentGroupById(Long equipmentGroupId);

    public List<ApsEquipmentGroup> selectApsEquipmentGroupList(ApsEquipmentGroup apsEquipmentGroup);

    public int insertApsEquipmentGroup(ApsEquipmentGroup apsEquipmentGroup);

    public int updateApsEquipmentGroup(ApsEquipmentGroup apsEquipmentGroup);

    public int deleteApsEquipmentGroupByIds(Long[] equipmentGroupIds);

    public int deleteApsEquipmentGroupById(Long equipmentGroupId);
}
