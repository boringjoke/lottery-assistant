import sys
import unittest
import warnings
from pathlib import Path

warnings.filterwarnings(
    "ignore",
    message="Using `httpx` with `starlette.testclient` is deprecated.*",
)

from fastapi.testclient import TestClient


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
sys.path.insert(0, str(SRC_ROOT))

from lottery_crawler.sources.sporttery.client import SportteryFetchError
from lottery_crawler.sources.sporttery.parser import parse_history_page
from lottery_crawler.domain.models import HistoryPage
from lottery_crawler.api import app, get_sporttery_client


FIXTURE_DIR = Path(__file__).resolve().parent / "fixtures" / "sporttery"


class FakeSportteryClient:
    def __init__(self, page: HistoryPage) -> None:
        self.page = page
        self.calls: list[tuple[int, int]] = []

    def fetch_history_page(self, *, page_no: int = 1, page_size: int = 5) -> HistoryPage:
        self.calls.append((page_no, page_size))
        return self.page


class FailingSportteryClient:
    def fetch_history_page(self, *, page_no: int = 1, page_size: int = 5) -> HistoryPage:
        raise SportteryFetchError("Sporttery returned non-JSON content")


class CrawlerApiTest(unittest.TestCase):
    def tearDown(self) -> None:
        app.dependency_overrides.clear()

    def test_health_returns_ok(self):
        client = TestClient(app)

        response = client.get("/health")

        self.assertEqual(200, response.status_code)
        self.assertEqual({"status": "ok"}, response.json())

    def test_history_page_returns_normalized_draw_page(self):
        fake_client = FakeSportteryClient(_fixture_page())
        app.dependency_overrides[get_sporttery_client] = lambda: fake_client
        client = TestClient(app)

        response = client.get("/api/crawler/dlt/history-page?pageNo=2&pageSize=5")

        self.assertEqual(200, response.status_code)
        data = response.json()
        self.assertEqual(1, data["pageNo"])
        self.assertEqual("26076", data["draws"][0]["issueNo"])
        self.assertEqual([15, 20, 27, 28, 35], data["draws"][0]["frontNumbers"])
        self.assertEqual([(2, 5)], fake_client.calls)

    def test_latest_returns_first_draw_from_history_page(self):
        fake_client = FakeSportteryClient(_fixture_page())
        app.dependency_overrides[get_sporttery_client] = lambda: fake_client
        client = TestClient(app)

        response = client.get("/api/crawler/dlt/latest")

        self.assertEqual(200, response.status_code)
        data = response.json()
        self.assertEqual("26076", data["draw"]["issueNo"])
        self.assertEqual([(1, 5)], fake_client.calls)

    def test_upstream_failure_returns_stable_error_payload(self):
        app.dependency_overrides[get_sporttery_client] = lambda: FailingSportteryClient()
        client = TestClient(app)

        response = client.get("/api/crawler/dlt/latest")

        self.assertEqual(502, response.status_code)
        self.assertEqual(
            {
                "code": "UPSTREAM_FETCH_FAILED",
                "message": "Sporttery returned non-JSON content",
            },
            response.json(),
        )


def _fixture_page() -> HistoryPage:
    payload = (FIXTURE_DIR / "history_page_success.json").read_text(
        encoding="utf-8"
    )
    return parse_history_page(payload)


if __name__ == "__main__":
    unittest.main()
