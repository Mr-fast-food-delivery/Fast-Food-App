import pytest
import requests
import uuid

BASE_URL = "http://localhost:8091/api"


# =========================================================
# GENERIC HELPERS
# =========================================================

def safe_json(res):
    """
    Parse JSON an toàn cho Integration Test.

    - Không crash nếu response body rỗng
    - Không crash nếu Content-Type không phải application/json
    - Không crash nếu backend trả response sai format
    """
    if not res.text:
        return {}

    content_type = res.headers.get("Content-Type", "")
    if "application/json" not in content_type:
        return {}

    try:
        return res.json()
    except ValueError:
        return {}


def assert_success_response(body):
    """
    Assert chuẩn cho response dạng wrapper:
    {
        statusCode: 200,
        message: "...",
        data: ...
    }
    """
    assert isinstance(body, dict)
    assert body.get("statusCode") == 200
    assert "message" in body


# =========================================================
# BASE FIXTURES
# =========================================================

@pytest.fixture
def base_url():
    return BASE_URL


# =========================================================
# REGISTER USER (FACTORY)
# =========================================================

@pytest.fixture
def register_user_factory(base_url):
    """
    Factory tạo user theo role.

    Usage:
        user = register_user_factory("CUSTOMER")
        user = register_user_factory("ADMIN")
    """
    def _register(role: str):
        payload = {
            "name": "CI User",
            "email": f"ci_{uuid.uuid4().hex}@test.com",
            "password": "password123",
            "address": "HCM",
            "phoneNumber": "0123456789",
            "roles": [role]
        }

        res = requests.post(f"{base_url}/auth/register", json=payload)
        assert res.status_code == 200

        body = safe_json(res)
        assert_success_response(body)

        return payload

    return _register


# =========================================================
# LOGIN TOKEN (FACTORY)
# =========================================================

@pytest.fixture
def login_token_factory(base_url, register_user_factory):
    """
    Factory login và trả về JWT token theo role.
    """
    def _login(role: str):
        user = register_user_factory(role)

        res = requests.post(
            f"{base_url}/auth/login",
            json={
                "email": user["email"],
                "password": user["password"]
            }
        )

        assert res.status_code == 200

        body = safe_json(res)
        assert "data" in body, f"Login response invalid: {body}"
        assert "token" in body["data"]

        return body["data"]["token"]

    return _login


# =========================================================
# ROLE-SPECIFIC TOKENS
# =========================================================

@pytest.fixture
def customer_token(login_token_factory):
    """JWT token của user CUSTOMER"""
    return login_token_factory("CUSTOMER")


@pytest.fixture
def admin_token(login_token_factory):
    """JWT token của user ADMIN"""
    return login_token_factory("ADMIN")
