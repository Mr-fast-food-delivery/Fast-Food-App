import requests


# 🔹 INT-AUTH-01 – Register user thành công
def test_INT_AUTH_01_register_success(base_url, register_payload):
    res = requests.post(f"{base_url}/auth/register", json=register_payload)

    assert res.status_code == 200
    body = res.json()

    assert body["statusCode"] == 200
    assert body["message"] == "User Registered Successfully"


# 🔹 INT-AUTH-02 – Email đã tồn tại
def test_INT_AUTH_02_register_duplicate_email(base_url, register_payload):
    requests.post(f"{base_url}/auth/register", json=register_payload)

    res = requests.post(f"{base_url}/auth/register", json=register_payload)

    assert res.status_code == 400
    assert "Email already exists" in res.text


# 🔹 INT-AUTH-03 – Role không tồn tại
def test_INT_AUTH_03_register_invalid_role(base_url, register_payload):
    register_payload["roles"] = ["INVALID_ROLE"]

    res = requests.post(f"{base_url}/auth/register", json=register_payload)

    assert res.status_code == 404
    assert "Role" in res.text


# 🔹 INT-AUTH-04 – Rollback khi role không hợp lệ
def test_INT_AUTH_04_register_rollback_multiple_roles(base_url, register_payload):
    register_payload["roles"] = ["CUSTOMER", "INVALID"]

    res = requests.post(f"{base_url}/auth/register", json=register_payload)
    assert res.status_code == 404

    # Verify rollback: user không login được
    login = requests.post(
        f"{base_url}/auth/login",
        json={
            "email": register_payload["email"],
            "password": register_payload["password"]
        }
    )
    assert login.status_code == 404
