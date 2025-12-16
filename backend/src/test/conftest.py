import pytest
import requests
import uuid

BASE_URL = "http://localhost:8091/api"


# =========================
# GENERIC HELPERS
# =========================

def assert_json_response(res):
    assert res.headers.get("Content-Type", "").startswith("application/json"), \
        f"Expected JSON response, got: {res.headers.get('Content-Type')}"
    return res.json()


def assert_success_response(body):
    assert "statusCode" in body
    assert body["statusCode"] == 200
    assert "message" in body


# =========================
# FIXTURES
# =========================

@pytest.fixture
def base_url():
    return BASE_URL


@pytest.fixture
def unique_email():
    return f"ci_{uuid.uuid4().hex}@test.com"


@pytest.fixture
def register_payload(unique_email):
    return {
        "name": "CI User",
        "email": unique_email,
        "password": "password123",
        "address": "HCM",
        "phoneNumber": "0123456789",
        "roles": ["CUSTOMER"]
    }


@pytest.fixture
def register_user(base_url, register_payload):
    res = requests.post(f"{base_url}/auth/register", json=register_payload)
    assert res.status_code == 200

    body = assert_json_response(res)
    assert_success_response(body)

    return register_payload

    assert res.status_code == 200
    body = assert_json_response(res)

    assert "data" in body, f"Login response missing data: {body}"
    assert "token" in body["data"], f"Login response missing token: {body}"

    return body["data"]["token"]

@pytest.fixture
def fresh_login_token(base_url, unique_email):
    payload = {
        "name": "CI User",
        "email": unique_email,
        "password": "password123",
        "address": "HCM",
        "phoneNumber": "0123456789",
        "roles": ["CUSTOMER"]
    }

    requests.post(f"{base_url}/auth/register", json=payload)
    res = requests.post(f"{base_url}/auth/login", json={
        "email": payload["email"],
        "password": payload["password"]
    })

    return res.json()["data"]["token"]

