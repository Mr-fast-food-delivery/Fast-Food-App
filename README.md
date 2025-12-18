# food-ordering-app
A food ordering app using springboot for backend and react fro frontend. With AWS for deployment and payment integrations
link to this website: fast-food-app-nu.vercel.app
![food](https://github.com/user-attachments/assets/29c1de9d-93dc-4835-bd4c-8297e1103510)

---

# 🍔 **Fast Food Delivery System**

### *End-to-End Food Ordering Platform — React + Spring Boot + CI/CD + Allure Report*

🔗 **Live Website:**
👉 [https://fast-food-app-nu.vercel.app/](https://fast-food-app-nu.vercel.app/)

🔗 **Backend API Base URL (Production):**
👉 [https://your-backend-domain.com/api](https://your-backend-domain.com/api)

🔗 **Allure Test Report:**
👉 [https://your-domain.com/allure-report/](https://your-domain.com/allure-report/)
*(Nếu bạn cung cấp link thực tế, tôi sẽ thay vào ngay.)*

---

## 📌 **1. Giới thiệu dự án**

**Fast Food Delivery System** là ứng dụng **đặt đồ ăn trực tuyến** được xây dựng theo mô hình **Micro Frontend + RESTful Backend** với đầy đủ các module:

* Quản lý người dùng & xác thực
* Hiển thị menu món ăn
* Giỏ hàng + đặt hàng
* Thanh toán trực tuyến
* Theo dõi trạng thái đơn hàng
* Admin quản lý món ăn & đơn hàng
* Tích hợp email + thanh toán + lưu trữ ảnh

Dự án hướng đến **quy chuẩn triển khai thực tế của doanh nghiệp**, tích hợp đầy đủ:

* **CI/CD pipeline** tự động build & test
* **Allure Reporting** để theo dõi chất lượng
* **Deployment tối ưu** (Docker, Vercel, AWS, Render, Railway,…)

---

## 📦 **2. Tính năng nổi bật**

### 👤 **Người dùng**

✔ Đăng ký, đăng nhập, xác thực JWT
✔ Xem danh sách món ăn
✔ Tìm kiếm, lọc món ăn
✔ Thêm/xoá/sửa giỏ hàng
✔ Đặt món, chọn địa chỉ + phương thức thanh toán
✔ Theo dõi đơn hàng theo thời gian thực
✔ Xem lịch sử đơn

### 🛒 **Admin**

✔ Thêm / sửa / xoá món ăn
✔ Xử lý đơn hàng
✔ Quản lý người dùng
✔ Dashboard thống kê

### ⚙️ Kỹ thuật

✔ Kiến trúc tách lớp rõ ràng
✔ API REST chuẩn hoá
✔ Quản lý ảnh bằng S3 hoặc local storage
✔ Allure Reporting cho backend test
✔ GitHub Actions CI/CD
✔ React UI tối ưu Lighthouse

---

## 🧱 **3. Kiến trúc tổng quan**

```
Client (React)
     ↓ HTTP/HTTPS
API Gateway / Backend (Spring Boot)
     ↓ JPA / Hibernate
Database (MySQL / PostgreSQL)
     ↓ AWS SDK
S3 Storage (Images)
```

---

## 📁 **4. Cấu trúc thư mục chi tiết**

```
Fast-Food-App/
│
├── backend/                        # Spring Boot backend
│   ├── src/main/java/com/...       # API, Services, Entities
│   ├── src/test/java/com/...       # Unit + Integration Tests
│   ├── src/test/allure-results/    # Allure raw results
│   ├── pom.xml                     # Maven config
│   └── Dockerfile
│
├── frontend/                       # React customer UI
│   ├── src/components/
│   ├── src/pages/
│   ├── src/services/api.js
│   ├── public/
│   ├── package.json
│   └── Dockerfile
│
├── .github/workflows/              # CI/CD pipelines
│   ├── backend-ci.yml
│   └── frontend-ci.yml
│
├── docker-compose.yml
├── README.md
└── LICENSE
```

---

## 🛠️ **5. Công nghệ sử dụng**

### **Frontend**

* React 19
* React Router DOM
* Axios
* Chart.js
* TailwindCSS
* Vercel Hosting

### **Backend**

* Spring Boot 3.4
* Spring Security
* JPA / Hibernate
* MySQL / PostgreSQL
* Email Sender
* Stripe / Payment Gateway
* Lombok
* ModelMapper

### **DevOps & CI/CD**

* GitHub Actions
* Docker & Docker Compose
* Allure Reporting
* Vercel / AWS EC2 / Railway / Render

---

## 🧪 **6. Testing & Allure Report**

Dự án sử dụng:

* **JUnit 5**
* **Spring Test**
* **Mockito**
* **Allure Report** cho UI trực quan

### Chạy test backend:

```bash
cd backend
mvn clean test
```

### Sinh báo cáo Allure:

```bash
allure serve target/allure-results
```

### Allure Report Online:

👉 [https://your-domain.com/allure-report/](https://your-domain.com/allure-report/)
*(Cung cấp link thực tế nếu bạn muốn tôi ghép vào.)*

---

## ⚙️ **7. Cài đặt Backend**

### 1. Clone repo

```bash
git clone https://github.com/Mr-fast-food-delivery/Fast-Food-App.git
cd Fast-Food-App/backend
```

### 2. Cấu hình DB (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fastfood
    username: root
    password: 123456
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: YOUR_SECRET_KEY
aws:
  s3:
    bucket: fastfood-image-storage
```

### 3. Chạy server

```bash
mvn spring-boot:run
```

Backend chạy tại:
👉 [http://localhost:8080/api](http://localhost:8080/api)

---

## 🎨 **8. Cài đặt Frontend**

### 1. Chuyển thư mục

```bash
cd ../frontend
```

### 2. Cài dependencies

```bash
npm install
```

### 3. Chạy development server

```bash
npm start
```

Frontend chạy tại:
👉 [http://localhost:3000/](http://localhost:3000/)

---

## 🚀 **9. Deployment**

### **Frontend – Vercel**

```bash
npm run build
vercel deploy
```

### **Backend – Docker**

```bash
docker build -t fastfood-backend .
docker run -p 8080:8080 fastfood-backend
```

### **Docker Compose (Frontend + Backend)**

```bash
docker compose up -d
```

---

## 🤖 **10. CI/CD with GitHub Actions**

### **Frontend CI/CD workflow**

* Lint
* Build
* Test
* Deploy to Vercel

```yaml
name: Frontend CI/CD
on: [push]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - uses: actions/setup-node@v3
        with:
          node-version: "18"

      - run: npm install --prefix frontend
      - run: npm run build --prefix frontend
```

### **Backend CI/CD workflow**

* Setup JDK
* Build Maven
* Run tests
* Publish Allure

```yaml
name: Backend CI

on: [push]

jobs:
  backend-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - uses: actions/setup-java@v3
        with:
          java-version: '21'

      - run: mvn clean test --file backend/pom.xml
      - run: cp -r backend/target/allure-results ./allure-results
```

---

## 👥 **12. Đóng góp**

Rất hoan nghênh mọi đóng góp 🎉
Quy trình chuẩn:

1. Fork repo
2. Tạo nhánh mới
3. Commit & push
4. Tạo Pull Request

---

## 📜 **13. License**

MIT License — bạn được phép sử dụng tự do cho các mục đích cá nhân & thương mại.

---
