"""부하 테스트 실행 설정. 전부 환경변수로 받는다 — 코드 수정 없이 로컬/스테이징을 바꿀 수 있게."""

import os


class Settings:
    base_url: str = os.environ.get("LOADTEST_BASE_URL", "http://localhost:8080")
    login_id: str = os.environ.get("LOADTEST_LOGIN_ID", "tester")
    password: str = os.environ.get("LOADTEST_PASSWORD", "test1234!")
    counselor_id: str = os.environ.get("LOADTEST_COUNSELOR_ID", "")
    # seed_audio.py 실행 후 출력되는 audioUri를 여기에 넣는다 (s3://bucket/key)
    sample_audio_uri: str = os.environ.get("LOADTEST_SAMPLE_AUDIO_URI", "")


settings = Settings()
