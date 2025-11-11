import requests
import uuid
import mysql.connector

BASE_URL = "http://foodappTest:8091/api"
REGISTER_URL = f"{BASE_URL}/auth/register"
LOGIN_URL = f"{BASE_URL}/auth/login"
ACCOUNT_URL = f"{BASE_URL}/users/account"


def random_email():
    return f"test_{uuid.uuid4().hex[:8]}@example.com"


def register_and_login():
    email = random_email()
    password = "123456789"

    payload = {
        "name": "LoginTestUser",
        "email": email,
        "password": password,
        "phoneNumber": "0911222333",
        "address": "Test Street",
        "roles": ["customer"]
    }

    r = requests.post(REGISTER_URL, json=payload)
    assert r.status_code == 200, f"❌ Register failed: {r.text}"

    login_res = requests.post(LOGIN_URL, json={"email": email, "password": password})
    assert login_res.status_code == 200, f"❌ Login failed: {login_res.text}"

    token = login_res.json()["data"]["token"]
    return email, password, token


def test_get_current_user_success():
    _, _, token = register_and_login()
    headers = {"Authorization": f"Bearer {token}"}
    res = requests.get(ACCOUNT_URL, headers=headers)
    assert res.status_code == 200
    body = res.json()
    assert body["statusCode"] == 200
    assert "email" in body["data"]
    assert "name" in body["data"]


def test_get_current_user_missing_token():
    res = requests.get(ACCOUNT_URL)
    assert res.status_code in (400, 401)


def test_get_current_user_invalid_token():
    headers = {"Authorization": "Bearer invalid.token.value"}
    res = requests.get(ACCOUNT_URL, headers=headers)
    assert res.status_code in (400, 401)


def test_get_current_user_inactive_user():
    email, password, token = register_and_login()

    conn = mysql.connector.connect(
        host="mysql_test", user="root", password="123456", database="fooddb_test"
    )
    cur = conn.cursor()
    cur.execute("UPDATE users SET is_active = 0 WHERE email = %s", (email,))
    conn.commit()
    conn.close()

    headers = {"Authorization": f"Bearer {token}"}
    res = requests.get(ACCOUNT_URL, headers=headers)
    assert res.status_code in (401, 404)

    # ✅ Kiểm tra linh hoạt hơn: "inactive" hoặc "not active"
    body_text = res.text.lower()
    assert "inactive" in body_text or "not active" in body_text, f"Unexpected message: {res.text}"

