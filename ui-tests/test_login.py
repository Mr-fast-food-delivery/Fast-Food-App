import time
import random
import string
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from webdriver_manager.chrome import ChromeDriverManager


def random_email():
    s = ''.join(random.choice(string.ascii_lowercase) for _ in range(10))
    return f"test_{s}@gmail.com"


def test_register_and_login():

    # ====== FIX WEBDRIVER ======
    options = Options()
    options.add_argument("--start-maximized")
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)

    # =====================================================
    # 1) MỞ TRANG ĐĂNG KÝ
    # =====================================================
    driver.get("http://localhost:3000/register")
    time.sleep(5)

    email = random_email()
    password = "Test123@123"

    # Fill form đúng với FE của bạn
    driver.find_element(By.ID, "name").send_keys("Auto Test User")
    driver.find_element(By.ID, "email").send_keys(email)
    driver.find_element(By.ID, "password").send_keys(password)
    driver.find_element(By.ID, "confirmPassword").send_keys(password)
    driver.find_element(By.ID, "phoneNumber").send_keys("0123456789")
    driver.find_element(By.ID, "address").send_keys("123 Test Street")

    # ========= CLICK BUTTON SUBMIT =========
    # Dùng type="submit" vì FE của bạn dùng type submit
    driver.find_element(
        By.XPATH, "//button[@type='submit' and contains(text(), 'Register')]"
    ).click()

    time.sleep(5)

    # =====================================================
    # 2) LOGIN
    # =====================================================
    driver.get("http://localhost:3000/login")
    time.sleep(5)

    driver.find_element(By.ID, "email").send_keys(email)
    driver.find_element(By.ID, "password").send_keys(password)
    driver.find_element(By.ID, "password").send_keys(Keys.ENTER)

    time.sleep(5)

    # =====================================================
    # 3) KIỂM TRA LOGIN THÀNH CÔNG
    # =====================================================
    assert "dashboard" in driver.current_url.lower(), "❌ Login thất bại!"

    driver.quit()
