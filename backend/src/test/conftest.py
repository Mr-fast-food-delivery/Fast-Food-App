import pytest
import requests
import uuid

BASE_URL = "http://localhost:8091/api"

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
    return register_payload


@pytest.fixture
def login_user(base_url, register_user):
    res = requests.post(
        f"{base_url}/auth/login",
        json={
            "email": register_user["email"],
            "password": register_user["password"]
        }
    )
    assert res.status_code == 200
    return res.json()["data"]["token"]
