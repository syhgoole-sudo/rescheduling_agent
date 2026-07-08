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
import com.ruoyi.aps.domain.ApsInsertEvent;
import com.ruoyi.aps.service.IApsInsertEventService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 插单事件Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/insertEvent")
public class ApsInsertEventController extends BaseController
{
    @Autowired
    private IApsInsertEventService apsInsertEventService;

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsInsertEvent apsInsertEvent)
    {
        startPage();
        List<ApsInsertEvent> list = apsInsertEventService.selectApsInsertEventList(apsInsertEvent);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:export')")
    @Log(title = "插单事件", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsInsertEvent apsInsertEvent)
    {
        List<ApsInsertEvent> list = apsInsertEventService.selectApsInsertEventList(apsInsertEvent);
        ExcelUtil<ApsInsertEvent> util = new ExcelUtil<ApsInsertEvent>(ApsInsertEvent.class);
        util.exportExcel(response, list, "插单事件数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:list')")
    @GetMapping(value = "/{eventId}")
    public AjaxResult getInfo(@PathVariable("eventId") Long eventId)
    {
        return success(apsInsertEventService.selectApsInsertEventById(eventId));
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:add')")
    @Log(title = "插单事件", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsInsertEvent apsInsertEvent)
    {
        apsInsertEvent.setCreateBy(getUsername());
        return toAjax(apsInsertEventService.insertApsInsertEvent(apsInsertEvent));
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:add')")
    @Log(title = "插单影响分析", businessType = BusinessType.INSERT)
    @PostMapping("/createAndAnalyze/{insertOrderId}")
    public AjaxResult createAndAnalyze(@PathVariable("insertOrderId") Long insertOrderId)
    {
        return AjaxResult.success(apsInsertEventService.createAndAnalyzeInsertEvent(insertOrderId, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:add')")
    @Log(title = "插单策略推荐", businessType = BusinessType.UPDATE)
    @PostMapping("/recommendStrategy/{eventId}")
    public AjaxResult recommendStrategy(@PathVariable("eventId") Long eventId)
    {
        Map<String, Object> result = apsInsertEventService.recommendStrategy(eventId, getUsername());
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:add')")
    @Log(title = "插单局部重调度", businessType = BusinessType.INSERT)
    @PostMapping("/generateLocalReschedule/{eventId}")
    public AjaxResult generateLocalReschedule(@PathVariable("eventId") Long eventId, @RequestBody(required = false) Map<String, Object> body)
    {
        String algorithmType = body == null || body.get("algorithmType") == null ? "RULE" : body.get("algorithmType").toString();
        Integer randomSeed = parseRandomSeed(body == null ? null : body.get("randomSeed"));
        Map<String, Object> result = apsInsertEventService.generateLocalReschedule(eventId, getUsername(), algorithmType);
        result.putIfAbsent("randomSeed", randomSeed);
        return AjaxResult.success(result);
    }

    private Integer parseRandomSeed(Object value)
    {
        if (value == null || value.toString().trim().isEmpty())
        {
            return 42;
        }
        return Integer.valueOf(value.toString());
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:list')")
    @GetMapping("/explain/{eventId}")
    public AjaxResult explain(@PathVariable("eventId") Long eventId)
    {
        Map<String, Object> result = apsInsertEventService.generateExplanationReport(eventId, getUsername());
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:list')")
    @GetMapping("/aiExplain/{eventId}")
    public AjaxResult aiExplain(@PathVariable("eventId") Long eventId)
    {
        Map<String, Object> result = apsInsertEventService.generateAiExplanationReport(eventId, getUsername());
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:edit')")
    @Log(title = "插单事件", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsInsertEvent apsInsertEvent)
    {
        apsInsertEvent.setUpdateBy(getUsername());
        return toAjax(apsInsertEventService.updateApsInsertEvent(apsInsertEvent));
    }

    @PreAuthorize("@ss.hasPermi('aps:insertEvent:remove')")
    @Log(title = "插单事件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{eventIds}")
    public AjaxResult remove(@PathVariable Long[] eventIds)
    {
        return toAjax(apsInsertEventService.deleteApsInsertEventByIds(eventIds));
    }
}

