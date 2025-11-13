import uuid
import requests

BASE_URL = "http://foodappTest:8091/api/auth/login"
REGISTER_URL = "http://foodappTest:8091/api/auth/register"


def random_email():
    return f"test_{uuid.uuid4().hex[:8]}@example.com"


def register_user(email=None, password="Secret123!"):
    """Helper: tạo user trước khi test đăng nhập."""
    if email is None:
        email = random_email()
    payload = {
        "name": "LoginTestUser",
        "email": email,
        "password": password,
        "phoneNumber": "0911222333",
        "address": "Test Street",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(REGISTER_URL, json=payload)
    assert res.status_code == 200, f"Failed to register test user: {res.text}"
    return email, password


def test_login_invalid_email_format():
    payload = {
        "email": "invalidemailformat",
        "password": "Secret123!"
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 400
    assert "Invalid email format" in res.text


def test_login_short_password():
    payload = {
        "email": "valid@example.com",
        "password": "123"
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 400
    assert "Password must be at least 6 characters long" in res.text


def test_login_missing_fields():
    payload = {}
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code in (400, 422)
    assert "email" in res.text.lower() or "password" in res.text.lower()


def test_login_invalid_credentials():
    email, password = register_user()
    payload = {
        "email": email,
        "password": "WrongPass123"
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code in (400, 401)
    assert "Invalid Password" in res.text or "Invalid Email" in res.text


def test_login_success():
    email, password = register_user()
    payload = {
        "email": email,
        "password": password
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 200, f"Expected 200 OK, got {res.status_code}: {res.text}"

    body = res.json()
    assert "data" in body
    data = body["data"]
    assert "token" in data
    assert "roles" in data
    assert isinstance(data["roles"], list)
    assert len(data["token"]) > 10
    assert body["message"] == "Login Successful"

def test_login_email_not_found():
    payload = {
        "email": random_email(),  # email chưa từng đăng ký
        "password": "Secret123!"
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 400
    assert "Invalid Email" in res.text


def test_login_inactive_user():
    # Tạo user nhưng set isActive = False trong DB (giả lập)
    email, password = register_user()
    # Giả lập user bị khóa (API riêng hoặc script)
    import mysql.connector
    conn = mysql.connector.connect(host="mysql_test", user="root", password="123456", database="fooddb_test")
    cur = conn.cursor()
    cur.execute("UPDATE users SET is_active = 0 WHERE email = %s", (email,))
    conn.commit()
    conn.close()

    payload = {"email": email, "password": password}
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 404
    assert "Account not active" in res.text
