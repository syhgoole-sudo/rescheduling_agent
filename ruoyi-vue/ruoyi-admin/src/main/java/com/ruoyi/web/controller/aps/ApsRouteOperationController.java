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
import com.ruoyi.aps.domain.ApsRouteOperation;
import com.ruoyi.aps.service.IApsRouteOperationService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工艺路线工序Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/routeOperation")
public class ApsRouteOperationController extends BaseController
{
    @Autowired
    private IApsRouteOperationService apsRouteOperationService;

    @PreAuthorize("@ss.hasPermi('aps:routeOperation:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsRouteOperation apsRouteOperation)
    {
        startPage();
        List<ApsRouteOperation> list = apsRouteOperationService.selectApsRouteOperationList(apsRouteOperation);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:routeOperation:export')")
    @Log(title = "工艺路线工序", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsRouteOperation apsRouteOperation)
    {
        List<ApsRouteOperation> list = apsRouteOperationService.selectApsRouteOperationList(apsRouteOperation);
        ExcelUtil<ApsRouteOperation> util = new ExcelUtil<ApsRouteOperation>(ApsRouteOperation.class);
        util.exportExcel(response, list, "工艺路线工序数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:routeOperation:list')")
    @GetMapping(value = "/{routeOperationId}")
    public AjaxResult getInfo(@PathVariable("routeOperationId") Long routeOperationId)
    {
        return success(apsRouteOperationService.selectApsRouteOperationById(routeOperationId));
    }

    @PreAuthorize("@ss.hasPermi('aps:routeOperation:add')")
    @Log(title = "工艺路线工序", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsRouteOperation apsRouteOperation)
    {
        apsRouteOperation.setCreateBy(getUsername());
        return toAjax(apsRouteOperationService.insertApsRouteOperation(apsRouteOperation));
    }

    @PreAuthorize("@ss.hasPermi('aps:routeOperation:edit')")
    @Log(title = "工艺路线工序", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsRouteOperation apsRouteOperation)
    {
        apsRouteOperation.setUpdateBy(getUsername());
        return toAjax(apsRouteOperationService.updateApsRouteOperation(apsRouteOperation));
    }

    @PreAuthorize("@ss.hasPermi('aps:routeOperation:remove')")
    @Log(title = "工艺路线工序", businessType = BusinessType.DELETE)
    @DeleteMapping("/{routeOperationIds}")
    public AjaxResult remove(@PathVariable Long[] routeOperationIds)
    {
        return toAjax(apsRouteOperationService.deleteApsRouteOperationByIds(routeOperationIds));
    }
}

