import requests


# 🔹 INT-AUTH-05 – Login thành công + access API protected
def test_INT_AUTH_05_login_and_access_protected(base_url, customer_token):
    res = requests.get(
        f"{base_url}/users/account",
        headers={"Authorization": f"Bearer {customer_token}"}
    )

    assert res.status_code == 200


# 🔹 INT-AUTH-06 – Login thất bại với user inactive
def test_INT_AUTH_06_login_inactive_user(base_url, register_user_factory):
    # register
    user = register_user_factory("CUSTOMER")

    # login lần 1
    login = requests.post(
        f"{base_url}/auth/login",
        json={
            "email": user["email"],
            "password": user["password"]
        }
    )
    assert login.status_code == 200
    token = login.json()["data"]["token"]

    # deactivate
    deactivate = requests.delete(
        f"{base_url}/users/deactivate",
        headers={"Authorization": f"Bearer {token}"}
    )
    assert deactivate.status_code == 200

    # login lại
    login_again = requests.post(
        f"{base_url}/auth/login",
        json={
            "email": user["email"],
            "password": user["password"]
        }
    )

    assert login_again.status_code in [401, 403, 404]
