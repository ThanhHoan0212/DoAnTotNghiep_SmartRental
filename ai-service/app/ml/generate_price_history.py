"""Script sinh dữ liệu mô phỏng chuỗi thời gian giá thuê phòng trọ (Historical Rental Prices) tại TP.HCM.
Bao gồm dữ liệu 24 tháng phản ánh tính mùa vụ (mùa sinh viên nhập học, dịp Tết) và lạm phát hàng năm.
Phục vụ huấn luyện mô hình Dự đoán Xu hướng Giá (Price Prediction).
"""

import random
from pathlib import Path
import pandas as pd
import numpy as np

random.seed(42)
np.random.seed(42)

BASE_DIR = Path(__file__).resolve().parent.parent.parent
OUTPUT_PATH = BASE_DIR / "data" / "historical_rental_prices.csv"

DISTRICT_BASE_PRICES = {
    "Quận 1": {"base_price": 7800000, "avg_area": 28.0},
    "Quận 3": {"base_price": 7200000, "avg_area": 26.0},
    "Phú Nhuận": {"base_price": 5800000, "avg_area": 25.0},
    "Quận 10": {"base_price": 5400000, "avg_area": 24.0},
    "Bình Thạnh": {"base_price": 5200000, "avg_area": 25.0},
    "Quận 7": {"base_price": 4600000, "avg_area": 28.0},
    "Tân Bình": {"base_price": 4200000, "avg_area": 24.0},
    "Gò Vấp": {"base_price": 3500000, "avg_area": 22.0},
    "Thủ Đức": {"base_price": 3600000, "avg_area": 24.0},
    "Tân Phú": {"base_price": 3400000, "avg_area": 22.0},
    "Quận 12": {"base_price": 2800000, "avg_area": 22.0}
}

ROOM_TYPE_FACTORS = {
    "phòng trọ": 1.0,
    "chung cư mini": 1.35,
    "studio": 1.55
}

# Hệ số mùa vụ theo tháng (1-12)
SEASONALITY_FACTORS = {
    1: 0.97,   # Tháng cận Tết, nhu cầu trả phòng tăng, giá hạ nhẹ
    2: 0.96,   # Tháng Tết Nguyên Đán, giao dịch chững lại
    3: 0.99,   # Sau Tết, người thuê bắt đầu ổn định
    4: 1.00,
    5: 1.00,
    6: 1.01,
    7: 1.02,
    8: 1.06,   # Mùa sinh viên nhập học cao điểm
    9: 1.07,   # Sinh viên và tân cử nhân tìm phòng
    10: 1.04,  # Dư chấn mùa tựu trường
    11: 1.01,
    12: 0.99
}


def generate_price_history_dataset(num_months: int = 24):
    Path(OUTPUT_PATH).parent.mkdir(parents=True, exist_ok=True)
    records = []

    # Tạo danh sách các tháng trong 2 năm (2024-11 đến 2026-10)
    dates = pd.date_range(end="2026-10-01", periods=num_months, freq="MS")

    for dt in dates:
        year = dt.year
        month = dt.month
        month_idx = (year - 2024) * 12 + month  # Chỉ số bước thời gian

        # Hệ số lạm phát tích lũy (~3.5% / năm)
        inflation_mult = 1.0 + (month_idx * (0.035 / 12))
        season_mult = SEASONALITY_FACTORS[month]

        for district, d_info in DISTRICT_BASE_PRICES.items():
            base_p = d_info["base_price"]
            base_area = d_info["avg_area"]

            for r_type, r_factor in ROOM_TYPE_FACTORS.items():
                # Giá trung bình lý thuyết
                mean_price = base_p * r_factor * inflation_mult * season_mult
                # Thêm nhiễu ngẫu nhiên nhỏ (±1.5%)
                noise = random.uniform(0.985, 1.015)
                actual_price = round((mean_price * noise) / 10000) * 10000

                area_noise = random.uniform(0.95, 1.05)
                actual_area = round(base_area * area_noise, 1)

                records.append({
                    "date": dt.strftime("%Y-%m-%d"),
                    "year": year,
                    "month": month,
                    "district": district,
                    "room_type": r_type,
                    "avg_price": float(actual_price),
                    "avg_area": actual_area,
                    "avg_price_per_m2": round(actual_price / actual_area, 2),
                    "is_student_season": 1 if month in [8, 9, 10] else 0,
                    "is_tet_season": 1 if month in [1, 2] else 0,
                    "inflation_index": round(inflation_mult, 4)
                })

    df = pd.DataFrame(records)
    # Sắp xếp theo thứ tự thời gian
    df = df.sort_values(by=["district", "room_type", "date"]).reset_index(drop=True)
    df.to_csv(OUTPUT_PATH, index=False, encoding="utf-8")
    print(f"[OK] Generated {len(df)} historical price records ({num_months} months) saved to '{OUTPUT_PATH}'.")
    return df


if __name__ == "__main__":
    generate_price_history_dataset()
