package com.ruoyi.aps.service;

import java.util.List;
import com.ruoyi.aps.domain.ApsEquipment;

/**
 * 设备Service接口
 *
 * @author aps
 */
public interface IApsEquipmentService
{
    public ApsEquipment selectApsEquipmentById(Long equipmentId);

    public List<ApsEquipment> selectApsEquipmentList(ApsEquipment apsEquipment);

    public int insertApsEquipment(ApsEquipment apsEquipment);

    public int updateApsEquipment(ApsEquipment apsEquipment);

    public int deleteApsEquipmentByIds(Long[] equipmentIds);

    public int deleteApsEquipmentById(Long equipmentId);
}
