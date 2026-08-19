"""가상 상담사의 행동 패턴. '무엇을 얼마나 자주 하는가'만 정한다."""

import random

from locust import HttpUser, task, between

from loadtest.api_client import CallWrapApiClient
from loadtest.config import settings


class CounselorUser(HttpUser):
    """상담사 한 명: 로그인 → 대기열 확인 → 콜 접수 → 상태 확인 → 확정을 반복."""

    wait_time = between(1, 3)

    def on_start(self):
        self.api = CallWrapApiClient(self.client)
        self.api.login(settings.login_id, settings.password)
        self._tracked_review_ids: list = []

    @task(5)
    def browse_queue(self):
        """가장 흔한 동작 — 대기열 화면을 계속 새로고침."""
        self.api.list_queue()

    @task(3)
    def create_review(self):
        """새 콜 접수 — Spring→RabbitMQ→FastAPI 파이프라인을 실제로 태우는 동작."""
        if not settings.sample_audio_uri:
            return  # seed_audio.py를 먼저 돌려야 함
        res = self.api.create_review(settings.counselor_id, settings.sample_audio_uri)
        if res.ok:
            self._tracked_review_ids.append(res.json()["reviewId"])

    @task(2)
    def check_and_confirm(self):
        """접수해둔 콜 중 하나를 골라 상태 확인, 끝났으면 확정까지 흉내."""
        if not self._tracked_review_ids:
            return
        review_id = random.choice(self._tracked_review_ids)
        res = self.api.get_review(review_id)
        if not res.ok:
            return
        if res.json()["status"] in ("DONE", "NEEDS_HUMAN_REVIEW"):
            self.api.confirm_review(
                review_id,
                confirmed_fields={"grade": "NORMAL"},
                edited_fields=[],
                review_sec=random.randint(20, 120),
            )
            self._tracked_review_ids.remove(review_id)
