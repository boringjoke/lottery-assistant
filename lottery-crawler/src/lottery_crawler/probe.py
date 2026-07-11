"""兼容入口：支持 `python -m lottery_crawler.probe`。"""

from lottery_crawler.cli.probe import main


if __name__ == "__main__":
    raise SystemExit(main())
