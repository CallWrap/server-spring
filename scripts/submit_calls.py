#!/usr/bin/env python3
"""폴더에 있는 통화 음성을 한꺼번에 접수한다.

    python3 scripts/submit_calls.py ~/Desktop/통화모음

음성 하나를 넣는 절차(업로드 URL 발급 → 스토리지에 PUT → 콜 접수)를 폴더 전체에 반복한다.
분석실처럼 수백 건을 넣어야 하는 자리를 염두에 뒀다.

**파일명이 곧 sourceRequestId다.** 그 컬럼에 UNIQUE 제약이 걸려 있어서 이미 넣은 파일은
서버가 409로 거절한다 — 즉 **중간에 끊고 다시 돌려도 이어서 진행되고**, 폴더에 파일을 더
넣고 다시 돌리면 새 것만 들어간다. 폴더 감시 데몬을 따로 두지 않은 이유가 이것이다.

의존성은 파이썬 표준 라이브러리뿐이다(망분리 환경에서도 그대로 돈다). wav가 아닌 파일은
ffmpeg이 있으면 자동 변환하고, 없으면 그 파일만 건너뛴다.
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

#: 화자분리(pyannote)가 soundfile로 읽는 형식만 바로 받는다. 나머지는 ffmpeg으로 변환한다.
NATIVE = {".wav", ".flac", ".aiff", ".aif"}
CONVERTIBLE = {".m4a", ".mp3", ".aac", ".ogg", ".mp4", ".wma"}


def call_api(url: str, *, method: str = "GET", body: dict | None = None,
             token: str | None = None, api_key: str | None = None) -> tuple[int, dict]:
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"} if data else {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if api_key:
        headers["X-API-Key"] = api_key
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as res:
            raw = res.read()
            return res.status, (json.loads(raw) if raw else {})
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"message": raw.decode(errors="replace")[:200]}


def resolve_counselor(base: str, login_id: str, password: str) -> tuple[str, str | None]:
    """로그인해서 상담사 UUID를 얻는다 — 긴 UUID를 손으로 붙여넣지 않아도 되게."""
    status, body = call_api(f"{base}/api/auth/login", method="POST",
                            body={"loginId": login_id, "password": password})
    if status != 200:
        sys.exit(f"로그인 실패({status}): {body.get('message', body)}\n"
                 f"  --counselor-id 로 직접 지정하거나 --login-id/--password 를 확인하세요.")
    token = body["accessToken"]
    status, me = call_api(f"{base}/api/users/me", token=token)
    if status != 200:
        sys.exit(f"상담사 조회 실패({status}): {me}")
    return me["counselorId"], token


def to_wav(src: Path, workdir: Path) -> Path | None:
    """필요하면 16kHz 모노 wav로 변환. 변환할 수 없으면 None."""
    if src.suffix.lower() in NATIVE:
        return src
    if shutil.which("ffmpeg") is None:
        return None
    out = workdir / f"{src.stem}.wav"
    done = subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(src), "-ar", "16000", "-ac", "1", str(out)],
        capture_output=True,
    )
    return out if done.returncode == 0 and out.exists() else None


def upload(base: str, api_key: str, wav: Path) -> str | None:
    """업로드 URL을 받아 파일을 그대로 PUT한다. audio_uri를 돌려준다.

    /api/audio/upload-url은 상담사 JWT가 아니라 사보원 콜 접수 시스템용 API Key
    (SecurityConfig의 API_KEY_INTAKE 스코프)로 인가한다 — 사람이 로그인해서 접수하는
    경로가 아니라 외부 시스템이 접수하는 경로를 흉내 내기 때문."""
    status, body = call_api(f"{base}/api/audio/upload-url", method="POST",
                            body={"fileExtension": wav.suffix.lstrip(".")}, api_key=api_key)
    if status != 200:
        return None
    put = urllib.request.Request(body["uploadUrl"], data=wav.read_bytes(), method="PUT")
    try:
        with urllib.request.urlopen(put, timeout=300):
            pass
    except (urllib.error.URLError, OSError):
        return None
    return body["audioUri"]


def main() -> None:
    p = argparse.ArgumentParser(description="폴더의 음성 파일을 한꺼번에 접수한다")
    p.add_argument("folder", type=Path, help="음성 파일이 있는 폴더")
    p.add_argument("--base-url", default="http://localhost:8080")
    p.add_argument("--counselor-id", help="지정하지 않으면 로그인해서 알아낸다")
    p.add_argument("--login-id", default="test")
    p.add_argument("--password", default="test")
    p.add_argument("--api-key", default="local-dev-intake-key",
                    help="사보원 콜 접수용 API Key (SecurityConfig의 API_KEY_INTAKE 스코프). "
                         "로컬 기본값은 application-local.yml의 callwrap.api-key.intake와 동일 — "
                         "배포 환경에선 CALLWRAP_API_KEY_INTAKE 값으로 바꿔서 넘길 것.")
    p.add_argument("--dry-run", action="store_true", help="접수하지 않고 대상 파일만 보여준다")
    args = p.parse_args()

    base = args.base_url.rstrip("/")
    if not args.folder.is_dir():
        sys.exit(f"폴더가 아닙니다: {args.folder}")

    files = sorted(f for f in args.folder.iterdir()
                   if f.is_file() and f.suffix.lower() in NATIVE | CONVERTIBLE)
    if not files:
        sys.exit(f"음성 파일이 없습니다: {args.folder}")

    print(f"대상 {len(files)}건 — {args.folder}")
    if args.dry_run:
        for f in files:
            print(f"  {f.name}")
        return

    token = None
    counselor = args.counselor_id
    if not counselor:
        counselor, token = resolve_counselor(base, args.login_id, args.password)
    print(f"상담사 {counselor}\n")

    accepted = duplicated = failed = 0
    with tempfile.TemporaryDirectory() as tmp:
        workdir = Path(tmp)
        for i, src in enumerate(files, 1):
            head = f"[{i}/{len(files)}] {src.name}"
            wav = to_wav(src, workdir)
            if wav is None:
                print(f"{head} → 건너뜀 (변환 불가 — ffmpeg 필요)")
                failed += 1
                continue

            audio_uri = upload(base, args.api_key, wav)
            if audio_uri is None:
                print(f"{head} → 실패 (업로드)")
                failed += 1
                continue

            status, body = call_api(
                f"{base}/api/reviews", method="POST", api_key=args.api_key,
                body={
                    # 파일명을 그대로 쓴다 — 같은 파일을 두 번 넣으면 서버가 409로 거른다
                    "sourceRequestId": src.name,
                    "counselorId": counselor,
                    "audioFileUrl": audio_uri,
                    "callStartedAt": datetime.fromtimestamp(src.stat().st_mtime).isoformat(timespec="seconds"),
                },
            )
            if status == 202:
                print(f"{head} → 접수 {body.get('reviewId', '')[:8]}")
                accepted += 1
            elif status == 409:
                print(f"{head} → 이미 접수됨 (건너뜀)")
                duplicated += 1
            else:
                print(f"{head} → 실패 ({status}) {body.get('message', body)}")
                failed += 1

    print(f"\n접수 {accepted} · 중복 {duplicated} · 실패 {failed}")
    if accepted:
        print("처리 진행 상황은 대기열 화면이나 아래로 확인하세요:")
        print(f"  curl -s '{base}/api/reviews/stats'")


if __name__ == "__main__":
    main()
