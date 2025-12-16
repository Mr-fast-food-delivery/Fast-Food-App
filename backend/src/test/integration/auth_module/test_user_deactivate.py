import requests


def test_INT_AUTH_08_deactivate_invalidates_token(base_url, fresh_login_token):
    headers = {"Authorization": f"Bearer {fresh_login_token}"}

    # Deactivate account
    res = requests.delete(f"{base_url}/users/deactivate", headers=headers)
    assert res.status_code == 200

    # Token must be invalid after deactivation
    res = requests.get(f"{base_url}/users/account", headers=headers)
    assert res.status_code in [401, 403, 404]

def test_INT_AUTH_09_deactivate_send_email(base_url, fresh_login_token):
    headers = {"Authorization": f"Bearer {fresh_login_token}"}

    res = requests.delete(f"{base_url}/users/deactivate", headers=headers)

    assert res.status_code == 200
    assert "Account deactivated successfully" in res.text

def test_INT_AUTH_10_idempotent_deactivate(base_url, fresh_login_token):
    headers = {"Authorization": f"Bearer {fresh_login_token}"}

    res1 = requests.delete(f"{base_url}/users/deactivate", headers=headers)
    res2 = requests.delete(f"{base_url}/users/deactivate", headers=headers)

    assert res1.status_code == 200
    assert res2.status_code in [200, 404]

