FROM python:3.14-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/src
ENV PATH=/app/.venv/bin:$PATH

WORKDIR /app

RUN pip install --no-cache-dir uv

COPY lottery-crawler/pyproject.toml lottery-crawler/uv.lock ./

RUN uv sync --frozen --no-dev --no-install-project

COPY lottery-crawler/src ./src

EXPOSE 8000

CMD ["uvicorn", "lottery_crawler.api.app:app", "--host", "0.0.0.0", "--port", "8000"]
