import requests


# 🔹 INT-AUTH-08 – Deactivate làm token invalid
def test_INT_AUTH_08_deactivate_invalidates_token(base_url, customer_token):
    headers = {"Authorization": f"Bearer {customer_token}"}

    res = requests.delete(f"{base_url}/users/deactivate", headers=headers)
    assert res.status_code == 200

    res = requests.get(f"{base_url}/users/account", headers=headers)
    assert res.status_code in [401, 403, 404]


# 🔹 INT-AUTH-09 – Deactivate gửi email / message thành công
def test_INT_AUTH_09_deactivate_send_email(base_url, customer_token):
    res = requests.delete(
        f"{base_url}/users/deactivate",
        headers={"Authorization": f"Bearer {customer_token}"}
    )

    assert res.status_code == 200
    assert "deactivated" in res.text.lower()


# 🔹 INT-AUTH-10 – Deactivate idempotent
def test_INT_AUTH_10_idempotent_deactivate(base_url, customer_token):
    headers = {"Authorization": f"Bearer {customer_token}"}

    res1 = requests.delete(f"{base_url}/users/deactivate", headers=headers)
    res2 = requests.delete(f"{base_url}/users/deactivate", headers=headers)

    assert res1.status_code == 200
    assert res2.status_code in [200, 404]
