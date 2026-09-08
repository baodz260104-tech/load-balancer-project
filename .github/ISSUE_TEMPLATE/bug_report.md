---
name: Báo cáo lỗi (Bug report)
about: Tạo báo cáo lỗi phát sinh trong quá trình chạy Load Balancer
title: '[BUG] '
labels: bug
---

**Mô tả lỗi:**
Mô tả ngắn gọn về hành vi bất thường của hệ thống.

**Các bước tái hiện lỗi:**
1. Khởi động Load Balancer tại cổng 8000
2. Gửi request: `curl http://localhost:8000/...`
3. Nhận mã phản hồi lỗi...

**Kết quả kỳ vọng:**
Hệ thống nên chuyển tiếp thành công tới backend còn sống.