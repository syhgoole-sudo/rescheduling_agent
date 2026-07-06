from fastapi import FastAPI

from app.schemas.agent_schema import AgentExplainRequest, AgentExplainResponse
from app.schemas.schedule_schema import InitialScheduleRequest, InitialScheduleResponse, LocalRescheduleRequest, LocalRescheduleResponse
from app.services.agent_explain_service import explain_reschedule
from app.services.initial_schedule_service import generate_initial_schedule
from app.services.local_reschedule_service import generate_local_reschedule


app = FastAPI(title="APS Algorithm Service", version="0.1.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/api/schedule/initial", response_model=InitialScheduleResponse)
def initial_schedule(request: InitialScheduleRequest):
    return generate_initial_schedule(request)


@app.post("/api/schedule/reschedule/local", response_model=LocalRescheduleResponse)
def local_reschedule(request: LocalRescheduleRequest):
    return generate_local_reschedule(request)


@app.post("/api/agent/explain-reschedule", response_model=AgentExplainResponse)
def agent_explain_reschedule(request: AgentExplainRequest):
    return explain_reschedule(request)
