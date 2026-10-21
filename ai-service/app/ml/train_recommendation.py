"""Script huấn luyện và đánh giá mô hình Hệ thống Gợi ý Phòng trọ (Recommendation System).
Áp dụng Content-Based Filtering với Vector đặc trưng kết hợp (Giá, Diện tích, Quận, Tiện ích).
Đánh giá chất lượng gợi ý bằng các chỉ số học thuật: Precision@K, Recall@K, MAP@K (Mean Average Precision).
Lưu trữ mô hình và hồ sơ sở thích vào 'app/models/'.
"""

import sys
from pathlib import Path

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

import json
import joblib
import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity
from app.models.recommender import RoomFeatureEncoder
from app.ml.generate_recommendation_data import ALL_AMENITIES, generate_recommendation_dataset

BASE_DIR = Path(__file__).resolve().parent.parent.parent
CATALOG_PATH = BASE_DIR / "data" / "recommendation_rooms_catalog.json"
INTERACTIONS_PATH = BASE_DIR / "data" / "synthetic_interactions.csv"
MODELS_DIR = BASE_DIR / "app" / "models"


def compute_user_profile(user_interactions: pd.DataFrame, room_vectors_map: dict) -> np.ndarray:
    """Tính vector hồ sơ sở thích người dùng từ trung bình có trọng số các phòng đã tương tác"""
    weighted_vectors = []
    total_weights = 0.0

    for _, row in user_interactions.iterrows():
        room_id = int(row["room_id"])
        weight = float(row.get("weight", 1.0))
        if room_id in room_vectors_map:
            weighted_vectors.append(room_vectors_map[room_id] * weight)
            total_weights += weight

    if not weighted_vectors or total_weights == 0:
        return None

    user_profile = np.sum(weighted_vectors, axis=0) / total_weights
    return user_profile


def evaluate_recommendations(train_interactions: pd.DataFrame, test_interactions: pd.DataFrame,
                            room_vectors_map: dict, room_ids_list: list, k_values: list = [5, 10]):
    """Đánh giá hiệu năng hệ thống gợi ý với Precision@K, Recall@K, MAP@K trên tập Test"""
    precision_k = {k: [] for k in k_values}
    recall_k = {k: [] for k in k_values}
    map_k = {k: [] for k in k_values}

    candidate_matrix = np.array([room_vectors_map[rid] for rid in room_ids_list])

    # Nhóm tương tác theo user
    test_grouped = test_interactions.groupby("user_id")

    for user_id, test_group in test_grouped:
        train_user = train_interactions[train_interactions["user_id"] == user_id]
        if train_user.empty:
            continue

        user_vec = compute_user_profile(train_user, room_vectors_map)
        if user_vec is None:
            continue

        actual_room_ids = set(test_group["room_id"].tolist())
        already_seen = set(train_user["room_id"].tolist())

        # Tính Cosine Similarity
        sims = cosine_similarity([user_vec], candidate_matrix)[0]
        ranked_indices = np.argsort(-sims)

        # Lọc bỏ các phòng user đã tương tác trong tập Train
        recommendations = []
        for idx in ranked_indices:
            candidate_id = room_ids_list[idx]
            if candidate_id not in already_seen:
                recommendations.append(candidate_id)
            if len(recommendations) >= max(k_values):
                break

        # Tính toán Precision@K, Recall@K, AP@K
        for k in k_values:
            top_k = recommendations[:k]
            hits = len(set(top_k) & actual_room_ids)
            p_at_k = hits / k
            r_at_k = hits / len(actual_room_ids) if len(actual_room_ids) > 0 else 0

            precision_k[k].append(p_at_k)
            recall_k[k].append(r_at_k)

            # Average Precision @ K
            hit_count = 0
            score_sum = 0.0
            for rank, r_id in enumerate(top_k, 1):
                if r_id in actual_room_ids:
                    hit_count += 1
                    score_sum += hit_count / rank
            ap_at_k = score_sum / min(len(actual_room_ids), k) if min(len(actual_room_ids), k) > 0 else 0
            map_k[k].append(ap_at_k)

    return {
        "precision": {k: float(np.mean(precision_k[k])) for k in k_values},
        "recall": {k: float(np.mean(recall_k[k])) for k in k_values},
        "map": {k: float(np.mean(map_k[k])) for k in k_values}
    }


