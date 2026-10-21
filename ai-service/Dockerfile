FROM python:3.12-slim

# Ngăn Python tạo bytecode .pyc và buffer stdout
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# Cài đặt system dependencies nếu cần build thư viện C/C++
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy và cài đặt python requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code và data
COPY . .

# Expose port mặc định của FastAPI
EXPOSE 8000

# Chạy Uvicorn production
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
