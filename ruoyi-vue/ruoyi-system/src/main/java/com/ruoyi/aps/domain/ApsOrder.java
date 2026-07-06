package com.ruoyi.aps.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 订单对象 aps_order
 *
 * @author aps
 */
public class ApsOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @Excel(name = "订单ID")
    private Long orderId;

    /** 订单编码 */
    @Excel(name = "订单编码")
    private String orderCode;

    /** 订单类型：NORMAL普通订单，INSERT插单订单 */
    @Excel(name = "订单类型：NORMAL普通订单，INSERT插单订单")
    private String orderType;

    /** 产品ID */
    @Excel(name = "产品ID")
    private Long productId;

    /** 产品编码 */
    @Excel(name = "产品编码")
    private String productCode;

    /** 订单数量 */
    @Excel(name = "订单数量")
    private BigDecimal quantity;

    /** 优先级，数值越小优先级越高 */
    @Excel(name = "优先级，数值越小优先级越高")
    private Integer priorityLevel;

    /** 可开工时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "可开工时间")
    private Date releaseTime;

    /** 交期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "交期")
    private Date dueTime;

    /** 订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭 */
    @Excel(name = "订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭")
    private String orderStatus;

    /** 删除标志：0存在，2删除 */
    @Excel(name = "删除标志：0存在，2删除")
    private String delFlag;

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderCode(String orderCode)
    {
        this.orderCode = orderCode;
    }

    public String getOrderCode()
    {
        return orderCode;
    }

    public void setOrderType(String orderType)
    {
        this.orderType = orderType;
    }

    public String getOrderType()
    {
        return orderType;
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

    public void setQuantity(BigDecimal quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getQuantity()
    {
        return quantity;
    }

    public void setPriorityLevel(Integer priorityLevel)
    {
        this.priorityLevel = priorityLevel;
    }

    public Integer getPriorityLevel()
    {
        return priorityLevel;
    }

    public void setReleaseTime(Date releaseTime)
    {
        this.releaseTime = releaseTime;
    }

    public Date getReleaseTime()
    {
        return releaseTime;
    }

    public void setDueTime(Date dueTime)
    {
        this.dueTime = dueTime;
    }

    public Date getDueTime()
    {
        return dueTime;
    }

    public void setOrderStatus(String orderStatus)
    {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatus()
    {
        return orderStatus;
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
            .append("orderId", getOrderId())
            .append("orderCode", getOrderCode())
            .append("orderType", getOrderType())
            .append("productId", getProductId())
            .append("productCode", getProductCode())
            .append("quantity", getQuantity())
            .append("priorityLevel", getPriorityLevel())
            .append("releaseTime", getReleaseTime())
            .append("dueTime", getDueTime())
            .append("orderStatus", getOrderStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
