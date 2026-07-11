from __future__ import annotations

import argparse
import json
import sys

from lottery_crawler.models import HistoryPage
from lottery_crawler.sporttery_client import SportteryClient, SportteryFetchError


def render_history_page_json(page: HistoryPage) -> str:
    """使用稳定的后端字段名序列化标准化历史分页。"""
    return json.dumps(page.to_dict(), ensure_ascii=False, indent=2)


def render_latest_draw_json(page: HistoryPage) -> str:
    """将历史分页中的第一期开奖序列化为最新开奖探针结果。"""
    latest = page.draws[0] if page.draws else None
    return json.dumps(
        {"draw": latest.to_dict() if latest else None},
        ensure_ascii=False,
        indent=2,
    )


def build_parser() -> argparse.ArgumentParser:
    """构建探针命令行解析器，此过程不执行网络请求。"""
    parser = argparse.ArgumentParser(
        prog="python -m lottery_crawler.probe",
        description="探测中国体彩网大乐透开奖数据，并输出标准化 JSON。",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("latest", help="抓取最新一期大乐透开奖。")

    page_parser = subparsers.add_parser("page", help="抓取一页大乐透历史开奖。")
    page_parser.add_argument("--page-no", type=int, default=1)
    page_parser.add_argument("--page-size", type=int, default=5)

    return parser


def main(argv: list[str] | None = None) -> int:
    """运行探针命令行，并返回进程退出码。"""
    parser = build_parser()
    args = parser.parse_args(argv)
    client = SportteryClient()

    try:
        if args.command == "latest":
            page = client.fetch_history_page(page_no=1, page_size=5)
            print(render_latest_draw_json(page))
            return 0

        if args.command == "page":
            page = client.fetch_history_page(
                page_no=args.page_no,
                page_size=args.page_size,
            )
            print(render_history_page_json(page))
            return 0
    except SportteryFetchError as exc:
        print(f"probe failed: {exc}", file=sys.stderr)
        return 1

    parser.error(f"unsupported command: {args.command}")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
