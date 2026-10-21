# SmartRental AI Service

> **Dịch vụ AI độc lập** cho đồ án: *"Phát triển hệ thống tìm kiếm phòng trọ thông minh: Tích hợp AI phát hiện gian lận và dự đoán xu hướng giá thuê"*.

Service này được xây dựng bằng **Python 3.12 + FastAPI**, giao tiếp với Backend chính (**Java Spring Boot**) qua REST API nội bộ với bảo mật header `X-Internal-Api-Key`.

---

## 1. Cấu trúc thư mục

```
ai-service/
├── app/
│   ├── main.py                     # Khởi tạo FastAPI app, middleware, global error handling
│   ├── core/
│   │   ├── config.py               # Quản lý cấu hình đọc từ .env (Pydantic v2 BaseSettings)
│   │   └── security.py             # Xác thực API Key nội bộ (X-Internal-Api-Key)
│   ├── api/                        # API Routers cho từng module nghiệp vụ AI
│   │   ├── nlp_router.py           # Phân tích câu truy vấn tự nhiên
│   │   ├── chatbot_router.py       # Chatbot tư vấn phòng trọ
│   │   ├── recommendation_router.py# Gợi ý phòng trọ (Content-based filtering)
│   │   ├── fraud_router.py         # Phát hiện tin đăng gian lận / bất thường
│   │   └── price_prediction_router.py # Dự đoán xu hướng giá thuê
│   ├── schemas/                    # Pydantic v2 Request/Response Schemas
│   │   └── common.py               # Response chuẩn & Health check schema
│   ├── services/                   # Logic nghiệp vụ AI & thuật toán
│   ├── models/                     # Model đã huấn luyện (.pkl, .joblib)
│   └── ml/                         # Scripts huấn luyện & đánh giá mô hình
│       ├── train_recommendation.py
│       ├── train_fraud_detection.py
│       └── train_price_prediction.py
├── data/                           # Dataset mẫu & synthetic data phục vụ training/testing
├── tests/                          # Unit & integration tests với pytest + httpx
├── requirements.txt                # Danh sách thư viện Python
├── Dockerfile                      # Containerization cho AI Service
├── .env.example                    # Mẫu cấu hình môi trường
└── README.md
```

---

## 2. Yêu cầu hệ thống & Cài đặt

### Yêu cầu:
- Python 3.11+ (khuyến nghị Python 3.12)
- pip & venv

### Các bước cài đặt:

1. **Tạo và kích hoạt môi trường ảo (Virtual Environment):**
   ```bash
   # Tại thư mục ai-service:
   python -m venv venv

   # Kích hoạt trên Windows PowerShell:
   .\venv\Scripts\Activate.ps1

   # Kích hoạt trên Linux/macOS:
   source venv/bin/activate
   ```

2. **Cài đặt thư viện phụ thuộc:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Cấu hình môi trường:**
   Tạo file `.env` từ `.env.example`:
   ```bash
   cp .env.example .env
   ```

4. **Chạy kiểm thử (Automated Tests):**
   ```bash
   pytest
   ```

5. **Khởi chạy AI Service cục bộ:**
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   ```

---

## 3. Tài liệu API (Swagger UI)

Khi service đang chạy:
- **Swagger UI:** `http://localhost:8000/docs`
- **ReDoc:** `http://localhost:8000/redoc`
- **Health Check công khai:** `GET http://localhost:8000/health`
- **Health Check bảo mật nội bộ:** `GET http://localhost:8000/ai/health/secure` (Header: `X-Internal-Api-Key: smartrental-ai-secret-key-2026`)

---

## 4. Bảo mật & Tích hợp với Backend Spring Boot

- Giao tiếp giữa Spring Boot Backend và AI Service thông qua header:
  ```http
  X-Internal-Api-Key: smartrental-ai-secret-key-2026
  ```
- Định dạng phản hồi lỗi thống nhất:
  ```json
  {
    "success": false,
    "error_code": "ERROR_CODE_NAME",
    "message": "Mô tả chi tiết nguyên nhân lỗi"
  }
  ```

---

## 5. Danh sách các Module AI & Kết quả Thực nghiệm

Hệ thống AI Service bao gồm **6 Module hoàn chỉnh** được thiết kế khớp chuẩn với `AiServiceClient` của Spring Boot Backend:

### Module 1: Cấu trúc & Bảo mật cốt lõi
- Quản trị cấu hình tập trung qua Pydantic v2 `BaseSettings`.
- Kiểm tra bảo mật header `X-Internal-Api-Key`.
- Quản lý vòng đời startup/shutdown và error handler toàn cục.

### Module 2: Fraud Detection (Phát hiện gian lận tin đăng)
- **Endpoints:**
  - `POST /ai/fraud/check-listing`: Đánh giá rủi ro gian lận theo thang điểm 0 - 100, trả về nhãn (`approved`, `flagged_for_review`, `rejected`) và danh sách lý do khả nghi.
  - `POST /ai/fraud/sync-listing`: Đồng bộ tin đăng vào kho dữ liệu phục vụ phát hiện trùng lặp nội dung.
