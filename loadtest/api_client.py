"""Spring REST API 얇은 래퍼. 책임은 딱 하나 — 엔드포인트 호출과 응답 반환.
   '언제 부를지'는 모른다 — 그건 scenarios.py 책임.
"""

import uuid


class CallWrapApiClient:
    def __init__(self, client):
        self._client = client
        self._headers: dict = {}

    def login(self, login_id: str, password: str) -> str:
        res = self._client.post(
            "/api/auth/login",
            json={"loginId": login_id, "password": password},
            name="/api/auth/login",
        )
        res.raise_for_status()
        token = res.json()["accessToken"]
        self._headers["Authorization"] = f"Bearer {token}"
        return token

    def list_queue(self, page: int = 0, size: int = 20):
        return self._client.get(
            f"/api/reviews?page={page}&size={size}",
            headers=self._headers,
            name="/api/reviews [list]",
        )

    def get_review(self, review_id: str):
        return self._client.get(
            f"/api/reviews/{review_id}",
            headers=self._headers,
            name="/api/reviews/[id]",
        )

    def create_review(self, counselor_id: str, audio_file_url: str):
        return self._client.post(
            "/api/reviews",
            json={
                "sourceRequestId": f"loadtest-{uuid.uuid4()}",
                "counselorId": counselor_id,
                "audioFileUrl": audio_file_url,
            },
            headers=self._headers,
            name="/api/reviews [create]",
        )

    def confirm_review(self, review_id: str, confirmed_fields: dict, edited_fields: list, review_sec: int):
        return self._client.patch(
            f"/api/reviews/{review_id}/confirm",
            json={
                "confirmedFields": confirmed_fields,
                "editedFields": edited_fields,
                "reviewSec": review_sec,
            },
            headers=self._headers,
            name="/api/reviews/[id]/confirm",
        )
