import requests
import uuid
import mysql.connector

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
        "name": "Deactivatable User",
        "email": email,
        "password": password,
        "phoneNumber": "0911222333",
        "address": "Deactivate Street",
        "roles": ["CUSTOMER"],
    }

    res = requests.post(REGISTER_URL, json=payload)
    assert res.status_code == 200
    login_res = requests.post(LOGIN_URL, json={"email": email, "password": password})
    assert login_res.status_code == 200
    token = login_res.json()["data"]["token"]
    return email, token


def test_deactivate_own_account_success():
    email, token = register_and_login()
    headers = {"Authorization": f"Bearer {token}"}
    res = requests.put(f"{ACCOUNT_URL}/deactivate", headers=headers)
    assert res.status_code == 200
    body = res.json()
    assert "deactivated" in body["message"].lower()

    # ✅ Kiểm tra trong DB user bị vô hiệu hóa
    conn = mysql.connector.connect(
        host="mysql_test", user="root", password="123456", database="fooddb_test"
    )
    cur = conn.cursor()
    cur.execute("SELECT is_active FROM users WHERE email=%s", (email,))
    result = cur.fetchone()
    conn.close()
    assert result and result[0] == 0


def test_deactivate_own_account_unauthorized():
    res = requests.put(f"{ACCOUNT_URL}/deactivate")
    assert res.status_code in (400, 401)
