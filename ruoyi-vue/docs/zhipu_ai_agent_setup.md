# 智谱 AI 分析报告配置说明

## 1. 功能说明

系统新增“AI 分析报告”能力，用于基于已有插单事件、影响分析、策略推荐、KPI 对比和半导体可重入上下文，生成调度员可读的自然语言解释报告。

AI 只做解释和辅助决策，不参与排产，不生成任务时间，不分配设备，不修改方案状态，也不直接确认或拒绝方案。

## 2. API Key 安全要求

智谱 API Key 必须通过环境变量读取：

```text
ZHIPU_API_KEY
```

不要将真实 Key 写入：

- Java 源码
- Vue 源码
- Python 源码
- 数据库
- 文档
- Git 仓库

## 3. Windows PowerShell 配置方式

临时设置，仅当前 PowerShell 窗口有效：

```powershell
$env:ZHIPU_API_KEY="你的智谱APIKey"
```

长期设置：

```powershell
setx ZHIPU_API_KEY "你的智谱APIKey"
```

长期设置后，需要重新打开 PowerShell 或重启服务进程，Python FastAPI 才能读取到新环境变量。

## 4. 启动顺序

1. 设置 `ZHIPU_API_KEY` 环境变量。
2. 启动 Python FastAPI：

```powershell
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue\aps-algorithm-service
python -m uvicorn app.main:app --reload --port 8000
```

3. 启动若依后端。
4. 启动若依前端。

## 5. Fallback 机制

如果出现以下情况，系统会自动使用规则模板报告：

- 未设置 `ZHIPU_API_KEY`
- API Key 为空
- 智谱接口请求失败
- 智谱接口超时
- 模型返回内容无法解析
- Python Agent 服务不可用时，Java 后端使用已有规则模板解释报告兜底

Fallback 不影响调度主流程。

## 6. 接口

Python Agent 接口：

```text
POST /api/agent/explain-reschedule
```

若依后端接口：

```text
GET /aps/insertEvent/aiExplain/{eventId}
```

前端入口：

```text
插单事件 -> AI 分析报告
```
