import pytest
import requests
import uuid

BASE_URL = "http://localhost:8091/api"


# =========================================================
# GENERIC HELPERS
# =========================================================

def safe_json(res):
    """
    Parse JSON an toàn.
    Tránh crash khi response rỗng hoặc 204 No Content
    """
    if not res.text:
        return {}
    return res.json()


def assert_json_response(res):
    assert res.headers.get("Content-Type", "").startswith("application/json"), \
        f"Expected JSON response, got: {res.headers.get('Content-Type')}"
    return res.json()


def assert_success_response(body):
    assert "statusCode" in body
    assert body["statusCode"] == 200
    assert "message" in body


# =========================================================
# BASE FIXTURES
# =========================================================

@pytest.fixture
def base_url():
    return BASE_URL


@pytest.fixture
def unique_email():
    return f"ci_{uuid.uuid4().hex}@test.com"


# =========================================================
# REGISTER USER (FACTORY)
# =========================================================

@pytest.fixture
def register_user_factory(base_url):
    """
    Factory tạo user theo role
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

        body = assert_json_response(res)
        assert_success_response(body)

        return payload

    return _register


# =========================================================
# LOGIN TOKEN (FACTORY)
# =========================================================

@pytest.fixture
def login_token_factory(base_url, register_user_factory):
    """
    Factory login và trả về JWT token theo role
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

        assert "data" in body
        assert "token" in body["data"]

        return body["data"]["token"]

    return _login


# =========================================================
# ROLE-SPECIFIC TOKENS
# =========================================================

@pytest.fixture
def customer_token(login_token_factory):
    """
    Token user CUSTOMER
    """
    return login_token_factory("CUSTOMER")


@pytest.fixture
def admin_token(login_token_factory):
    """
    Token user ADMIN
    """
    return login_token_factory("ADMIN")
