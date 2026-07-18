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
import com.ruoyi.aps.domain.ApsScheduleTask;
import com.ruoyi.aps.service.IApsScheduleTaskService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 调度任务Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/scheduleTask")
public class ApsScheduleTaskController extends BaseController
{
    @Autowired
    private IApsScheduleTaskService apsScheduleTaskService;

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsScheduleTask apsScheduleTask)
    {
        startPage();
        List<ApsScheduleTask> list = apsScheduleTaskService.selectApsScheduleTaskList(apsScheduleTask);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:list')")
    @GetMapping("/listByPlan/{planId}")
    public AjaxResult listByPlan(@PathVariable("planId") Long planId)
    {
        return success(apsScheduleTaskService.selectApsScheduleTaskListByPlanId(planId));
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:list')")
    @GetMapping("/gantt/{planId}")
    public AjaxResult gantt(@PathVariable("planId") Long planId)
    {
        return success(apsScheduleTaskService.selectApsScheduleTaskGantt(planId));
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:list')")
    @GetMapping("/ganttCompare/{newPlanId}")
    public AjaxResult ganttCompare(@PathVariable("newPlanId") Long newPlanId)
    {
        return success(apsScheduleTaskService.getGanttCompareData(newPlanId));
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:export')")
    @Log(title = "调度任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsScheduleTask apsScheduleTask)
    {
        List<ApsScheduleTask> list = apsScheduleTaskService.selectApsScheduleTaskList(apsScheduleTask);
        ExcelUtil<ApsScheduleTask> util = new ExcelUtil<ApsScheduleTask>(ApsScheduleTask.class);
        util.exportExcel(response, list, "调度任务数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:list')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId)
    {
        return success(apsScheduleTaskService.selectApsScheduleTaskById(taskId));
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:add')")
    @Log(title = "调度任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsScheduleTask apsScheduleTask)
    {
        apsScheduleTask.setCreateBy(getUsername());
        return toAjax(apsScheduleTaskService.insertApsScheduleTask(apsScheduleTask));
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:edit')")
    @Log(title = "调度任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsScheduleTask apsScheduleTask)
    {
        apsScheduleTask.setUpdateBy(getUsername());
        return toAjax(apsScheduleTaskService.updateApsScheduleTask(apsScheduleTask));
    }

    @PreAuthorize("@ss.hasPermi('aps:scheduleTask:remove')")
    @Log(title = "调度任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(apsScheduleTaskService.deleteApsScheduleTaskByIds(taskIds, getUsername()));
    }
}

