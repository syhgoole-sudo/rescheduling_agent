package com.ruoyi.aps.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.aps.mapper.ApsProductMapper;
import com.ruoyi.aps.domain.ApsProduct;
import com.ruoyi.aps.service.IApsProductService;

/**
 * 产品Service业务层处理
 *
 * @author aps
 */
@Service
public class ApsProductServiceImpl implements IApsProductService
{
    @Autowired
    private ApsProductMapper apsProductMapper;

    @Override
    public ApsProduct selectApsProductById(Long productId)
    {
        return apsProductMapper.selectApsProductById(productId);
    }

    @Override
    public List<ApsProduct> selectApsProductList(ApsProduct apsProduct)
    {
        return apsProductMapper.selectApsProductList(apsProduct);
    }

    @Override
    public int insertApsProduct(ApsProduct apsProduct)
    {
        return apsProductMapper.insertApsProduct(apsProduct);
    }

    @Override
    public int updateApsProduct(ApsProduct apsProduct)
    {
        return apsProductMapper.updateApsProduct(apsProduct);
    }

    @Override
    public int deleteApsProductByIds(Long[] productIds)
    {
        return apsProductMapper.deleteApsProductByIds(productIds);
    }

    @Override
    public int deleteApsProductById(Long productId)
    {
        return apsProductMapper.deleteApsProductById(productId);
    }
}
