from __future__ import annotations

import json
from decimal import Decimal, InvalidOperation
from typing import Any

from lottery_crawler.models import DrawResult, HistoryPage, PrizeTier


SPORTTERY_DLT_HISTORY_URL = (
    "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry"
)


class SportteryParseError(ValueError):
    """当中国体彩网响应无法安全标准化时抛出。"""


def parse_history_page(raw_content: str) -> HistoryPage:
    """将中国体彩网历史开奖分页响应解析为标准化开奖数据。"""
    stripped = raw_content.lstrip()
    if not stripped.startswith("{"):
        raise SportteryParseError("Sporttery response is non-JSON content")

    try:
        payload = json.loads(raw_content)
    except json.JSONDecodeError as exc:
        raise SportteryParseError("Sporttery response is invalid JSON") from exc

    value = _expect_dict(payload.get("value"), "value")
    raw_draws = value.get("list") or []
    if not isinstance(raw_draws, list):
        raise SportteryParseError("Sporttery value.list must be a list")

    return HistoryPage(
        page_no=_parse_int(value.get("pageNo"), "pageNo"),
        page_size=_parse_int(value.get("pageSize"), "pageSize"),
        pages=_parse_int(value.get("pages"), "pages"),
        total=_parse_int(value.get("total"), "total"),
        draws=[_parse_draw(item) for item in raw_draws],
    )


def _parse_draw(raw_draw: Any) -> DrawResult:
    draw = _expect_dict(raw_draw, "draw")
    numbers = _parse_draw_numbers(_expect_str(draw.get("lotteryDrawResult"), "lotteryDrawResult"))

    raw_prize_tiers = draw.get("prizeLevelList") or []
    if not isinstance(raw_prize_tiers, list):
        raise SportteryParseError("prizeLevelList must be a list")

    return DrawResult(
        lottery_type="DLT",
        issue_no=_expect_str(draw.get("lotteryDrawNum"), "lotteryDrawNum"),
        draw_date=_expect_str(draw.get("lotteryDrawTime"), "lotteryDrawTime"),
        front_numbers=numbers[:5],
        back_numbers=numbers[5:],
        pool_balance=_parse_money(draw.get("poolBalanceAfterdraw")),
        sales_amount=_parse_money(draw.get("totalSaleAmount")),
        prize_tiers=[_parse_prize_tier(tier) for tier in raw_prize_tiers],
        source=SPORTTERY_DLT_HISTORY_URL,
        pdf_url=_optional_str(draw.get("drawPdfUrl")),
    )


def _parse_prize_tier(raw_tier: Any) -> PrizeTier:
    tier = _expect_dict(raw_tier, "prize tier")
    return PrizeTier(
        name=_expect_str(tier.get("prizeLevel"), "prizeLevel"),
        stake_count=_parse_int(tier.get("stakeCount"), "stakeCount"),
        stake_amount=_parse_money(tier.get("stakeAmount")),
        total_prize_amount=_parse_money(tier.get("totalPrizeamount")),
        sort=_parse_optional_int(tier.get("sort"), "sort"),
        group=_optional_str(tier.get("group")),
    )


def _parse_draw_numbers(value: str) -> list[int]:
    parts = value.split()
    if len(parts) != 7:
        raise SportteryParseError("lotteryDrawResult must contain 7 numbers")

    try:
        numbers = [int(part) for part in parts]
    except ValueError as exc:
        raise SportteryParseError("lotteryDrawResult contains non-numeric values") from exc

    front_numbers = numbers[:5]
    back_numbers = numbers[5:]
    if any(number < 1 or number > 35 for number in front_numbers):
        raise SportteryParseError("front numbers must be between 1 and 35")
    if any(number < 1 or number > 12 for number in back_numbers):
        raise SportteryParseError("back numbers must be between 1 and 12")
    return numbers


def _parse_money(value: Any) -> Decimal | None:
    if value is None:
        return None
    text = str(value).strip()
    if text in {"", "---", "-1"}:
        return None
    normalized = text.replace(",", "")
    try:
        return Decimal(normalized)
    except InvalidOperation as exc:
        raise SportteryParseError(f"invalid money value: {text}") from exc


def _parse_int(value: Any, field_name: str) -> int:
    text = _expect_str(value, field_name).replace(",", "")
    try:
        return int(text)
    except ValueError as exc:
        raise SportteryParseError(f"{field_name} must be an integer") from exc


def _parse_optional_int(value: Any, field_name: str) -> int | None:
    if value is None or value == "":
        return None
    return _parse_int(value, field_name)


def _expect_dict(value: Any, field_name: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise SportteryParseError(f"{field_name} must be an object")
    return value


def _expect_str(value: Any, field_name: str) -> str:
    if value is None:
        raise SportteryParseError(f"{field_name} is required")
    text = str(value).strip()
    if not text:
        raise SportteryParseError(f"{field_name} is required")
    return text


def _optional_str(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None
