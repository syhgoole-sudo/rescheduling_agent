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
import com.ruoyi.aps.domain.ApsEquipmentGroup;
import com.ruoyi.aps.service.IApsEquipmentGroupService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 设备组Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/equipmentGroup")
public class ApsEquipmentGroupController extends BaseController
{
    @Autowired
    private IApsEquipmentGroupService apsEquipmentGroupService;

    @PreAuthorize("@ss.hasPermi('aps:equipmentGroup:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsEquipmentGroup apsEquipmentGroup)
    {
        startPage();
        List<ApsEquipmentGroup> list = apsEquipmentGroupService.selectApsEquipmentGroupList(apsEquipmentGroup);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:equipmentGroup:export')")
    @Log(title = "设备组", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsEquipmentGroup apsEquipmentGroup)
    {
        List<ApsEquipmentGroup> list = apsEquipmentGroupService.selectApsEquipmentGroupList(apsEquipmentGroup);
        ExcelUtil<ApsEquipmentGroup> util = new ExcelUtil<ApsEquipmentGroup>(ApsEquipmentGroup.class);
        util.exportExcel(response, list, "设备组数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:equipmentGroup:list')")
    @GetMapping(value = "/{equipmentGroupId}")
    public AjaxResult getInfo(@PathVariable("equipmentGroupId") Long equipmentGroupId)
    {
        return success(apsEquipmentGroupService.selectApsEquipmentGroupById(equipmentGroupId));
    }

    @PreAuthorize("@ss.hasPermi('aps:equipmentGroup:add')")
    @Log(title = "设备组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsEquipmentGroup apsEquipmentGroup)
    {
        apsEquipmentGroup.setCreateBy(getUsername());
        return toAjax(apsEquipmentGroupService.insertApsEquipmentGroup(apsEquipmentGroup));
    }

    @PreAuthorize("@ss.hasPermi('aps:equipmentGroup:edit')")
    @Log(title = "设备组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsEquipmentGroup apsEquipmentGroup)
    {
        apsEquipmentGroup.setUpdateBy(getUsername());
        return toAjax(apsEquipmentGroupService.updateApsEquipmentGroup(apsEquipmentGroup));
    }

    @PreAuthorize("@ss.hasPermi('aps:equipmentGroup:remove')")
    @Log(title = "设备组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{equipmentGroupIds}")
    public AjaxResult remove(@PathVariable Long[] equipmentGroupIds)
    {
        return toAjax(apsEquipmentGroupService.deleteApsEquipmentGroupByIds(equipmentGroupIds));
    }
}