def train_and_evaluate():
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    if not CATALOG_PATH.exists() or not INTERACTIONS_PATH.exists():
        print("[INFO] Recommendation datasets not found. Generating...")
        rooms_catalog, interactions_df = generate_recommendation_dataset()
    else:
        with open(CATALOG_PATH, "r", encoding="utf-8") as f:
            rooms_catalog = json.load(f)
        interactions_df = pd.read_csv(INTERACTIONS_PATH)
        print(f"[INFO] Loaded {len(rooms_catalog)} rooms and {len(interactions_df)} interactions.")

    # 1. Trích xuất danh sách tất cả các quận
    all_districts = list({r["district"] for r in rooms_catalog})

    # 2. Xây dựng và Huấn luyện RoomFeatureEncoder
    encoder = RoomFeatureEncoder(districts=all_districts, amenities=ALL_AMENITIES)
    encoder.fit(rooms_catalog)

    room_vectors_map = {}
    room_metadata_map = {}
    for r in rooms_catalog:
        vec = encoder.transform_single(r)
        room_vectors_map[r["room_id"]] = vec
        room_metadata_map[r["room_id"]] = {
            "room_id": r["room_id"],
            "title": r["title"],
            "district": r["district"],
            "price": r["price"],
            "area": r["area"],
            "amenities": r.get("amenities", [])
        }

    joblib.dump(encoder, MODELS_DIR / "recommendation_feature_encoder.joblib")
    joblib.dump(room_vectors_map, MODELS_DIR / "recommendation_room_vectors.joblib")
    joblib.dump(room_metadata_map, MODELS_DIR / "recommendation_room_metadata.joblib")
    print(f"[OK] Saved RoomFeatureEncoder and {len(room_vectors_map)} room vectors to 'app/models/'.")

    # 3. Phân chia Train/Test tương tác người dùng (80/20 theo thời gian/ngẫu nhiên)
    interactions_df = interactions_df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    train_size = int(len(interactions_df) * 0.8)
    train_df = interactions_df.iloc[:train_size]
    test_df = interactions_df.iloc[train_size:]

    # 4. Tính toán hồ sơ sở thích toàn bộ người dùng (User Profiles)
    user_profiles = {}
    for user_id, group in interactions_df.groupby("user_id"):
        prof = compute_user_profile(group, room_vectors_map)
        if prof is not None:
            user_profiles[int(user_id)] = prof

    joblib.dump(user_profiles, MODELS_DIR / "user_profiles.joblib")
    print(f"[OK] Saved {len(user_profiles)} User Profiles to 'app/models/user_profiles.joblib'.")

    # 5. Xác định danh sách phòng phổ biến cho Cold-Start
    interaction_counts = interactions_df["room_id"].value_counts()
    popular_room_ids = interaction_counts.head(20).index.tolist()
    popular_rooms = [room_metadata_map[rid] for rid in popular_room_ids if rid in room_metadata_map]
    joblib.dump(popular_rooms, MODELS_DIR / "popular_rooms.joblib")
    print(f"[OK] Saved {len(popular_rooms)} popular rooms for Cold-Start strategy.")

    # 6. Đánh giá chất lượng gợi ý trên tập Test
    room_ids_list = list(room_vectors_map.keys())
    metrics = evaluate_recommendations(train_df, test_df, room_vectors_map, room_ids_list, k_values=[5, 10])

    print("\n" + "=" * 65)
    print("   BÁO CÁO KẾT QUẢ THỰC NGHIỆM - RECOMMENDATION SYSTEM")
    print("=" * 65)
    print(f"Tổng số người dùng tham gia đánh giá: {len(test_df['user_id'].unique())}")
    print(f"Thuật toán cốt lõi: Content-Based Filtering (Cosine Similarity)")
    print("-" * 65)
    print(f"Chỉ số Precision@5:   {metrics['precision'][5] * 100:.2f}%")
    print(f"Chỉ số Precision@10:  {metrics['precision'][10] * 100:.2f}%")
    print(f"Chỉ số Recall@10:     {metrics['recall'][10] * 100:.2f}%")
    print(f"Chỉ số MAP@10 (Mean Average Precision): {metrics['map'][10] * 100:.2f}%")
    print("=" * 65)

    return metrics


if __name__ == "__main__":
    train_and_evaluate()
