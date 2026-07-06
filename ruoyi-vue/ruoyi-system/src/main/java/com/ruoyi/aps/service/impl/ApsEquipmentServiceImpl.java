package com.ruoyi.aps.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.aps.mapper.ApsEquipmentMapper;
import com.ruoyi.aps.domain.ApsEquipment;
import com.ruoyi.aps.service.IApsEquipmentService;

/**
 * 设备Service业务层处理
 *
 * @author aps
 */
@Service
public class ApsEquipmentServiceImpl implements IApsEquipmentService
{
    @Autowired
    private ApsEquipmentMapper apsEquipmentMapper;

    @Override
    public ApsEquipment selectApsEquipmentById(Long equipmentId)
    {
        return apsEquipmentMapper.selectApsEquipmentById(equipmentId);
    }

    @Override
    public List<ApsEquipment> selectApsEquipmentList(ApsEquipment apsEquipment)
    {
        return apsEquipmentMapper.selectApsEquipmentList(apsEquipment);
    }

    @Override
    public int insertApsEquipment(ApsEquipment apsEquipment)
    {
        return apsEquipmentMapper.insertApsEquipment(apsEquipment);
    }

    @Override
    public int updateApsEquipment(ApsEquipment apsEquipment)
    {
        return apsEquipmentMapper.updateApsEquipment(apsEquipment);
    }

    @Override
    public int deleteApsEquipmentByIds(Long[] equipmentIds)
    {
        return apsEquipmentMapper.deleteApsEquipmentByIds(equipmentIds);
    }

    @Override
    public int deleteApsEquipmentById(Long equipmentId)
    {
        return apsEquipmentMapper.deleteApsEquipmentById(equipmentId);
    }
}
