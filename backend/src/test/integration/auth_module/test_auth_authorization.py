import requests

# 🔹 INT-AUTH-07 – Authorization theo role CUSTOMER
def test_INT_AUTH_07_customer_cannot_access_admin_api(base_url, login_token):
    res = requests.get(
        f"{base_url}/users/all",
        headers={"Authorization": f"Bearer {login_token}"}
    )

    assert res.status_code == 403
    
# # 🔹 INT-AUTH-07 – Authorization theo role
# def test_INT_AUTH_07_authorization_admin_vs_customer(base_url):
#     # CUSTOMER
#     customer = requests.post(
#         f"{base_url}/auth/login",
#         json={"email": "customer@test.com", "password": "password123"}
#     )
#     customer_token = customer.json()["data"]["token"]

#     res_customer = requests.get(
#         f"{base_url}/users/all",
#         headers={"Authorization": f"Bearer {customer_token}"}
#     )
#     assert res_customer.status_code == 403

#     # ADMIN
#     admin = requests.post(
#         f"{base_url}/auth/login",
#         json={"email": "admin@test.com", "password": "password123"}
#     )
#     admin_token = admin.json()["data"]["token"]

#     res_admin = requests.get(
#         f"{base_url}/users/all",
#         headers={"Authorization": f"Bearer {admin_token}"}
#     )
#     assert res_admin.status_code == 200
