from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal


@dataclass(frozen=True)
class PrizeTier:
    """上游数据源奖级信息的标准化结果。"""

    name: str  # 奖级名称，例如“一等奖”“一等奖(追加)”。
    stake_count: int  # 当前奖级的中奖注数。
    stake_amount: Decimal | None  # 当前奖级单注奖金；未知或上游返回“---”时为空。
    total_prize_amount: Decimal | None  # 当前奖级总奖金；未知时为空。
    sort: int | None  # 上游返回的奖级排序值，用于保持官方展示顺序。
    group: str | None  # 上游返回的奖级分组编码，用于后续追溯原始奖级。

    def to_dict(self) -> dict[str, object]:
        """返回可直接序列化为 JSON 的字典，Decimal 统一转为字符串。"""
        return {
            "name": self.name,
            "stakeCount": self.stake_count,
            "stakeAmount": _decimal_to_string(self.stake_amount),
            "totalPrizeAmount": _decimal_to_string(self.total_prize_amount),
            "sort": self.sort,
            "group": self.group,
        }


@dataclass(frozen=True)
class DrawResult:
    """单期大乐透开奖结果的标准化结果。"""

    lottery_type: str  # 彩票类型编码，MVP 固定为 DLT。
    issue_no: str  # 开奖期号，按字符串保存以避免前导零或格式变化问题。
    draw_date: str  # 开奖日期，格式为 YYYY-MM-DD。
    front_numbers: list[int]  # 大乐透前区号码，共 5 个，范围 1-35。
    back_numbers: list[int]  # 大乐透后区号码，共 2 个，范围 1-12。
    pool_balance: Decimal | None  # 本期开奖后的奖池金额；上游缺失时为空。
    sales_amount: Decimal | None  # 本期销售金额；上游缺失时为空。
    prize_tiers: list[PrizeTier]  # 本期奖级明细列表，顺序沿用官方返回顺序。
    source: str  # 数据来源地址，用于排查和追溯。
    pdf_url: str | None  # 官方开奖公告 PDF 地址；上游未提供时为空。

    def to_dict(self) -> dict[str, object]:
        """返回面向后端集成的 JSON 友好字典。"""
        return {
            "lotteryType": self.lottery_type,
            "issueNo": self.issue_no,
            "drawDate": self.draw_date,
            "frontNumbers": self.front_numbers,
            "backNumbers": self.back_numbers,
            "poolBalance": _decimal_to_string(self.pool_balance),
            "salesAmount": _decimal_to_string(self.sales_amount),
            "prizeTiers": [tier.to_dict() for tier in self.prize_tiers],
            "source": self.source,
            "pdfUrl": self.pdf_url,
        }


@dataclass(frozen=True)
class HistoryPage:
    """中国体彩网分页开奖结果的标准化结果。"""

    page_no: int  # 当前页码。
    page_size: int  # 每页条数。
    pages: int  # 总页数。
    total: int  # 符合查询条件的总记录数。
    draws: list[DrawResult]  # 当前页标准化后的开奖列表。

    def to_dict(self) -> dict[str, object]:
        """返回面向探针输出的 JSON 友好字典。"""
        return {
            "pageNo": self.page_no,
            "pageSize": self.page_size,
            "pages": self.pages,
            "total": self.total,
            "draws": [draw.to_dict() for draw in self.draws],
        }


def _decimal_to_string(value: Decimal | None) -> str | None:
    if value is None:
        return None
    return format(value, "f")
