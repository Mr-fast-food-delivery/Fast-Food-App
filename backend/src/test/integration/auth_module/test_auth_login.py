import requests


# 🔹 INT-AUTH-05 – Login thành công + API protected
def test_INT_AUTH_05_login_and_access_protected(base_url, login_token):
    headers = {"Authorization": f"Bearer {login_token}"}

    res = requests.get(f"{base_url}/users/account", headers=headers)

    assert res.status_code == 200
    body = res.json()

    assert "data" in body
    assert "email" in body["data"]


# 🔹 INT-AUTH-06 – Login thất bại với user inactive
def test_INT_AUTH_06_login_inactive_user(base_url, register_user):
    # login
    login = requests.post(
        f"{base_url}/auth/login",
        json={
            "email": register_user["email"],
            "password": register_user["password"]
        }
    )
    token = login.json()["data"]["token"]

    # deactivate
    requests.delete(
        f"{base_url}/users/deactivate",
        headers={"Authorization": f"Bearer {token}"}
    )

    # login lại
    login_again = requests.post(
        f"{base_url}/auth/login",
        json={
            "email": register_user["email"],
            "password": register_user["password"]
        }
    )

    assert login_again.status_code == 404
