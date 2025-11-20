import uuid
import requests

BASE_URL = "http://foodappTest:8091/api/auth/register"


def random_email():
    return f"test_{uuid.uuid4().hex[:8]}@example.com"

def test_register_success():
    payload = {
        "name": "Alice",
        "email": random_email(),
        "password": "Secret123!",
        "phoneNumber": "0911222333",
        "address": "123 Main Street",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 200
    body = res.json()
    assert body["message"] == "User Registered Successfully"
    assert body["statusCode"] == 200

def test_register_invalid_password():
    payload = {
        "name": "Eve",
        "email": random_email(),
        "password": "123",  # Invalid password (too short)
        "phoneNumber": "0901234567",
        "address": "Unknown",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 400
    assert "Password must be at least 6 characters long" in res.text

def test_register_invalid_phoneNumber_too_short():
    payload = {
        "name": "Grace",
        "email": random_email(),
        "password": "ValidPass1!",
        "phoneNumber": "091234567",  # Invalid phone number (too short)
        "address": "Anywhere",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 400
    assert "Phone number must not exceed 10 digits" in res.text

def test_register_invalid_phoneNumber_include_letters():
    payload = {
        "name": "Frank",
        "email": random_email(),
        "password": "ValidPass1!",
        "phoneNumber": "55InvalidPhone",  # Invalid phone number
        "address": "Someplace",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 400
    assert "Invalid phone number format" in res.text

def test_invalid_email_format():
    payload = {
        "name": "TestUser",
        "email": "invalid-email-format",
        "password": "Pass123!",
        "phoneNumber": "0901234567",
        "address": "123 Test St",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 400
    assert "Invalid email format" in res.text   

def test_register_email_exists():
    email = random_email()
    # First request
    first = requests.post(BASE_URL, json={
        "name": "Bob",
        "email": email,
        "password": "Pass123!",
        "phoneNumber": "0909999999",
        "address": "City",
        "roles": ["CUSTOMER"]
    })
    assert first.status_code == 200

    # Second request → must fail 400
    res = requests.post(BASE_URL, json={
        "name": "Bob2",
        "email": email,
        "password": "Pass123!",
        "phoneNumber": "0908888888",
        "address": "City",
        "roles": ["CUSTOMER"]
    })
    assert res.status_code == 400
    assert "Email already exists" in res.text


def test_register_role_not_found():
    payload = {
        "name": "Charlie",
        "email": random_email(),
        "password": "Pass123!",
        "phoneNumber": "0907777777",
        "address": "Nowhere",
        "roles": ["invalid_role"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 404
    assert "Role 'invalid_role' Not Found" in res.text


def test_register_default_role():
    payload = {
        "name": "David",
        "email": random_email(),
        "password": "Pass123!",
        "phoneNumber": "0906666666",
        "address": "Test District",
        "roles": []
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code == 200
    body = res.json()
    assert body["message"] == "User Registered Successfully"


def test_register_missing_email():
    payload = {
        "name": "NoEmail",
        "password": "Pass123!",
        "phoneNumber": "0905555555",
        "address": "Somewhere",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code in (400, 422)

def test_register_empty_body():
    res = requests.post(BASE_URL, json={})
    assert res.status_code in (400, 422)

def test_register_missing_name():
    payload = {
        "password": "noName!",
        "email":random_email(),
        "phoneNumber": "0905555555",
        "address": "Somewhere",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code in (400, 422)

def test_register_missing_password():
    payload = {
        "name": "NoPass",
        "email":random_email(),
        "phoneNumber": "0905555555",
        "address": "Somewhere",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code in (400, 422)

def test_register_missing_phoneNumber():
    payload = {
        "name": "NoPhone",
        "email":random_email(),
        "password": "Pass123!",
        "address": "Somewhere",
        "roles": ["CUSTOMER"]
    }
    res = requests.post(BASE_URL, json=payload)
    assert res.status_code in (400, 422)
