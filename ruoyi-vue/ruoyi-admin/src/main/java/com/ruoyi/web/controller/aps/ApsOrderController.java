package com.ruoyi.web.controller.aps;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.aps.domain.ApsOrder;
import com.ruoyi.aps.service.IApsOrderService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 订单Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/order")
public class ApsOrderController extends BaseController
{
    @Autowired
    private IApsOrderService apsOrderService;

    @PreAuthorize("@ss.hasPermi('aps:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsOrder apsOrder)
    {
        startPage();
        List<ApsOrder> list = apsOrderService.selectApsOrderList(apsOrder);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:order:export')")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsOrder apsOrder)
    {
        List<ApsOrder> list = apsOrderService.selectApsOrderList(apsOrder);
        ExcelUtil<ApsOrder> util = new ExcelUtil<ApsOrder>(ApsOrder.class);
        util.exportExcel(response, list, "订单数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:order:list')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        return success(apsOrderService.selectApsOrderById(orderId));
    }

    @PreAuthorize("@ss.hasPermi('aps:order:add')")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsOrder apsOrder)
    {
        apsOrder.setCreateBy(getUsername());
        return toAjax(apsOrderService.insertApsOrder(apsOrder));
    }

    @PreAuthorize("@ss.hasPermi('aps:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsOrder apsOrder)
    {
        apsOrder.setUpdateBy(getUsername());
        return toAjax(apsOrderService.updateApsOrder(apsOrder));
    }

    @PreAuthorize("@ss.hasPermi('aps:order:remove')")
    @Log(title = "订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(apsOrderService.deleteApsOrderByIds(orderIds));
    }
}

