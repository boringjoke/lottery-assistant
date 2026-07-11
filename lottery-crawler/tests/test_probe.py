import json
import sys
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
sys.path.insert(0, str(SRC_ROOT))

from lottery_crawler.probe import render_history_page_json
from lottery_crawler.sporttery_parser import parse_history_page


FIXTURE_DIR = Path(__file__).resolve().parent / "fixtures"


class ProbeTest(unittest.TestCase):
    def test_render_history_page_json_outputs_stable_backend_contract(self):
        payload = (FIXTURE_DIR / "sporttery_history_page_success.json").read_text(
            encoding="utf-8"
        )
        page = parse_history_page(payload)

        rendered = render_history_page_json(page)

        data = json.loads(rendered)
        self.assertEqual(1, data["pageNo"])
        self.assertEqual("26076", data["draws"][0]["issueNo"])
        self.assertEqual([15, 20, 27, 28, 35], data["draws"][0]["frontNumbers"])
        self.assertEqual([2, 11], data["draws"][0]["backNumbers"])
        self.assertEqual("804904836.99", data["draws"][0]["poolBalance"])
        self.assertEqual(None, data["draws"][0]["prizeTiers"][1]["stakeAmount"])


if __name__ == "__main__":
    unittest.main()
