"""FastAPI 服务入口。"""

from lottery_crawler.api.app import app, get_sporttery_client

__all__ = ["app", "get_sporttery_client"]