- **Thuật toán & Feature Engineering:**
  - Random Forest Classifier (100 estimators, class_weight='balanced') kết hợp TF-IDF n-gram và quy tắc heuristics (phát hiện giá quá thấp bất thường theo m², từ khóa lừa đảo cọc, địa chỉ ảo).
- **Kết quả thực nghiệm:**
  - Accuracy: **89.00%**
  - ROC-AUC: **0.7047**

### Module 3: Recommendation System (Gợi ý phòng trọ cá nhân hóa)
- **Endpoints:**
  - `GET /ai/recommendation/{user_id}`: Gợi ý danh sách phòng trọ cho người dùng.
  - `POST /ai/recommendation/personalized`: Gợi ý có context bổ sung (danh sách yêu thích, tin đang xem).
  - `POST /ai/recommendation/sync-interaction`: Ghi nhận tương tác (`view`, `favorite`, `contact`).
  - `POST /ai/recommendation/sync-room`: Cập nhật phòng mới vào ma trận đặc trưng.
- **Thuật toán:**
  - Content-Based Filtering với TF-IDF trên tiện ích, mô tả và khoảng cách cosine trên không gian chuẩn hóa `[price, area, district, latitude, longitude]`.
  - Chiến lược Cold-Start Popularity Recommender dựa trên điểm số tương tác có suy giảm theo thời gian.
- **Kết quả thực nghiệm:**
  - Precision@5: **0.4286** | Precision@10: **0.3000**
  - Recall@10: **0.7857**
  - MAP@10: **0.6548**

### Module 4: NLP Parse Query (Phân tích tìm kiếm tự nhiên)
- **Endpoints:**
  - `POST /ai/nlp/parse-query`: Phân tích câu tiếng Việt tự nhiên thành bộ lọc tìm kiếm chuẩn xác.
- **Đặc điểm xử lý:**
  - Xử lý phương ngữ và viết tắt phổ biến: *"q7"*, *"bthanh"*, *"3tr"*, *"dưới 4 triệu"*, *"25m2"*, *"có máy lạnh gác lửng"*.
  - Tương thích kép: Trả về cả object lồng `filters` và các trường phẳng khớp trực tiếp `ParsedQueryResponse` phía Java Spring Boot.

### Module 5: Chatbot tư vấn phòng trọ thông minh
- **Endpoints:**
  - `POST /ai/chatbot/respond`: Xử lý phản hồi đa lượt (multi-turn conversation) theo ngữ cảnh phòng đang xem hoặc khu vực quan tâm.
  - `GET /ai/chatbot/history/{session_id}`: Xem lại lịch sử trò chuyện của phiên.
- **Đặc điểm:**
  - Phân loại ý định người dùng (Hỏi chính sách thú cưng, giá điện nước/dịch vụ, tìm phòng theo khu vực/ngân sách, tư vấn thủ tục hợp đồng/đặt cọc).
  - Tự động gợi ý các phòng trọ phù hợp ngay trong câu trả lời nếu người dùng có nhu cầu tìm kiếm.

### Module 6: Price Prediction & Trend Analysis (Dự đoán giá & xu hướng thị trường)
- **Endpoints:**
  - `GET /ai/price-prediction?district=Quận 7&room_type=phòng trọ`: Dự đoán giá thuê trung bình tháng kế tiếp kèm khoảng tin cậy 95% ($y \pm 1.96 \cdot \text{RMSE}$) và chiều hướng xu hướng (`increasing`, `decreasing`, `stable`).
  - `GET /ai/price-prediction/market-overview`: Báo cáo tổng quan giá sàn, giá trần, giá trung bình theo m² và xu hướng trên 11 quận trọng điểm tại TP.HCM.
- **Mô hình & Dữ liệu huấn luyện:**
  - Dataset chuỗi thời gian 24 tháng phản ánh tính mùa vụ sinh viên nhập học (tháng 8 - 10) và biến động sau Tết Nguyên Đán.
  - Thuật toán Gradient Boosting Regressor với đặc trưng trễ (lags $t-1, t-2, t-3$) và đường trung bình động (rolling mean 3 tháng).
- **Kết quả thực nghiệm:**
  - **MAE (Mean Absolute Error):** 63,942 VND
  - **RMSE (Root Mean Squared Error):** 82,288 VND
  - **MAPE (Mean Absolute Percentage Error):** 1.21%
  - **$R^2$ Score:** **0.9944**

---

## 6. Lệnh huấn luyện lại mô hình (Retrain Scripts)

Khi có thêm dữ liệu mới từ Backend, có thể chạy lại các script huấn luyện:

```bash
# Huấn luyện mô hình phát hiện gian lận:
python -m app.ml.train_fraud_detection

# Huấn luyện mô hình gợi ý phòng trọ:
python -m app.ml.train_recommendation

# Huấn luyện mô hình dự đoán xu hướng giá:
python -m app.ml.train_price_prediction
```
