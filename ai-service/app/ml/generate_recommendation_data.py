"""Script sinh dữ liệu mô phỏng tương tác người dùng (User Interactions) phục vụ hệ thống Recommendation.
Tạo các hồ sơ sở thích người dùng (sinh viên, người đi làm, gia đình/pet) để kiểm thử thuật toán Content-Based.
"""

import json
import random
from pathlib import Path
import pandas as pd
import numpy as np

random.seed(42)
np.random.seed(42)

BASE_DIR = Path(__file__).resolve().parent.parent.parent
LISTINGS_PATH = BASE_DIR / "data" / "synthetic_listings.csv"
OUTPUT_INTERACTIONS = BASE_DIR / "data" / "synthetic_interactions.csv"
OUTPUT_ROOMS_CATALOG = BASE_DIR / "data" / "recommendation_rooms_catalog.json"

ALL_AMENITIES = [
    "Máy lạnh",
    "Tủ lạnh",
    "Máy giặt",
    "Gác lửng",
    "Ban công",
    "Bảo vệ 24/7",
    "Thang máy",
    "Cho nuôi thú cưng",
    "Khóa vân tay",
    "Chỗ để xe",
    "Wifi miễn phí"
]

USER_PERSONAS = [
    {
        "name": "STUDENT_BUDGET",
        "districts": ["Gò Vấp", "Thủ Đức", "Tân Bình", "Quận 12"],
        "max_price": 3800000,
        "desired_amenities": ["Gác lửng", "Wifi miễn phí", "Chỗ để xe", "Máy giặt"]
    },
    {
        "name": "YOUNG_PROFESSIONAL",
        "districts": ["Quận 1", "Quận 3", "Bình Thạnh", "Phú Nhuận"],
        "max_price": 9500000,
        "desired_amenities": ["Máy lạnh", "Ban công", "Thang máy", "Khóa vân tay", "Bảo vệ 24/7"]
    },
    {
        "name": "MID_TIER_PET_LOVER",
        "districts": ["Quận 7", "Quận 10", "Bình Thạnh"],
        "max_price": 6500000,
        "desired_amenities": ["Cho nuôi thú cưng", "Máy lạnh", "Tủ lạnh", "Ban công", "Chỗ để xe"]
    }
]


def generate_recommendation_dataset():
    if not LISTINGS_PATH.exists():
        from app.ml.generate_fraud_data import generate_synthetic_dataset
        generate_synthetic_dataset(output_path=str(LISTINGS_PATH))

    listings_df = pd.read_csv(LISTINGS_PATH)
    # Chỉ lấy các tin đăng hợp lệ (is_fraud == 0) để gợi ý
    clean_rooms = listings_df[listings_df["is_fraud"] == 0].copy()

    # Gán tiện ích thực tế cho từng phòng
    rooms_catalog = []
    for _, row in clean_rooms.iterrows():
        price = float(row["price"])
        dist = row["district"]
        # Phòng giá cao thường có nhiều tiện ích hơn
        if price >= 6000000:
            num_amenities = random.randint(5, 8)
        elif price >= 3500000:
            num_amenities = random.randint(3, 5)
        else:
            num_amenities = random.randint(2, 4)

        amenities = random.sample(ALL_AMENITIES, k=num_amenities)
        rooms_catalog.append({
            "room_id": int(row["room_id"]),
            "title": str(row["title"]),
            "district": dist,
            "price": price,
            "area": float(row["area"]),
            "amenities": amenities
        })

    # Lưu catalog phòng vào json
    with open(OUTPUT_ROOMS_CATALOG, "w", encoding="utf-8") as f:
        json.dump(rooms_catalog, f, ensure_ascii=False, indent=2)
    print(f"[OK] Generated room catalog with {len(rooms_catalog)} rooms saved to '{OUTPUT_ROOMS_CATALOG}'.")

    # Sinh 200 người dùng với các tương tác theo persona
    num_users = 200
    interactions = []
    action_types = [("VIEW", 1.0), ("FAVORITE", 3.0), ("CONTACT", 2.0)]

    for user_id in range(1, num_users + 1):
        persona = random.choice(USER_PERSONAS)
        preferred_districts = persona["districts"]
        max_p = persona["max_price"]
        desired_ams = set(persona["desired_amenities"])

        # Lọc các phòng phù hợp với persona
        matching_candidates = [
            r for r in rooms_catalog
            if r["district"] in preferred_districts and r["price"] <= max_p
        ]
        if not matching_candidates:
            matching_candidates = rooms_catalog

        # Mỗi user có từ 5 đến 20 tương tác
        num_interactions = random.randint(5, 20)
        selected_rooms = random.sample(matching_candidates, k=min(num_interactions, len(matching_candidates)))

        for r in selected_rooms:
            # Chọn loại hành động theo trọng số thực tế (VIEW nhiều nhất, FAVORITE ít hơn)
            action, weight = random.choices(
                action_types,
                weights=[0.60, 0.25, 0.15],
                k=1
            )[0]

            interactions.append({
                "user_id": user_id,
                "room_id": r["room_id"],
                "action": action,
                "weight": weight,
                "district": r["district"],
                "price": r["price"],
                "persona": persona["name"]
            })

    interactions_df = pd.DataFrame(interactions)
    interactions_df.to_csv(OUTPUT_INTERACTIONS, index=False, encoding="utf-8")
    print(f"[OK] Generated {len(interactions_df)} interactions for {num_users} users saved to '{OUTPUT_INTERACTIONS}'.")
    return rooms_catalog, interactions_df


if __name__ == "__main__":
    generate_recommendation_dataset()
