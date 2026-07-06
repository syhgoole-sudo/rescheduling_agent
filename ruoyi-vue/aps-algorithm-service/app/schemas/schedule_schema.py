from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel


class OrderDTO(BaseModel):
    orderId: int
    orderCode: str
    orderType: str
    productId: int
    productCode: str
    quantity: Optional[float] = None
    priorityLevel: int = 999
    releaseTime: datetime
    dueTime: datetime
    orderStatus: str


class RouteOperationDTO(BaseModel):
    routeOperationId: Optional[int] = None
    productId: int
    productCode: Optional[str] = None
    processSeq: int
    processCode: str
    processName: str
    equipmentGroupId: int
    standardDuration: int


class EquipmentGroupDTO(BaseModel):
    equipmentGroupId: int
    equipmentGroupCode: str
    equipmentGroupName: str
    status: Optional[str] = None


class EquipmentDTO(BaseModel):
    equipmentId: int
    equipmentCode: str
    equipmentName: str
    equipmentGroupId: int
    equipmentGroupCode: Optional[str] = None
    status: Optional[str] = None


class InitialScheduleRequest(BaseModel):
    requestId: str
    scheduleStartTime: datetime
    orders: List[OrderDTO]
    routeOperations: List[RouteOperationDTO]
    equipmentGroups: List[EquipmentGroupDTO] = []
    equipments: List[EquipmentDTO]
    algorithmConfig: Dict[str, Any] = {}


class TaskScheduleDTO(BaseModel):
    orderId: int
    orderCode: str
    productId: int
    productCode: str
    processSeq: int
    processCode: str
    processName: str
    equipmentGroupId: int
    equipmentId: int
    equipmentCode: str
    plannedStartTime: str
    plannedEndTime: str
    duration: int
    isFrozen: str = "N"
    isInserted: str = "N"
    isChanged: str = "N"
    sourceTaskId: Optional[int] = None
    originalStartTime: Optional[str] = None
    originalEndTime: Optional[str] = None
    originalEquipmentId: Optional[int] = None


class InitialScheduleResponse(BaseModel):
    requestId: str
    success: bool
    message: str
    algorithmName: str
    taskSchedules: List[TaskScheduleDTO]
    kpi: Dict[str, Any]
    warnings: List[str]
    runtimeMs: int


class InsertTaskDTO(BaseModel):
    orderId: int
    orderCode: str
    productId: int
    productCode: str
    processSeq: int
    processCode: str
    processName: str
    equipmentGroupId: int
    standardDuration: int


class LocalRescheduleRequest(BaseModel):
    requestId: str
    eventId: int
    sourcePlanId: int
    scheduleStartTime: datetime
    insertOrder: OrderDTO
    frozenTasks: List[TaskScheduleDTO] = []
    adjustableTasks: List[TaskScheduleDTO] = []
    insertTasks: List[InsertTaskDTO] = []
    equipments: List[EquipmentDTO]
    routeOperations: List[RouteOperationDTO] = []
    strategyConfig: Dict[str, Any] = {}


class LocalRescheduleResponse(BaseModel):
    requestId: str
    success: bool
    message: str
    algorithmName: str
    strategyType: str
    taskSchedules: List[TaskScheduleDTO]
    kpi: Dict[str, Any]
    warnings: List[str]
    runtimeMs: int
