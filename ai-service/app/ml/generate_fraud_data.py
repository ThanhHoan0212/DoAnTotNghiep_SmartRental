"""Script sinh dữ liệu mô phỏng (Synthetic Data Generator) phục vụ huấn luyện và đánh giá mô hình AI Phát hiện Gian lận (Fraud Detection).
Dữ liệu phản ánh thực tế phân phối giá thuê trọ tại các quận thuộc TP. Hồ Chí Minh.
"""

import os
import random
from pathlib import Path
import pandas as pd
import numpy as np

# Thiết lập seed để kết quả sinh dữ liệu tái lập được (Reproducible)
random.seed(42)
np.random.seed(42)

DISTRICT_PRICE_RANGES = {
    "Quận 1": {"price_per_m2_min": 250000, "price_per_m2_max": 450000, "min_area": 18, "max_area": 45},
    "Quận 3": {"price_per_m2_min": 220000, "price_per_m2_max": 400000, "min_area": 18, "max_area": 40},
    "Quận 10": {"price_per_m2_min": 180000, "price_per_m2_max": 320000, "min_area": 16, "max_area": 38},
    "Bình Thạnh": {"price_per_m2_min": 170000, "price_per_m2_max": 300000, "min_area": 16, "max_area": 40},
    "Phú Nhuận": {"price_per_m2_min": 190000, "price_per_m2_max": 320000, "min_area": 18, "max_area": 38},
    "Quận 7": {"price_per_m2_min": 160000, "price_per_m2_max": 280000, "min_area": 18, "max_area": 42},
    "Tân Bình": {"price_per_m2_min": 150000, "price_per_m2_max": 260000, "min_area": 16, "max_area": 35},
    "Gò Vấp": {"price_per_m2_min": 120000, "price_per_m2_max": 220000, "min_area": 15, "max_area": 35},
    "Thủ Đức": {"price_per_m2_min": 120000, "price_per_m2_max": 200000, "min_area": 15, "max_area": 35},
    "Quận 12": {"price_per_m2_min": 100000, "price_per_m2_max": 180000, "min_area": 15, "max_area": 30},
    "Bình Tân": {"price_per_m2_min": 100000, "price_per_m2_max": 170000, "min_area": 15, "max_area": 30},
}

NORMAL_TITLE_TEMPLATES = [
    "Cho thuê phòng trọ khép kín mới xây tại {district}",
    "Phòng trọ cao cấp full nội thất gần các trường đại học tại {district}",
    "Studio hiện đại, giờ giấc tự do, có gác lửng tại {district}",
    "Phòng trọ có ban công thoáng mát, an ninh tốt khu vực {district}",
    "Căn hộ mini sạch sẽ, tiện nghi đầy đủ, bảo vệ 24/7 ở {district}",
    "Phòng trọ giá tốt cho sinh viên và người đi làm ở {district}",
    "Cho thuê phòng trọ riêng tư, không chung chủ tại {district}"
]

NORMAL_DESC_TEMPLATES = [
    "Vị trí đắc địa tại {district}, thuận tiện di chuyển ra trung tâm. Phòng có diện tích {area}m2, trang bị sẵn máy lạnh, tủ quần áo, nệm cao cấp. Có bảo vệ trông xe, khóa vân tay, camera an ninh 24/7. Điện nước tính theo công tơ nhà nước.",
    "Cần cho thuê gấp phòng tại {district}. Diện tích {area}m2 thoáng mát, cửa sổ lấy sáng tự nhiên. Tiện ích: máy giặt dùng chung, wifi tốc độ cao, thang máy, khu vực để xe rộng rãi không thu thêm phí. Giá thuê {price} đ/tháng, cọc 1 tháng.",
    "Phòng trọ khép kín {area}m2 ở trung tâm {district}. Nội thất gồm điều hòa inverter, bình nóng lạnh, kệ bếp nấu ăn, bồn rửa sạch sẽ. Khu dân cư văn minh, yên tĩnh, an ninh tuyệt đối.",
    "Chính chủ cho thuê phòng trọ diện tích {area}m2 tại {district}. Giờ giấc hoàn toàn tự do, bạn bè đến chơi thoải mái. Trang bị đầy đủ giường, tủ, máy lạnh mới 100%. Ưu tiên hợp đồng thuê dài hạn 6 tháng - 1 năm."
]

SUSPICIOUS_PHRASES = [
    "chuyển khoản cọc trước qua zalo để giữ phòng gấp",
    "chỉ nhận cọc online vì chủ nhà đang ở xa",
    "phòng đẹp giá rẻ hot nhất tuần cọc ngay kẻo hết",
    "yêu cầu cọc trước 2 triệu mới cho xem phòng thực tế",
    "liên hệ qua zalo không gọi trực tiếp cọc trước nhận phòng",
    "cọc giữ chân trước 50% tiền thuê nhận phòng ngay trong ngày"
]


