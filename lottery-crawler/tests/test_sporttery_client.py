import sys
import unittest
from pathlib import Path

import httpx


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
sys.path.insert(0, str(SRC_ROOT))

from lottery_crawler.sources.sporttery.client import SportteryClient, SportteryFetchError


FIXTURE_DIR = Path(__file__).resolve().parent / "fixtures" / "sporttery"


class SportteryClientTest(unittest.TestCase):
    def test_fetch_history_page_uses_safe_defaults_and_browser_headers(self):
        captured_requests = []
        payload = (FIXTURE_DIR / "history_page_success.json").read_text(
            encoding="utf-8"
        )

        def handler(request: httpx.Request) -> httpx.Response:
            captured_requests.append(request)
            return httpx.Response(200, text=payload, headers={"content-type": "application/json"})

        client = SportteryClient(transport=httpx.MockTransport(handler))

        page = client.fetch_history_page()

        self.assertEqual(2, len(page.draws))
        request = captured_requests[0]
        self.assertEqual("85", request.url.params["gameNo"])
        self.assertEqual("0", request.url.params["provinceId"])
        self.assertEqual("1", request.url.params["isVerify"])
        self.assertEqual("5", request.url.params["pageSize"])
        self.assertEqual("1", request.url.params["pageNo"])
        self.assertIn("Mozilla/5.0", request.headers["user-agent"])
        self.assertEqual("application/json, text/plain, */*", request.headers["accept"])
        self.assertEqual("https://static.sporttery.cn/", request.headers["referer"])

    def test_fetch_history_page_rejects_html_block_page(self):
        def handler(request: httpx.Request) -> httpx.Response:
            return httpx.Response(
                200,
                text="<!DOCTYPE html><html>Restricted Access</html>",
                headers={"content-type": "text/html"},
            )

        client = SportteryClient(transport=httpx.MockTransport(handler))

        with self.assertRaisesRegex(SportteryFetchError, "non-JSON"):
            client.fetch_history_page()

    def test_fetch_history_page_rejects_forbidden_response(self):
        def handler(request: httpx.Request) -> httpx.Response:
            return httpx.Response(403, text="Forbidden")

        client = SportteryClient(transport=httpx.MockTransport(handler))

        with self.assertRaisesRegex(SportteryFetchError, "403"):
            client.fetch_history_page()


if __name__ == "__main__":
    unittest.main()
