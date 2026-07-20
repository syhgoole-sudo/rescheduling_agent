import json
import os
import urllib.error
import urllib.request
from typing import Any, Dict

from app.schemas.agent_schema import AgentExplainRequest, AgentExplainResponse


ZHIPU_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
DEFAULT_MODEL = "glm-4-flash"


def explain_reschedule(request: AgentExplainRequest) -> AgentExplainResponse:
    fallback = _build_fallback_report(request)
    api_key = os.getenv("ZHIPU_API_KEY", "").strip()
    if not api_key:
        print("ZHIPU_API_KEY loaded: false")
        return fallback
    print("ZHIPU_API_KEY loaded: true")

    try:
        prompt = _build_prompt(request)
        payload = {
            "model": DEFAULT_MODEL,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是半导体可重入插单重调度解释助手。你不能生成排产方案，不能修改任务时间，"
                        "不能分配设备，不能绕过调度算法，不能直接确认或拒绝方案。你只能基于输入的结构化数据"
                        "解释影响范围、策略原因、KPI 变化、方案收益、方案代价和调度建议。"
                    ),
                },
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.2,
        }
        http_request = urllib.request.Request(
            ZHIPU_ENDPOINT,
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        with urllib.request.urlopen(http_request, timeout=15) as response:
            body = json.loads(response.read().decode("utf-8"))
        content = body["choices"][0]["message"]["content"]
        parsed = _parse_model_json(content)
        return AgentExplainResponse(
            eventSummary=parsed.get("eventSummary") or fallback.eventSummary,
            impactAnalysis=parsed.get("impactAnalysis") or fallback.impactAnalysis,
            strategyExplanation=parsed.get("strategyExplanation") or fallback.strategyExplanation,
            kpiInterpretation=parsed.get("kpiInterpretation") or fallback.kpiInterpretation,
            riskWarning=parsed.get("riskWarning") or fallback.riskWarning,
            recommendation=parsed.get("recommendation") or fallback.recommendation,
            fullReport=parsed.get("fullReport") or content,
            modelName=DEFAULT_MODEL,
            fallbackUsed=False,
        )
    except TimeoutError:
        print("Zhipu call timeout, use fallback")
        return fallback
    except Exception:
        print("Zhipu call failed, use fallback")
        return fallback


def _build_prompt(request: AgentExplainRequest) -> str:
    data = request.dict()
    return (
        "请基于以下结构化数据生成 JSON 格式的插单重调度解释报告。\n"
        "输出字段必须包括：eventSummary, impactAnalysis, strategyExplanation, "
        "kpiInterpretation, riskWarning, recommendation, fullReport。\n"
        "要求：专业、简洁、基于数据，不夸大；必须同时说明收益和代价；"
        "strategyExplanation 必须引用 strategyRecommendation 中的扰动特征和 strategyReason，说明为什么推荐当前策略；"
        "如果插单按期完成要说明；如果延期订单数、总延期或 makespan 增加，也必须说明；"
        "结合半导体可重入语境解释 lot、step、tool group、PHOTO 重入、hot lot。\n"
        "结构化数据：\n"
        + json.dumps(data, ensure_ascii=False, indent=2, default=str)
    )


def _parse_model_json(content: str) -> Dict[str, Any]:
    text = content.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.lower().startswith("json"):
            text = text[4:].strip()
    return json.loads(text)


def _build_fallback_report(request: AgentExplainRequest) -> AgentExplainResponse:
    event = request.event or {}
    insert_order = request.insertOrder or {}
    impact = request.impact or {}
    strategy = impact.get("strategyRecommendation") or impact.get("strategy") or {}
    features = strategy.get("impactFeatures") or impact.get("impactFeatures") or {}
    kpi = request.kpiCompare or {}
    summary = kpi.get("summary") or {}
    delay = kpi.get("trueDelayCompare") or kpi.get("delayCompare") or {}
    makespan = kpi.get("makespanCompare") or {}
    insert_compare = kpi.get("insertOrderCompare") or {}

    event_summary = (
        f"本次 hot lot 插单事件为 {event.get('eventCode', '')}，插单 lot 为 "
        f"{insert_order.get('orderCode', '')}，产品/工艺族为 {insert_order.get('productCode', '')}，"
        f"优先级为 {insert_order.get('priorityLevel', '')}。"
    )
    impact_analysis = (
        f"系统识别影响等级为 {impact.get('impactLevel', 'UNKNOWN')}，受影响 lot-step "
        f"{impact.get('affectedTaskCount', 0)} 个，受影响 lot {impact.get('affectedOrderCount', 0)} 个，"
        f"受影响 tool {impact.get('affectedEquipmentCount', 0)} 台。"
    )
    strategy_code = strategy.get("strategyCode") or strategy.get("strategyType") or request.strategyType
    strategy_name = strategy.get("strategyName") or strategy_code
    strategy_reason = strategy.get("strategyReason") or strategy.get("recommendedReason") or "未保存详细规则命中原因"
    scope_text = {
        "GLOBAL_RESCHEDULE": "源方案全部未完成 lot-step 进入可调整集合，以覆盖大范围 Tool 冲突",
        "INSERT_PRIORITY": "Hot Lot steps 以真实交期保障为首要目标，其他任务按影响范围受控调整",
        "MIN_CHANGE": "未受影响任务保持冻结，仅开放必要 lot-step 调整，以降低计划稳定性扰动",
        "LOCAL_RESCHEDULE": "仅调整受影响 lot-step 和 Hot Lot steps，未受影响任务保持冻结",
    }.get(strategy_code, "按已识别影响范围调整相关 lot-step")
    strategy_explanation = (
        f"系统推荐 {strategy_name}（{strategy_code}）。推荐依据：{strategy_reason}"
        f"扰动特征为影响任务比例 {_format_rate(features.get('taskRate'))}、"
        f"影响 Lot 比例 {_format_rate(features.get('lotRate'))}、"
        f"影响 Tool 比例 {_format_rate(features.get('equipmentImpactRate'))}、"
        f"交期紧迫度 {_format_number(features.get('criticalRatio'))}。执行范围：{scope_text}。"
        "该推荐只用于辅助调度员决策，不替代人工确认。"
    )
    kpi_interpretation = (
        f"新方案任务数 {summary.get('newTaskCount', 0)}，变更任务数 {summary.get('changedTaskCount', 0)}，"
        f"插单任务数 {summary.get('insertedTaskCount', 0)}，冻结任务数 {summary.get('frozenTaskCount', 0)}。"
        f"真实延期 Lot 数变化 {delay.get('trueDelayOrderCountDiff', delay.get('delayOrderCountDiff', 0))}，"
        f"真实总延期分钟变化 {delay.get('trueTotalDelayMinutesDiff', delay.get('totalDelayMinutesDiff', 0))}，"
        f"makespan 变化 {makespan.get('makespanDiffMinutes', 0)} 分钟。"
    )
    insert_delay = int(insert_compare.get("insertOrderTrueDelayMinutes") or insert_compare.get("insertOrderDelayMinutes") or 0)
    risk_warning = "插单 lot 可按期完成。" if insert_delay == 0 else f"插单 lot 仍延期 {insert_delay} 分钟，需要调度员关注。"
    if float(summary.get("changedTaskRatio") or 0) > 0.5:
        risk_warning += " 同时，新方案变更任务比例较高，说明计划扰动较大。"
    recommendation = "建议采用，但需关注计划扰动。" if insert_delay == 0 else "谨慎采用，建议继续评估 hot lot 交期风险。"
    full_report = "\n\n".join([
        event_summary,
        impact_analysis,
        strategy_explanation,
        kpi_interpretation,
        "风险提示：" + risk_warning,
        "调度建议：" + recommendation,
    ])
    return AgentExplainResponse(
        eventSummary=event_summary,
        impactAnalysis=impact_analysis,
        strategyExplanation=strategy_explanation,
        kpiInterpretation=kpi_interpretation,
        riskWarning=risk_warning,
        recommendation=recommendation,
        fullReport=full_report,
        modelName="fallback-template",
        fallbackUsed=True,
    )


def _format_rate(value: Any) -> str:
    try:
        return f"{float(value) * 100:.1f}%"
    except (TypeError, ValueError):
        return "未知"


def _format_number(value: Any) -> str:
    try:
        return f"{float(value):.2f}"
    except (TypeError, ValueError):
        return "未知"
