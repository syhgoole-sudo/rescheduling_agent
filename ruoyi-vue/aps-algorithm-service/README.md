# APS Algorithm Service

MVP Python FastAPI service for initial rule-based scheduling.

## Start

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## API

- `POST /api/schedule/initial`
- `GET /health`
