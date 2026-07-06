package com.ruoyi.aps.mapper;

import java.util.List;
import com.ruoyi.aps.domain.ApsEquipment;

/**
 * 设备Mapper接口
 *
 * @author aps
 */
public interface ApsEquipmentMapper
{
    public ApsEquipment selectApsEquipmentById(Long equipmentId);

    public List<ApsEquipment> selectApsEquipmentList(ApsEquipment apsEquipment);

    public int insertApsEquipment(ApsEquipment apsEquipment);

    public int updateApsEquipment(ApsEquipment apsEquipment);

    public int deleteApsEquipmentById(Long equipmentId);

    public int deleteApsEquipmentByIds(Long[] equipmentIds);
}
