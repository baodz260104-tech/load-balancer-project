# Application-Level Software Load Balancer (L7)

Dự án nghiên cứu và hiện thực một bộ cân bằng tải mức ứng dụng (Layer 7 Load Balancer) bằng Java thuần, đóng vai trò như một Reverse Proxy điều phối lưu lượng truy cập HTTP đồng đều đến cụm máy chủ backend.

---

## 1. Mục tiêu dự án
- Hiện thực cơ chế **Reverse Proxy** chuyển tiếp HTTP Request và Response hai chiều.
- Cài đặt và thực nghiệm các thuật toán cân bằng tải cốt lõi: **Round Robin**, **Least Connections**, **IP Hash**.
- Xây dựng cơ chế **Active Health Check** chạy ngầm để phát hiện máy chủ bị crash và tự động cô lập/khôi phục máy chủ trong cụm.
- Đo kiểm hiệu năng và độ trễ (latency, throughput) khi có và không có Load Balancer.

---

## 2. Công nghệ sử dụng
- **Ngôn ngữ:** Java (JDK 21) tận dụng Virtual Threads / Non-blocking I/O.
- **Quản lý dự án & Build:** Apache Maven.
- **Hạ tầng thử nghiệm:** Docker & Docker Compose (cụm 4 backend HTTP Echo).
- **Kiểm thử tự động:** JUnit 5.
- **Tích hợp liên tục (CI):** GitHub Actions.

---

## 3. Cấu trúc hệ thống
```text
[Client] 
   │
   ▼
[Load Balancer :8000]
   ├── Thuật toán định tuyến (Round-Robin Engine)
   └── Giám sát trạng thái (Background Health Checker)
         │
         ├───▶ Backend 1 (:8081)
         ├───▶ Backend 2 (:8082)
         ├───▶ Backend 3 (:8083)
         └───▶ Backend 4 (:8084)