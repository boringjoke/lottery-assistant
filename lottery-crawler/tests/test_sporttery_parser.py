import json
import sys
import unittest
from decimal import Decimal
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
sys.path.insert(0, str(SRC_ROOT))

from lottery_crawler.sporttery_parser import SportteryParseError, parse_history_page


FIXTURE_DIR = Path(__file__).resolve().parent / "fixtures"


class SportteryParserTest(unittest.TestCase):
    def test_parse_history_page_extracts_draws_and_prize_tiers(self):
        payload = (FIXTURE_DIR / "sporttery_history_page_success.json").read_text(
            encoding="utf-8"
        )

        page = parse_history_page(payload)

        self.assertEqual(1, page.page_no)
        self.assertEqual(2, page.page_size)
        self.assertEqual(2894, page.total)
        self.assertEqual(2, len(page.draws))

        latest = page.draws[0]
        self.assertEqual("26076", latest.issue_no)
        self.assertEqual("2026-07-08", latest.draw_date)
        self.assertEqual([15, 20, 27, 28, 35], latest.front_numbers)
        self.assertEqual([2, 11], latest.back_numbers)
        self.assertEqual(Decimal("804904836.99"), latest.pool_balance)
        self.assertEqual(Decimal("303115587"), latest.sales_amount)
        self.assertEqual("https://pdf.sporttery.cn/33800/26076/26076.pdf", latest.pdf_url)
        self.assertEqual(9, len(latest.prize_tiers))

        first_tier = latest.prize_tiers[0]
        self.assertEqual("一等奖", first_tier.name)
        self.assertEqual(4, first_tier.stake_count)
        self.assertEqual(Decimal("10000000"), first_tier.stake_amount)
        self.assertEqual(Decimal("40000000"), first_tier.total_prize_amount)

        add_on_tier = latest.prize_tiers[1]
        self.assertEqual("一等奖(追加)", add_on_tier.name)
        self.assertIsNone(add_on_tier.stake_amount)

    def test_parse_history_page_rejects_html_block_page(self):
        with self.assertRaisesRegex(SportteryParseError, "non-JSON"):
            parse_history_page("<!DOCTYPE html><html>Restricted Access</html>")

    def test_parse_history_page_rejects_invalid_number_count(self):
        payload = json.loads(
            (FIXTURE_DIR / "sporttery_history_page_success.json").read_text(
                encoding="utf-8"
            )
        )
        payload["value"]["list"][0]["lotteryDrawResult"] = "01 02 03"

        with self.assertRaisesRegex(SportteryParseError, "7 numbers"):
            parse_history_page(json.dumps(payload))

    def test_parse_history_page_treats_empty_list_as_empty_page(self):
        payload = {
            "success": True,
            "errorCode": "0",
            "errorMessage": "处理成功",
            "value": {"pageNo": 999999, "pageSize": 5, "pages": 579, "total": 2894, "list": []},
        }

        page = parse_history_page(json.dumps(payload))

        self.assertEqual(999999, page.page_no)
        self.assertEqual(0, len(page.draws))


if __name__ == "__main__":
    unittest.main()
