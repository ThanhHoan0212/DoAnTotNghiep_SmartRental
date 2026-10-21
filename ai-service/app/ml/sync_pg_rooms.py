"""Script đồng bộ dữ liệu phòng trọ từ PostgreSQL (qua Spring Boot Backend API) vào AI Service.
Cập nhật catalog, huấn luyện lại Feature Encoder, sinh vector đặc trưng và lưu trữ mô hình.
"""

import json
import logging
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional
import httpx
import joblib
import numpy as np

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("sync_pg_rooms")

BASE_DIR = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(BASE_DIR))

DATA_DIR = BASE_DIR / "data"
MODELS_DIR = BASE_DIR / "app" / "models"
CATALOG_PATH = DATA_DIR / "recommendation_rooms_catalog.json"
BACKEND_BASE_URL = "http://localhost:8080/api/v1"

# Import RoomFeatureEncoder
from app.models.recommender import RoomFeatureEncoder
from app.ml.generate_recommendation_data import ALL_AMENITIES


def fetch_rooms_from_backend(backend_url: str = BACKEND_BASE_URL) -> List[Dict[str, Any]]:
    """Truy vấn toàn bộ phòng trọ từ PostgreSQL qua Backend API"""
    logger.info("Fetching rooms from backend: %s/rooms?size=1000", backend_url)
    try:
        with httpx.Client(timeout=10.0) as client:
            resp = client.get(f"{backend_url}/rooms?size=1000")
            if resp.status_code != 200:
                logger.error("Failed to fetch rooms: HTTP %d - %s", resp.status_code, resp.text)
                return []
            
            data = resp.json().get("data", {})
            room_summaries = data.get("content", [])
            logger.info("Found %d rooms in PostgreSQL database.", len(room_summaries))

            enriched_rooms = []
            for item in room_summaries:
                room_id = str(item.get("id"))
                # Lấy chi tiết để có danh sách tiện ích (amenities)
                try:
                    detail_resp = client.get(f"{backend_url}/rooms/{room_id}")
                    if detail_resp.status_code == 200:
                        detail = detail_resp.json().get("data", {})
                        amenities = [a.get("name") for a in detail.get("amenities", []) if isinstance(a, dict) and "name" in a]
                    else:
                        amenities = []
                except Exception as ex:
                    logger.warning("Could not fetch detail for room %s: %s", room_id, ex)
                    amenities = []

                room_dict = {
                    "room_id": room_id,
                    "title": item.get("title", f"Phòng trọ #{room_id}"),
                    "district": item.get("district", "TP.HCM"),
                    "price": float(item.get("price", 0.0)),
                    "area": float(item.get("area", 20.0)),
                    "address": item.get("address", ""),
                    "amenities": amenities
                }
                enriched_rooms.append(room_dict)

            return enriched_rooms
    except Exception as e:
        logger.error("Error communicating with Backend API: %s", str(e), exc_info=True)
        return []


