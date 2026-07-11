from __future__ import annotations

from fastapi import Depends, FastAPI, Query
from fastapi.responses import JSONResponse

from lottery_crawler.domain.models import HistoryPage
from lottery_crawler.sources.sporttery.client import SportteryClient, SportteryFetchError


app = FastAPI(title="lottery-crawler", version="0.1.0")


def get_sporttery_client() -> SportteryClient:
    """创建中国体彩网客户端，便于测试时替换为假客户端。"""
    return SportteryClient()


@app.get("/health")
def health() -> dict[str, str]:
    """返回服务健康状态，供本地联调和部署探活使用。"""
    return {"status": "ok"}


@app.get("/api/crawler/dlt/latest")
def get_latest_draw(
    client: SportteryClient = Depends(get_sporttery_client),
) -> JSONResponse:
    """抓取最新一期大乐透开奖，并返回标准化 JSON。"""
    try:
        page = client.fetch_history_page(page_no=1, page_size=5)
    except SportteryFetchError as exc:
        return _upstream_error(str(exc))

    if not page.draws:
        return _empty_result_error()

    return JSONResponse(content={"draw": page.draws[0].to_dict()})


@app.get("/api/crawler/dlt/history-page")
def get_history_page(
    page_no: int = Query(1, alias="pageNo", ge=1),
    page_size: int = Query(5, alias="pageSize", ge=1, le=50),
    client: SportteryClient = Depends(get_sporttery_client),
) -> JSONResponse:
    """抓取一页大乐透历史开奖，并返回标准化分页 JSON。"""
    try:
        page = client.fetch_history_page(page_no=page_no, page_size=page_size)
    except SportteryFetchError as exc:
        return _upstream_error(str(exc))

    return JSONResponse(content=page.to_dict())


def _upstream_error(message: str) -> JSONResponse:
    """将上游抓取失败转换为稳定错误响应。"""
    return JSONResponse(
        status_code=502,
        content={
            "code": "UPSTREAM_FETCH_FAILED",
            "message": message,
        },
    )


def _empty_result_error() -> JSONResponse:
    """将上游空结果转换为稳定错误响应。"""
    return JSONResponse(
        status_code=502,
        content={
            "code": "UPSTREAM_EMPTY_RESULT",
            "message": "Sporttery returned an empty draw list",
        },
    )
