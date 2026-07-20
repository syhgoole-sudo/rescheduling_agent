package com.ruoyi.aps.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Selects an explainable rescheduling policy before RULE or GA solves the schedule.
 * This class only decides scope and priority; it does not execute a scheduling algorithm.
 */
@Component
public class StrategySelector
{
    public static final String LOCAL_RESCHEDULE = "LOCAL_RESCHEDULE";
    public static final String GLOBAL_RESCHEDULE = "GLOBAL_RESCHEDULE";
    public static final String INSERT_PRIORITY = "INSERT_PRIORITY";
    public static final String MIN_CHANGE = "MIN_CHANGE";

    private static final List<String> SUPPORTED_STRATEGIES = Arrays.asList(
            LOCAL_RESCHEDULE, GLOBAL_RESCHEDULE, INSERT_PRIORITY, MIN_CHANGE);

    public Recommendation select(ImpactFeatures features)
    {
        return select(features, null);
    }

    public Recommendation select(ImpactFeatures features, String requestedStrategy)
    {
        String manualStrategy = normalizeStrategy(requestedStrategy);
        if (manualStrategy != null)
        {
            return buildRecommendation(manualStrategy, features,
                    "调度员基于现场约束人工选择该策略；系统仍保留扰动特征供复核。", true);
        }

        if (features.getCriticalRatio() < 1D)
        {
            return buildRecommendation(INSERT_PRIORITY, features,
                    "Hot Lot 剩余交付时间小于剩余加工时间，存在真实交期延期风险。", false);
        }
        if (features.getImpactRate() > 0.5D || features.getEquipmentImpactRate() > 0.5D)
        {
            return buildRecommendation(GLOBAL_RESCHEDULE, features,
                    "受影响 Lot-Step 或 Tool 比例超过 50%，局部调整可能无法覆盖主要资源冲突。", false);
        }
        if (features.isStabilityRequired() && features.getImpactRate() < 0.5D)
        {
            return buildRecommendation(MIN_CHANGE, features,
                    "影响范围可控且现场启用了稳定性优先，建议限制原计划变更。", false);
        }
        if (features.getImpactRate() < 0.2D && features.getCriticalRatio() >= 1D)
        {
            return buildRecommendation(LOCAL_RESCHEDULE, features,
                    "受影响 Lot-Step 比例低于 20%，且 Hot Lot 剩余交付时间可覆盖剩余加工时间。", false);
        }
        return buildRecommendation(LOCAL_RESCHEDULE, features,
                "扰动未达到全局重调度或插单优先阈值，默认采用局部重调度控制计划扰动。", false);
    }

    private Recommendation buildRecommendation(String strategyCode, ImpactFeatures features,
            String ruleReason, boolean manuallySelected)
    {
        String groupText = features.getAffectedEquipmentGroups().isEmpty()
                ? "相关 Tool Group"
                : String.join("、", features.getAffectedEquipmentGroups());
        String reason = String.format(Locale.ROOT,
                "%s 当前影响任务比例 %.1f%%，影响 Lot 比例 %.1f%%，影响 Tool 比例 %.1f%%，交期紧迫度 %.2f，影响集中于%s。",
                ruleReason, features.getImpactRate() * 100D, features.getLotImpactRate() * 100D,
                features.getEquipmentImpactRate() * 100D, features.getCriticalRatio(), groupText);
        return new Recommendation(strategyCode, strategyName(strategyCode), reason,
                alternativeStrategies(strategyCode), features, manuallySelected);
    }

