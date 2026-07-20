package com.ruoyi.aps.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.ruoyi.aps.strategy.StrategySelector.ImpactFeatures;

class StrategySelectorTest
{
    private final StrategySelector selector = new StrategySelector();

    @Test
    void smallImpactSelectsLocalReschedule()
    {
        assertEquals(StrategySelector.LOCAL_RESCHEDULE,
                selector.select(features(0.12D, 0.10D, 1.5D, 0.20D, false)).getStrategyCode());
    }

    @Test
    void largeImpactSelectsGlobalReschedule()
    {
        assertEquals(StrategySelector.GLOBAL_RESCHEDULE,
                selector.select(features(0.60D, 0.40D, 1.5D, 0.30D, false)).getStrategyCode());
    }

    @Test
    void infeasibleDueWindowSelectsInsertPriority()
    {
        assertEquals(StrategySelector.INSERT_PRIORITY,
                selector.select(features(0.65D, 0.50D, 0.80D, 0.60D, false)).getStrategyCode());
    }

    @Test
    void stabilitySensitiveDisturbanceSelectsMinChange()
    {
        assertEquals(StrategySelector.MIN_CHANGE,
                selector.select(features(0.15D, 0.10D, 1.8D, 0.20D, true)).getStrategyCode());
    }

    private ImpactFeatures features(double taskRate, double lotRate, double criticalRatio,
            double equipmentRate, boolean stabilityRequired)
    {
        ImpactFeatures result = new ImpactFeatures();
        result.setImpactRate(taskRate);
        result.setLotImpactRate(lotRate);
        result.setCriticalRatio(criticalRatio);
        result.setEquipmentImpactRate(equipmentRate);
        result.setChangeRate(taskRate);
        result.setInsertPriority(1);
        result.setRemainingDeliveryMinutes(180L);
        result.setRemainingProcessingMinutes(120L);
        result.setStabilityRequired(stabilityRequired);
        return result;
    }
}
