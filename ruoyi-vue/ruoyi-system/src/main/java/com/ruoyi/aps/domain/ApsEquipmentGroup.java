package com.ruoyi.aps.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备组对象 aps_equipment_group
 *
 * @author aps
 */
public class ApsEquipmentGroup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备组ID */
    @Excel(name = "设备组ID")
    private Long equipmentGroupId;

    /** 设备组编码 */
    @Excel(name = "设备组编码")
    private String equipmentGroupCode;

    /** 设备组名称 */
    @Excel(name = "设备组名称")
    private String equipmentGroupName;

    /** 状态：0正常，1停用 */
    @Excel(name = "状态：0正常，1停用")
    private String status;

    /** 删除标志：0存在，2删除 */
    @Excel(name = "删除标志：0存在，2删除")
    private String delFlag;

    public void setEquipmentGroupId(Long equipmentGroupId)
    {
        this.equipmentGroupId = equipmentGroupId;
    }

    public Long getEquipmentGroupId()
    {
        return equipmentGroupId;
    }

    public void setEquipmentGroupCode(String equipmentGroupCode)
    {
        this.equipmentGroupCode = equipmentGroupCode;
    }

    public String getEquipmentGroupCode()
    {
        return equipmentGroupCode;
    }

    public void setEquipmentGroupName(String equipmentGroupName)
    {
        this.equipmentGroupName = equipmentGroupName;
    }

    public String getEquipmentGroupName()
    {
        return equipmentGroupName;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("equipmentGroupId", getEquipmentGroupId())
            .append("equipmentGroupCode", getEquipmentGroupCode())
            .append("equipmentGroupName", getEquipmentGroupName())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