    private String normalizeStrategy(String strategyCode)
    {
        if (strategyCode == null || strategyCode.trim().isEmpty())
        {
            return null;
        }
        String normalized = strategyCode.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STRATEGIES.contains(normalized))
        {
            throw new IllegalArgumentException("Unsupported rescheduling strategy: " + strategyCode);
        }
        return normalized;
    }

    private List<String> alternativeStrategies(String selected)
    {
        List<String> result = new ArrayList<>();
        for (String strategy : SUPPORTED_STRATEGIES)
        {
            if (!strategy.equals(selected))
            {
                result.add(strategy);
            }
        }
        return result;
    }

    public static String strategyName(String strategyCode)
    {
        if (GLOBAL_RESCHEDULE.equals(strategyCode))
        {
            return "全局重调度";
        }
        if (INSERT_PRIORITY.equals(strategyCode))
        {
            return "插单优先策略";
        }
        if (MIN_CHANGE.equals(strategyCode))
        {
            return "最小变更策略";
        }
        return "局部重调度";
    }

    public static class ImpactFeatures
    {
        private double impactRate;
        private double lotImpactRate;
        private double criticalRatio;
        private double equipmentImpactRate;
        private int insertPriority;
        private double changeRate;
        private long remainingDeliveryMinutes;
        private long remainingProcessingMinutes;
        private boolean stabilityRequired;
        private List<String> affectedEquipmentGroups = new ArrayList<>();

        public Map<String, Object> toMap()
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskRate", impactRate);
            result.put("impactRate", impactRate);
            result.put("lotRate", lotImpactRate);
            result.put("lotImpactRate", lotImpactRate);
            result.put("criticalRatio", criticalRatio);
            result.put("equipmentImpactRate", equipmentImpactRate);
            result.put("insertPriority", insertPriority);
            result.put("changeRate", changeRate);
            result.put("remainingDeliveryMinutes", remainingDeliveryMinutes);
            result.put("remainingProcessingMinutes", remainingProcessingMinutes);
            result.put("stabilityRequired", stabilityRequired);
            result.put("affectedEquipmentGroups", affectedEquipmentGroups);
            return result;
        }

        public double getImpactRate() { return impactRate; }
        public void setImpactRate(double impactRate) { this.impactRate = impactRate; }
        public double getLotImpactRate() { return lotImpactRate; }
        public void setLotImpactRate(double lotImpactRate) { this.lotImpactRate = lotImpactRate; }
        public double getCriticalRatio() { return criticalRatio; }
        public void setCriticalRatio(double criticalRatio) { this.criticalRatio = criticalRatio; }
        public double getEquipmentImpactRate() { return equipmentImpactRate; }
        public void setEquipmentImpactRate(double equipmentImpactRate) { this.equipmentImpactRate = equipmentImpactRate; }
        public int getInsertPriority() { return insertPriority; }
        public void setInsertPriority(int insertPriority) { this.insertPriority = insertPriority; }
        public double getChangeRate() { return changeRate; }
        public void setChangeRate(double changeRate) { this.changeRate = changeRate; }
        public long getRemainingDeliveryMinutes() { return remainingDeliveryMinutes; }
        public void setRemainingDeliveryMinutes(long remainingDeliveryMinutes) { this.remainingDeliveryMinutes = remainingDeliveryMinutes; }
        public long getRemainingProcessingMinutes() { return remainingProcessingMinutes; }
        public void setRemainingProcessingMinutes(long remainingProcessingMinutes) { this.remainingProcessingMinutes = remainingProcessingMinutes; }
        public boolean isStabilityRequired() { return stabilityRequired; }
        public void setStabilityRequired(boolean stabilityRequired) { this.stabilityRequired = stabilityRequired; }
        public List<String> getAffectedEquipmentGroups() { return affectedEquipmentGroups; }
        public void setAffectedEquipmentGroups(List<String> affectedEquipmentGroups)
        {
            this.affectedEquipmentGroups = affectedEquipmentGroups == null ? new ArrayList<>() : affectedEquipmentGroups;
        }
    }

    public static class Recommendation
    {
        private final String strategyCode;
        private final String strategyName;
        private final String strategyReason;
        private final List<String> alternativeStrategies;
        private final ImpactFeatures impactFeatures;
        private final boolean manuallySelected;

        Recommendation(String strategyCode, String strategyName, String strategyReason,
                List<String> alternativeStrategies, ImpactFeatures impactFeatures, boolean manuallySelected)
        {
            this.strategyCode = strategyCode;
            this.strategyName = strategyName;
            this.strategyReason = strategyReason;
            this.alternativeStrategies = alternativeStrategies;
            this.impactFeatures = impactFeatures;
            this.manuallySelected = manuallySelected;
        }

        public Map<String, Object> toMap()
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("strategyCode", strategyCode);
            result.put("strategyType", strategyCode);
            result.put("strategyName", strategyName);
            result.put("strategyReason", strategyReason);
            result.put("recommendedReason", strategyReason);
            result.put("impactFeatures", impactFeatures.toMap());
            result.put("alternativeStrategies", alternativeStrategies);
            result.put("manuallySelected", manuallySelected);
            result.put("freezePolicy", freezePolicy(strategyCode));
            result.put("insertPolicy", insertPolicy(strategyCode));
            result.put("objective", objective(strategyCode));
            return result;
        }

        public String getStrategyCode() { return strategyCode; }

        private String freezePolicy(String code)
        {
            if (GLOBAL_RESCHEDULE.equals(code)) return "源方案全部未完成任务进入可调整集合";
            if (MIN_CHANGE.equals(code)) return "冻结未受影响任务，仅开放必要任务调整";
            return "未受影响任务保持不变，受影响任务进入可调整集合";
        }

        private String insertPolicy(String code)
        {
            return INSERT_PRIORITY.equals(code)
                    ? "Hot Lot steps 以交期保障为首要排序目标"
                    : "Hot Lot steps 加入可调整集合并按优先级参与排序";
        }

        private String objective(String code)
        {
            if (GLOBAL_RESCHEDULE.equals(code)) return "在大范围资源冲突下重新优化整体计划";
            if (MIN_CHANGE.equals(code)) return "最小化相对原计划的任务变更与后移";
            if (INSERT_PRIORITY.equals(code)) return "优先降低 Hot Lot 真实交期延期风险";
            return "保障 Hot Lot 交期并控制局部计划扰动";
        }
    }
}
