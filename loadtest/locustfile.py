"""Locust CLI 진입점. 실제 로직은 scenarios.py에 있고, 이 파일은 노출만 한다."""

from loadtest.scenarios import CounselorUser

__all__ = ["CounselorUser"]