def generate_synthetic_dataset(num_samples: int = 1200, output_path: str = "data/synthetic_listings.csv"):
    Path(output_path).parent.mkdir(parents=True, exist_ok=True)
    records = []
    districts = list(DISTRICT_PRICE_RANGES.keys())

    # 1. Sinh các tin đăng bình thường (Normal / Legitimate: ~88%)
    normal_count = int(num_samples * 0.88)
    for i in range(1, normal_count + 1):
        dist = random.choice(districts)
        dist_info = DISTRICT_PRICE_RANGES[dist]

        area = round(random.uniform(dist_info["min_area"], dist_info["max_area"]), 1)
        base_price_per_m2 = random.uniform(dist_info["price_per_m2_min"], dist_info["price_per_m2_max"])
        # Thêm dao động ngẫu nhiên ±10%
        price = round((base_price_per_m2 * area) * random.uniform(0.9, 1.1) / 50000) * 50000

        title = random.choice(NORMAL_TITLE_TEMPLATES).format(district=dist)
        desc = random.choice(NORMAL_DESC_TEMPLATES).format(
            district=dist, area=area, price=f"{int(price):,}"
        )

        records.append({
            "room_id": i,
            "title": title,
            "description": desc,
            "price": float(price),
            "district": dist,
            "area": area,
            "price_per_m2": round(price / area, 2),
            "is_fraud": 0,
            "fraud_type": "NONE"
        })

    # 2. Sinh các tin đăng gian lận giả lập (Fraud Anomaly: ~12%)
    fraud_count = num_samples - normal_count
    for j in range(normal_count + 1, normal_count + fraud_count + 1):
        fraud_type = random.choice(["EXTREME_LOW_PRICE", "EXTREME_HIGH_PRICE", "SUSPICIOUS_CONTENT", "DUPLICATE_COPY"])
        dist = random.choice(districts)
        dist_info = DISTRICT_PRICE_RANGES[dist]
        area = round(random.uniform(dist_info["min_area"], dist_info["max_area"]), 1)

        normal_price = ((dist_info["price_per_m2_min"] + dist_info["price_per_m2_max"]) / 2) * area

        if fraud_type == "EXTREME_LOW_PRICE":
            # Giá siêu rẻ (chỉ bằng 20% - 35% giá thị trường) nhằm lừa cọc
            price = round((normal_price * random.uniform(0.2, 0.35)) / 50000) * 50000
            title = f"GẤP! Căn hộ luxury {dist} giá sốc chỉ {int(price):,}đ/tháng"
            desc = f"Phòng siêu đẹp full nội thất cao cấp tại {dist}, diện tích {area}m2. Cần người ở ngay giảm sâu chỉ còn {int(price):,}đ. {random.choice(SUSPICIOUS_PHRASES)}."

        elif fraud_type == "EXTREME_HIGH_PRICE":
            # Giá ảo cực cao (gấp 3.5 - 5 lần bình thường)
            price = round((normal_price * random.uniform(3.5, 5.0)) / 100000) * 100000
            title = f"Phòng trọ bình dân giá đặc biệt tại {dist}"
            desc = f"Cho thuê phòng {area}m2 tại {dist}, giá {int(price):,}đ/tháng."

        elif fraud_type == "SUSPICIOUS_CONTENT":
            # Giá trong ngưỡng nhưng nội dung chứa câu từ lừa đảo đặt cọc
            price = round((normal_price * random.uniform(0.85, 1.15)) / 50000) * 50000
            title = f"Phòng trọ giá tốt {dist} cọc ngay"
            desc = f"Phòng đẹp tại {dist}, diện tích {area}m2 giá {int(price):,}đ. {random.choice(SUSPICIOUS_PHRASES)}. Ai liên hệ trước ưu tiên."

        else: # DUPLICATE_COPY
            # Copy nguyên văn mô tả từ một tin đăng bình thường trước đó
            reference = random.choice(records[:normal_count])
            price = reference["price"]
            area = reference["area"]
            dist = reference["district"]
            title = reference["title"]
            desc = reference["description"] + " (Liên hệ ngay chính chủ)."

        records.append({
            "room_id": j,
            "title": title,
            "description": desc,
            "price": float(price),
            "district": dist,
            "area": area,
            "price_per_m2": round(price / area, 2),
            "is_fraud": 1,
            "fraud_type": fraud_type
        })

    df = pd.DataFrame(records)
    # Xáo trộn dữ liệu ngẫu nhiên
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    df.to_csv(output_path, index=False, encoding="utf-8")
    print(f"[OK] Generated {len(df)} synthetic room listings saved to '{output_path}'.")
    print(f"     Normal listings: {len(df[df['is_fraud'] == 0])}")
    print(f"     Fraud listings:  {len(df[df['is_fraud'] == 1])}")
    return df


if __name__ == "__main__":
    generate_synthetic_dataset()
