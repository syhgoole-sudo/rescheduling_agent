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
import com.ruoyi.aps.domain.ApsEquipment;
import com.ruoyi.aps.service.IApsEquipmentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 设备Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/equipment")
public class ApsEquipmentController extends BaseController
{
    @Autowired
    private IApsEquipmentService apsEquipmentService;

    @PreAuthorize("@ss.hasPermi('aps:equipment:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsEquipment apsEquipment)
    {
        startPage();
        List<ApsEquipment> list = apsEquipmentService.selectApsEquipmentList(apsEquipment);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:equipment:export')")
    @Log(title = "设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsEquipment apsEquipment)
    {
        List<ApsEquipment> list = apsEquipmentService.selectApsEquipmentList(apsEquipment);
        ExcelUtil<ApsEquipment> util = new ExcelUtil<ApsEquipment>(ApsEquipment.class);
        util.exportExcel(response, list, "设备数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:equipment:list')")
    @GetMapping(value = "/{equipmentId}")
    public AjaxResult getInfo(@PathVariable("equipmentId") Long equipmentId)
    {
        return success(apsEquipmentService.selectApsEquipmentById(equipmentId));
    }

    @PreAuthorize("@ss.hasPermi('aps:equipment:add')")
    @Log(title = "设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsEquipment apsEquipment)
    {
        apsEquipment.setCreateBy(getUsername());
        return toAjax(apsEquipmentService.insertApsEquipment(apsEquipment));
    }

    @PreAuthorize("@ss.hasPermi('aps:equipment:edit')")
    @Log(title = "设备", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsEquipment apsEquipment)
    {
        apsEquipment.setUpdateBy(getUsername());
        return toAjax(apsEquipmentService.updateApsEquipment(apsEquipment));
    }

    @PreAuthorize("@ss.hasPermi('aps:equipment:remove')")
    @Log(title = "设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{equipmentIds}")
    public AjaxResult remove(@PathVariable Long[] equipmentIds)
    {
        return toAjax(apsEquipmentService.deleteApsEquipmentByIds(equipmentIds));
    }
}

