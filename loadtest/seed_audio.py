"""부하테스트 전 1회 실행 — 샘플 오디오 하나를 업로드해두고 audioUri를 출력한다.
   실행: python -m loadtest.seed_audio ./sample.wav
"""

import sys

import requests

from loadtest.config import settings


def main(sample_path: str) -> None:
    login_res = requests.post(
        f"{settings.base_url}/api/auth/login",
        json={"loginId": settings.login_id, "password": settings.password},
    )
    login_res.raise_for_status()
    token = login_res.json()["accessToken"]
    headers = {"Authorization": f"Bearer {token}"}

    url_res = requests.post(
        f"{settings.base_url}/api/audio/upload-url",
        json={"fileExtension": sample_path.rsplit(".", 1)[-1]},
        headers=headers,
    )
    url_res.raise_for_status()
    presigned = url_res.json()

    with open(sample_path, "rb") as f:
        put_res = requests.put(presigned["uploadUrl"], data=f)
    put_res.raise_for_status()

    print("LOADTEST_SAMPLE_AUDIO_URI=" + presigned["audioUri"])


if __name__ == "__main__":
    main(sys.argv[1])
