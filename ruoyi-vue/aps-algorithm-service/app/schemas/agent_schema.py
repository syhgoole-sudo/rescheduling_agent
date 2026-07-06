from typing import Any, Dict

from pydantic import BaseModel


class AgentExplainRequest(BaseModel):
    event: Dict[str, Any] = {}
    insertOrder: Dict[str, Any] = {}
    impact: Dict[str, Any] = {}
    strategyType: str = ""
    sourcePlan: Dict[str, Any] = {}
    newPlan: Dict[str, Any] = {}
    kpiCompare: Dict[str, Any] = {}
    semiconductorContext: Dict[str, Any] = {}
    planStatus: str = ""


class AgentExplainResponse(BaseModel):
    eventSummary: str
    impactAnalysis: str
    strategyExplanation: str
    kpiInterpretation: str
    riskWarning: str
    recommendation: str
    fullReport: str
    modelName: str
    fallbackUsed: bool
