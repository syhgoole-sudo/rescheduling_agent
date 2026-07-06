package com.ruoyi.aps.mapper;

import java.util.List;
import com.ruoyi.aps.domain.ApsProduct;

/**
 * 产品Mapper接口
 *
 * @author aps
 */
public interface ApsProductMapper
{
    public ApsProduct selectApsProductById(Long productId);

    public List<ApsProduct> selectApsProductList(ApsProduct apsProduct);

    public int insertApsProduct(ApsProduct apsProduct);

    public int updateApsProduct(ApsProduct apsProduct);

    public int deleteApsProductById(Long productId);

    public int deleteApsProductByIds(Long[] productIds);
}