def sync_and_rebuild_models(rooms: Optional[List[Dict[str, Any]]] = None) -> int:
    """Đồng bộ phòng trọ vào catalog và cập nhật không gian vector đặc trưng"""
    if rooms is None:
        rooms = fetch_rooms_from_backend()

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    # 1. Danh sách phòng: Ưu tiên toàn bộ phòng từ PostgreSQL lên đầu
    merged_rooms = list(rooms or [])
    seen_ids = {str(r["room_id"]) for r in merged_rooms}

    # Bổ sung các phòng mô phỏng từ synthetic_listings để tương thích với unit test & fallback
    listings_path = DATA_DIR / "synthetic_listings.csv"
    if listings_path.exists():
        import pandas as pd
        listings_df = pd.read_csv(listings_path)
        clean = listings_df[listings_df["is_fraud"] == 0]
        for _, row in clean.iterrows():
            rid = int(row["room_id"])
            if str(rid) not in seen_ids:
                merged_rooms.append({
                    "room_id": rid,
                    "title": str(row["title"]),
                    "district": str(row["district"]),
                    "price": float(row["price"]),
                    "area": float(row["area"]),
                    "amenities": ALL_AMENITIES[:4]
                })
                seen_ids.add(str(rid))

    # 2. Lưu danh mục phòng trọ kết hợp
    with open(CATALOG_PATH, "w", encoding="utf-8") as f:
        json.dump(merged_rooms, f, ensure_ascii=False, indent=2)
    logger.info("Saved %d total rooms (including %d PG rooms) to %s", len(merged_rooms), len(rooms or []), CATALOG_PATH)

    # 3. Xây dựng danh sách các quận và tiện ích
    all_districts = list({r["district"] for r in merged_rooms if r.get("district")})
    if not all_districts:
        all_districts = ["Bình Thạnh", "Quận 1", "Thủ Đức", "Gò Vấp", "Quận 7", "Cầu Giấy"]

    # Tổng hợp tất cả tiện ích
    amenity_set = set(ALL_AMENITIES)
    for r in merged_rooms:
        for am in r.get("amenities", []):
            amenity_set.add(am)
    all_amenities = sorted(list(amenity_set))

    # 4. Fit Encoder & Vectorize
    encoder = RoomFeatureEncoder(districts=all_districts, amenities=all_amenities)
    encoder.fit(merged_rooms)

    room_vectors_map = {}
    room_metadata_map = {}

    for r in merged_rooms:
        rid = r["room_id"]
        vec = encoder.transform_single(r)
        room_vectors_map[rid] = vec
        room_metadata_map[rid] = {
            "room_id": rid,
            "title": r.get("title", ""),
            "district": r.get("district", ""),
            "price": r.get("price", 0.0),
            "area": r.get("area", 0.0),
            "address": r.get("address", ""),
            "amenities": r.get("amenities", [])
        }

    # 5. Lưu lại vào các file joblib của AI Service
    joblib.dump(encoder, MODELS_DIR / "recommendation_feature_encoder.joblib")
    joblib.dump(room_vectors_map, MODELS_DIR / "recommendation_room_vectors.joblib")
    joblib.dump(room_metadata_map, MODELS_DIR / "recommendation_room_metadata.joblib")

    # Popular rooms dành cho Cold-Start: Toàn bộ phòng PostgreSQL được xếp đầu tiên
    popular_rooms = [room_metadata_map[r["room_id"]] for r in (rooms or []) if r["room_id"] in room_metadata_map]
    # Thêm các phòng khác nếu ít hơn 10 phòng
    for r in merged_rooms:
        if len(popular_rooms) >= 10:
            break
        if r["room_id"] not in [p["room_id"] for p in popular_rooms]:
            popular_rooms.append(room_metadata_map[r["room_id"]])
    joblib.dump(popular_rooms, MODELS_DIR / "popular_rooms.joblib")

    # 6. Tính toán hồ sơ người dùng (User Profiles)
    interactions_path = DATA_DIR / "synthetic_interactions.csv"
    if interactions_path.exists():
        import pandas as pd
        interactions_df = pd.read_csv(interactions_path)
        from app.ml.train_recommendation import compute_user_profile
        user_profiles = {}
        for user_id, group in interactions_df.groupby("user_id"):
            prof = compute_user_profile(group, room_vectors_map)
            if prof is not None:
                user_profiles[int(user_id)] = prof
        joblib.dump(user_profiles, MODELS_DIR / "user_profiles.joblib")
        logger.info("Saved %d User Profiles to 'app/models/user_profiles.joblib'.", len(user_profiles))

    logger.info("Successfully vectorized %d rooms into '%s'.", len(room_vectors_map), MODELS_DIR)
    print(f"\n[HOÀN TẤT] Đã đồng bộ thành công {len(rooms or [])} phòng trọ từ PostgreSQL vào AI Service!")
    for idx, r in enumerate(rooms or [], start=1):
        print(f" {idx}. [{r['room_id']}] {r['district']} - {int(r['price']):,}đ - {r['title']}")

    return len(rooms)


if __name__ == "__main__":
    count = sync_and_rebuild_models()
    print(f"\nTổng số phòng được đồng bộ: {count}")
