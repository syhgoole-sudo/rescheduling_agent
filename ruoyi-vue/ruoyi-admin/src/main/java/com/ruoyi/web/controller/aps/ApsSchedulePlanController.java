package com.ruoyi.web.controller.aps;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.aps.domain.ApsSchedulePlan;
import com.ruoyi.aps.service.IApsSchedulePlanService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 调度方案Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/schedulePlan")
public class ApsSchedulePlanController extends BaseController
{
    @Autowired
    private IApsSchedulePlanService apsSchedulePlanService;

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsSchedulePlan apsSchedulePlan)
    {
        startPage();
        List<ApsSchedulePlan> list = apsSchedulePlanService.selectApsSchedulePlanList(apsSchedulePlan);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:export')")
    @Log(title = "调度方案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsSchedulePlan apsSchedulePlan)
    {
        List<ApsSchedulePlan> list = apsSchedulePlanService.selectApsSchedulePlanList(apsSchedulePlan);
        ExcelUtil<ApsSchedulePlan> util = new ExcelUtil<ApsSchedulePlan>(ApsSchedulePlan.class);
        util.exportExcel(response, list, "调度方案数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:list')")
    @GetMapping(value = "/{planId}")
    public AjaxResult getInfo(@PathVariable("planId") Long planId)
    {
        return success(apsSchedulePlanService.selectApsSchedulePlanById(planId));
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:add')")
    @Log(title = "调度方案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsSchedulePlan apsSchedulePlan)
    {
        apsSchedulePlan.setCreateBy(getUsername());
        return toAjax(apsSchedulePlanService.insertApsSchedulePlan(apsSchedulePlan));
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:add')")
    @Log(title = "初始调度", businessType = BusinessType.INSERT)
    @PostMapping("/generateInitial")
    public AjaxResult generateInitial()
    {
        return success(apsSchedulePlanService.generateInitialSchedule(getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:list')")
    @GetMapping("/compare/{newPlanId}")
    public AjaxResult compare(@PathVariable("newPlanId") Long newPlanId)
    {
        Map<String, Object> result = apsSchedulePlanService.compareReschedulePlan(newPlanId);
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:edit')")
    @Log(title = "确认重调度方案", businessType = BusinessType.UPDATE)
    @PostMapping("/confirm/{planId}")
    public AjaxResult confirm(@PathVariable("planId") Long planId)
    {
        Map<String, Object> result = apsSchedulePlanService.confirmReschedulePlan(planId, getUsername());
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:edit')")
    @Log(title = "拒绝重调度方案", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{planId}")
    public AjaxResult reject(@PathVariable("planId") Long planId)
    {
        Map<String, Object> result = apsSchedulePlanService.rejectReschedulePlan(planId, getUsername());
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:edit')")
    @Log(title = "调度方案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsSchedulePlan apsSchedulePlan)
    {
        apsSchedulePlan.setUpdateBy(getUsername());
        return toAjax(apsSchedulePlanService.updateApsSchedulePlan(apsSchedulePlan));
    }

    @PreAuthorize("@ss.hasPermi('aps:schedulePlan:remove')")
    @Log(title = "调度方案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable Long[] planIds)
    {
        return toAjax(apsSchedulePlanService.deleteApsSchedulePlanByIds(planIds));
    }
}

