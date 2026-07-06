package com.ruoyi.aps.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工艺路线工序对象 aps_route_operation
 *
 * @author aps
 */
public class ApsRouteOperation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 产品工艺路线工序ID */
    @Excel(name = "产品工艺路线工序ID")
    private Long routeOperationId;

    /** 产品ID */
    @Excel(name = "产品ID")
    private Long productId;

    /** 产品编码 */
    @Excel(name = "产品编码")
    private String productCode;

    /** 工序顺序号 */
    @Excel(name = "工序顺序号")
    private Integer processSeq;

    /** 工序编码 */
    @Excel(name = "工序编码")
    private String processCode;

    /** 工序名称 */
    @Excel(name = "工序名称")
    private String processName;

    /** 设备组ID */
    @Excel(name = "设备组ID")
    private Long equipmentGroupId;

    /** 标准加工时长，单位分钟 */
    @Excel(name = "标准加工时长，单位分钟")
    private Integer standardDuration;

    /** 状态：0正常，1停用 */
    @Excel(name = "状态：0正常，1停用")
    private String status;

    /** 删除标志：0存在，2删除 */
    @Excel(name = "删除标志：0存在，2删除")
    private String delFlag;

    public void setRouteOperationId(Long routeOperationId)
    {
        this.routeOperationId = routeOperationId;
    }

    public Long getRouteOperationId()
    {
        return routeOperationId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductCode(String productCode)
    {
        this.productCode = productCode;
    }

    public String getProductCode()
    {
        return productCode;
    }

    public void setProcessSeq(Integer processSeq)
    {
        this.processSeq = processSeq;
    }

    public Integer getProcessSeq()
    {
        return processSeq;
    }

    public void setProcessCode(String processCode)
    {
        this.processCode = processCode;
    }

    public String getProcessCode()
    {
        return processCode;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setEquipmentGroupId(Long equipmentGroupId)
    {
        this.equipmentGroupId = equipmentGroupId;
    }

    public Long getEquipmentGroupId()
    {
        return equipmentGroupId;
    }

    public void setStandardDuration(Integer standardDuration)
    {
        this.standardDuration = standardDuration;
    }

    public Integer getStandardDuration()
    {
        return standardDuration;
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
            .append("routeOperationId", getRouteOperationId())
            .append("productId", getProductId())
            .append("productCode", getProductCode())
            .append("processSeq", getProcessSeq())
            .append("processCode", getProcessCode())
            .append("processName", getProcessName())
            .append("equipmentGroupId", getEquipmentGroupId())
            .append("standardDuration", getStandardDuration())
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
