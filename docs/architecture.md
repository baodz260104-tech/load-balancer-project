# Sơ Đồ Kiến Trúc Hệ Thống (System Architecture)

Hệ thống cân bằng tải mức ứng dụng (L7 Reverse Proxy) được thiết kế theo mô hình kiến trúc dưới đây:

## 1. Sơ đồ luồng phân phối tải tổng thể

```mermaid
flowchart TD
    subgraph Clients [Người dùng / Kiểm thử]
        C1[Client 1]
        C2[Client 2]
        C3[Apache Bench / k6]
    end

    LB[Load Balancer Entrypoint :8000]

    subgraph CoreEngine [Load Balancer Internal Engine]
        direction TB
        ProxyHandler[Reverse Proxy Handler]
        Strategy[Load Balancing Engine<br/>Round Robin / Least Connections]
        HealthCheck[Active Health Checker<br/>Scheduled Worker]
        ServerPool[(Active Backend Pool)]

        ProxyHandler --> Strategy
        Strategy --> ServerPool
        HealthCheck -. Cập nhật trạng thái .-> ServerPool
    end

    subgraph BackendCluster [Cụm Máy Chủ Backend]
        B1["Backend 1 (:8081)"]
        B2["Backend 2 (:8082)"]
        B3["Backend 3 (:8083)"]
        B4["Backend 4 (:8084)"]
    end

    C1 -->|HTTP Request| LB
    C2 -->|HTTP Request| LB
    C3 -->|High Concurrency| LB

    LB --> ProxyHandler

    ServerPool ==>|Forward Request| B1
    ServerPool ==>|Forward Request| B2
    ServerPool ==>|Forward Request| B3
    ServerPool ==>|Forward Request| B4

    HealthCheck -. Ping /health .-> B1
    HealthCheck -. Ping /health .-> B2
    HealthCheck -. Ping /health .-> B3
    HealthCheck -. Ping /health .-> B4
```

---

## 2. Chi tiết các thành phần

- **Reverse Proxy Handler:** Nhận kết nối HTTP từ Client qua cổng `:8000`, phân tích Headers, gắn thêm thông tin proxy (`X-Forwarded-For`) và chuyển tiếp tới Backend được chọn.
- **Load Balancing Engine:** Chứa logic thuật toán định tuyến để chọn ra backend phù hợp nhất từ danh sách máy chủ còn hoạt động.
- **Active Health Checker:** Luồng chạy nền (Background Worker) định kỳ ping tới endpoint `/health` của các backend. Nếu server gặp sự cố hoặc timeout, tiến trình sẽ tạm thời loại server đó khỏi danh sách để tránh gửi request của người dùng vào máy chết.
- **Backend Cluster:** Cụm các máy chủ dịch vụ thực thi xử lý nghiệp vụ, chạy độc lập trên các cổng khác nhau.