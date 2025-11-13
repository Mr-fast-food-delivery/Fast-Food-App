import requests
import uuid

BASE_URL = "http://foodappTest:8091/api"
REGISTER_URL = f"{BASE_URL}/auth/register"
LOGIN_URL = f"{BASE_URL}/auth/login"
USERS_URL = f"{BASE_URL}/users"


def random_email():
    return f"user_{uuid.uuid4().hex[:8]}@example.com"


def register_and_login(role="CUSTOMER"):
    email = random_email()
    password = "Secret123!"

    payload = {
        "name": "Test Admin" if role == "admin" else "Test User",
        "email": email,
        "password": password,
        "phoneNumber": "0911222333",
        "address": "Test Street",
        "roles": [role],
    }

    res = requests.post(REGISTER_URL, json=payload)
    assert res.status_code == 200

    login_res = requests.post(LOGIN_URL, json={"email": email, "password": password})
    assert login_res.status_code == 200
    token = login_res.json()["data"]["token"]
    return token


def test_get_all_users_as_admin():
    """Admin có thể lấy danh sách tất cả user"""
    token = register_and_login(role="ADMIN")
    headers = {"Authorization": f"Bearer {token}"}
    res = requests.get(f"{USERS_URL}/all", headers=headers)
    assert res.status_code == 200
    body = res.json()
    assert body["statusCode"] == 200
    assert isinstance(body["data"], list)


def test_get_all_users_as_CUSTOMER_forbidden():
    """CUSTOMER không được phép gọi getAllUsers"""
    token = register_and_login(role="CUSTOMER")
    headers = {"Authorization": f"Bearer {token}"}
    res = requests.get(f"{USERS_URL}/all", headers=headers)
    assert res.status_code in (401, 403)
