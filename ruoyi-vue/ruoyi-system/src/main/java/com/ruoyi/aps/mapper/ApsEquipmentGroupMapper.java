package com.ruoyi.aps.mapper;

import java.util.List;
import com.ruoyi.aps.domain.ApsEquipmentGroup;

/**
 * 设备组Mapper接口
 *
 * @author aps
 */
public interface ApsEquipmentGroupMapper
{
    public ApsEquipmentGroup selectApsEquipmentGroupById(Long equipmentGroupId);

    public List<ApsEquipmentGroup> selectApsEquipmentGroupList(ApsEquipmentGroup apsEquipmentGroup);

    public int insertApsEquipmentGroup(ApsEquipmentGroup apsEquipmentGroup);

    public int updateApsEquipmentGroup(ApsEquipmentGroup apsEquipmentGroup);

    public int deleteApsEquipmentGroupById(Long equipmentGroupId);

    public int deleteApsEquipmentGroupByIds(Long[] equipmentGroupIds);
}
