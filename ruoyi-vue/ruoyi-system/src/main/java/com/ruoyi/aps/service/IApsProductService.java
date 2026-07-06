package com.ruoyi.aps.service;

import java.util.List;
import com.ruoyi.aps.domain.ApsProduct;

/**
 * 产品Service接口
 *
 * @author aps
 */
public interface IApsProductService
{
    public ApsProduct selectApsProductById(Long productId);

    public List<ApsProduct> selectApsProductList(ApsProduct apsProduct);

    public int insertApsProduct(ApsProduct apsProduct);

    public int updateApsProduct(ApsProduct apsProduct);

    public int deleteApsProductByIds(Long[] productIds);

    public int deleteApsProductById(Long productId);
}
