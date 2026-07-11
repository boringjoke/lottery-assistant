from __future__ import annotations

import httpx

from lottery_crawler.domain.models import HistoryPage
from lottery_crawler.sources.sporttery.parser import (
    SPORTTERY_DLT_HISTORY_URL,
    SportteryParseError,
    parse_history_page,
)


class SportteryFetchError(RuntimeError):
    """当中国体彩网数据源无法安全抓取时抛出。"""


class SportteryClient:
    """用于访问中国体彩网大乐透历史开奖接口的 HTTP 客户端。"""

    DEFAULT_HEADERS = {
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/126.0.0.0 Safari/537.36"
        ),
        "Accept": "application/json, text/plain, */*",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Referer": "https://static.sporttery.cn/",
        "Origin": "https://static.sporttery.cn",
    }

    def __init__(
        self,
        *,
        base_url: str = SPORTTERY_DLT_HISTORY_URL,
        timeout: float = 20.0,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        """使用适合受保护数据源的保守默认值创建客户端。"""
        self._base_url = base_url
        self._timeout = timeout
        self._transport = transport

    def fetch_history_page(self, *, page_no: int = 1, page_size: int = 5) -> HistoryPage:
        """抓取并解析一页中国体彩网历史开奖数据。"""
        params = {
            "gameNo": "85",
            "provinceId": "0",
            "isVerify": "1",
            "pageSize": str(page_size),
            "pageNo": str(page_no),
        }

        with httpx.Client(
            headers=self.DEFAULT_HEADERS,
            timeout=self._timeout,
            transport=self._transport,
            follow_redirects=True,
        ) as client:
            try:
                response = client.get(self._base_url, params=params)
            except httpx.RequestError as exc:
                raise SportteryFetchError(f"Sporttery request failed: {exc}") from exc

        if response.status_code != 200:
            raise SportteryFetchError(
                f"Sporttery returned HTTP {response.status_code}"
            )

        raw_text = response.text
        if not raw_text.lstrip().startswith("{"):
            raise SportteryFetchError("Sporttery returned non-JSON content")

        try:
            return parse_history_page(raw_text)
        except SportteryParseError as exc:
            raise SportteryFetchError(f"Sporttery response parse failed: {exc}") from exc
