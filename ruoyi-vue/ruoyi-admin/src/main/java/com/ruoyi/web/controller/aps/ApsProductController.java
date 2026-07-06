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
import com.ruoyi.aps.domain.ApsProduct;
import com.ruoyi.aps.service.IApsProductService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 产品Controller
 *
 * @author aps
 */
@RestController
@RequestMapping("/aps/product")
public class ApsProductController extends BaseController
{
    @Autowired
    private IApsProductService apsProductService;

    @PreAuthorize("@ss.hasPermi('aps:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApsProduct apsProduct)
    {
        startPage();
        List<ApsProduct> list = apsProductService.selectApsProductList(apsProduct);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aps:product:export')")
    @Log(title = "产品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApsProduct apsProduct)
    {
        List<ApsProduct> list = apsProductService.selectApsProductList(apsProduct);
        ExcelUtil<ApsProduct> util = new ExcelUtil<ApsProduct>(ApsProduct.class);
        util.exportExcel(response, list, "产品数据");
    }

    @PreAuthorize("@ss.hasPermi('aps:product:list')")
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable("productId") Long productId)
    {
        return success(apsProductService.selectApsProductById(productId));
    }

    @PreAuthorize("@ss.hasPermi('aps:product:add')")
    @Log(title = "产品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ApsProduct apsProduct)
    {
        apsProduct.setCreateBy(getUsername());
        return toAjax(apsProductService.insertApsProduct(apsProduct));
    }

    @PreAuthorize("@ss.hasPermi('aps:product:edit')")
    @Log(title = "产品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ApsProduct apsProduct)
    {
        apsProduct.setUpdateBy(getUsername());
        return toAjax(apsProductService.updateApsProduct(apsProduct));
    }

    @PreAuthorize("@ss.hasPermi('aps:product:remove')")
    @Log(title = "产品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(apsProductService.deleteApsProductByIds(productIds));
    }
}

