import requests


# 🔹 INT-AUTH-07 – CUSTOMER không được access ADMIN API
def test_INT_AUTH_07_customer_cannot_access_admin_api(base_url, customer_token):
    res = requests.get(
        f"{base_url}/users/all",
        headers={"Authorization": f"Bearer {customer_token}"}
    )

    assert res.status_code == 403


# 🔹 INT-AUTH-07b – ADMIN access được ADMIN API
def test_INT_AUTH_07b_admin_can_access_admin_api(base_url, admin_token):
    res = requests.get(
        f"{base_url}/users/all",
        headers={"Authorization": f"Bearer {admin_token}"}
    )

    assert res.status_code == 200
