import xml.etree.ElementTree as ET
import requests

# Đường dẫn tới file JUnit XML
xml_file = "target/surefire-reports/TEST-com.phegon.FoodApp.auth_users.services.AuthServiceImplTest.xml"

# Parse file XML
try:
    tree = ET.parse(xml_file)
    root = tree.getroot()
except FileNotFoundError:
    print(f"File XML không tìm thấy: {xml_file}")
    exit(1)
except ET.ParseError as e:
    print(f"Lỗi khi đọc XML: {e}")
    exit(1)

# Lấy số liệu
tests = int(root.attrib.get('tests', 0))
failures = int(root.attrib.get('failures', 0))
skipped = int(root.attrib.get('skipped', 0))
success = tests - failures - skipped

print(f"Tests: {tests}, Success: {success}, Failures: {failures}, Skipped: {skipped}")

# Chuẩn bị metrics theo định dạng Prometheus
metrics = f"""
# TYPE junit_tests_total counter
junit_tests_total {tests}
# TYPE junit_tests_failed counter
junit_tests_failed {failures}
# TYPE junit_tests_skipped counter
junit_tests_skipped {skipped}
# TYPE junit_tests_success counter
junit_tests_success {success}
"""

# URL Pushgateway
push_url = "http://localhost:9091/metrics/job/junit_tests"

try:
    response = requests.post(push_url, data=metrics, headers={'Content-Type': 'text/plain'})
    if 200 <= response.status_code < 300:
        print("Push metrics thành công!")
    else:
        print("Lỗi push metrics:", response.status_code, response.text)
except requests.exceptions.RequestException as e:
    print("Lỗi khi kết nối Pushgateway:", e)
