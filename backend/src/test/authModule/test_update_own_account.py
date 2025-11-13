import requests
import uuid

BASE_URL = "http://foodappTest:8091/api"
REGISTER_URL = f"{BASE_URL}/auth/register"
LOGIN_URL = f"{BASE_URL}/auth/login"
ACCOUNT_URL = f"{BASE_URL}/users/account"


def random_email():
    return f"user_{uuid.uuid4().hex[:8]}@example.com"


def register_and_login():
    email = random_email()
    password = "Secret123!"
    payload = {
        "name": "Updatable User",
        "email": email,
        "password": password,
        "phoneNumber": "0911222333",
        "address": "Old Address",
        "roles": ["CUSTOMER"],
    }

    res = requests.post(REGISTER_URL, json=payload)
    assert res.status_code == 200
    login_res = requests.post(LOGIN_URL, json={"email": email, "password": password})
    assert login_res.status_code == 200
    token = login_res.json()["data"]["token"]
    return token


def test_update_own_account_success():
    token = register_and_login()
    headers = {"Authorization": f"Bearer {token}"}
    payload = {"name": "Updated Name", "phoneNumber": "0988777666", "address": "New Street"}
    res = requests.put(f"{ACCOUNT_URL}/update", json=payload, headers=headers)
    assert res.status_code == 200
    body = res.json()
    assert body["statusCode"] == 200
    assert "update" in body["message"].lower()


def test_update_own_account_invalid_data():
    token = register_and_login()
    headers = {"Authorization": f"Bearer {token}"}
    payload = {"phoneNumber": "invalid"}  # sai định dạng
    res = requests.put(f"{ACCOUNT_URL}/update", json=payload, headers=headers)
    assert res.status_code in (400, 422)
