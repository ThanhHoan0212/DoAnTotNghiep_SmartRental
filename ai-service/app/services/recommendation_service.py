import logging
from pathlib import Path
from typing import Any, Dict, List, Optional, Union
import joblib
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from app.core.config import settings
from app.models.recommender import RoomFeatureEncoder
from app.schemas.recommendation import (
    PersonalizedRecommendRequest,
    RecommendationItem,
    RecommendationResponse,
    RoomFeatureItem,
    UserInteractionSync,
)

logger = logging.getLogger("ai_service.recommendation")


class RecommendationService:
    def __init__(self):
        self.models_dir = settings.MODELS_DIR
        self.encoder = None
        self.room_vectors_map: Dict[Union[str, int], np.ndarray] = {}
        self.room_metadata_map: Dict[Union[str, int], Dict[str, Any]] = {}
        self.user_profiles: Dict[Union[str, int], np.ndarray] = {}
        self.popular_rooms: List[Dict[str, Any]] = []
        self.load_models()


    def load_models(self):
        """Tải các ma trận đặc trưng phòng và hồ sơ sở thích người dùng vào bộ nhớ"""
        try:
            encoder_path = self.models_dir / "recommendation_feature_encoder.joblib"
            vectors_path = self.models_dir / "recommendation_room_vectors.joblib"
            meta_path = self.models_dir / "recommendation_room_metadata.joblib"
            profiles_path = self.models_dir / "user_profiles.joblib"
            popular_path = self.models_dir / "popular_rooms.joblib"

            if encoder_path.exists():
                self.encoder = joblib.load(encoder_path)
            if vectors_path.exists():
                self.room_vectors_map = joblib.load(vectors_path)
            if meta_path.exists():
                self.room_metadata_map = joblib.load(meta_path)
            if profiles_path.exists():
                self.user_profiles = joblib.load(profiles_path)
            if popular_path.exists():
                self.popular_rooms = joblib.load(popular_path)

            logger.info(
                "Recommendation models loaded: %d rooms, %d user profiles, %d popular rooms.",
                len(self.room_vectors_map),
                len(self.user_profiles),
                len(self.popular_rooms)
            )
        except Exception as e:
            logger.error("Error loading recommendation models: %s", str(e), exc_info=True)

    def _build_candidate_pool(self, district: Optional[str] = None, max_price: Optional[float] = None) -> List[Union[str, int]]:
        """Lọc danh sách các ID phòng hợp lệ theo quận hoặc mức giá nếu được chỉ định"""
        candidate_ids = []
        for rid, meta in self.room_metadata_map.items():
            if district and meta.get("district", "").strip().lower() != district.strip().lower():
                continue
            if max_price and meta.get("price", 0) > max_price:
                continue
            candidate_ids.append(rid)

        # Nếu lọc quá chặt không có phòng nào, mở rộng toàn bộ
        if not candidate_ids:
            candidate_ids = list(self.room_metadata_map.keys())

        return candidate_ids

    def _generate_reason(self, meta: Dict[str, Any], score: float) -> str:
        """Sinh câu giải thích lý do gợi ý tự nhiên bằng tiếng Việt cho người dùng"""
        dist = meta.get("district", "")
        price = meta.get("price", 0)
        amenities = meta.get("amenities", [])
        highlight_am = ", ".join(amenities[:2]) if amenities else "tiện ích đầy đủ"

        if score >= 0.85:
            return f"Rất phù hợp với sở thích của bạn tại khu vực {dist}, giá {int(price):,}đ/tháng và có {highlight_am}."
        elif score >= 0.70:
            return f"Phù hợp với khu vực {dist} và các tiện ích ({highlight_am}) bạn thường quan tâm."
        else:
            return f"Phòng trọ tại {dist} có mức giá phù hợp với ngân sách tìm kiếm của bạn."

    def get_recommendations(
        self,
        user_id: Union[str, int],
        limit: int = 10,
        district: Optional[str] = None
    ) -> RecommendationResponse:
        """
        Gợi ý phòng trọ cho người dùng theo user_id:
        - Nếu user có lịch sử: Áp dụng Content-Based Filtering (Cosine Similarity).
        - Nếu user mới (Cold-Start): Trả về phòng phổ biến nhất (Popular Rooms).
        """
        # Kiểm tra xem user đã có vector sở thích chưa
        user_vec = self.user_profiles.get(user_id)

        # 1. Trường hợp Cold-Start (Người dùng mới chưa có dữ liệu tương tác)
        if user_vec is None or not self.room_vectors_map:
            return self._handle_cold_start(user_id, limit, district)

        # 2. Trường hợp Content-Based Filtering
        candidate_ids = self._build_candidate_pool(district=district)
        candidate_matrix = np.array([self.room_vectors_map[rid] for rid in candidate_ids])

        # Tính toán Cosine Similarity
        sims = cosine_similarity([user_vec], candidate_matrix)[0]
        sorted_indices = np.argsort(-sims)[:limit]

        items: List[RecommendationItem] = []
        recommended_ids: List[Union[str, int]] = []

        for idx in sorted_indices:
            rid = candidate_ids[idx]
            score = float(sims[idx])
            meta = self.room_metadata_map.get(rid, {})

            recommended_ids.append(rid)
            items.append(RecommendationItem(
                room_id=rid,
                title=meta.get("title", f"Phòng trọ #{rid}"),
                score=round(max(0.0, min(1.0, score)), 4),
                district=meta.get("district"),
                price=meta.get("price"),
                area=meta.get("area"),
                reason=self._generate_reason(meta, score)
            ))

        return RecommendationResponse(
            user_id=user_id,
            recommended_room_ids=recommended_ids,
            strategy="content_based",
            items=items,
            recommendations=items
        )

    def _handle_cold_start(self, user_id: Union[str, int], limit: int, district: Optional[str] = None) -> RecommendationResponse:
        """Xử lý Cold-Start: trả về phòng phổ biến hoặc phòng chất lượng cao từ database"""
        candidates = self.popular_rooms
        if district:
            district_candidates = [
                r for r in candidates
                if r.get("district", "").strip().lower() == district.strip().lower()
            ]
            if district_candidates:
                candidates = district_candidates

        # Nếu danh sách popular chưa có, lấy từ metadata
        if not candidates:
            candidates = list(self.room_metadata_map.values())

        selected = candidates[:limit]
        items = []
        recommended_ids = []

        for r in selected:
            rid = r["room_id"]
            recommended_ids.append(rid)
            dist = r.get("district", "")
            items.append(RecommendationItem(
                room_id=rid,
                title=r.get("title", f"Phòng trọ #{rid}"),
                score=0.90,
                district=dist,
                price=r.get("price"),
                area=r.get("area"),
                reason=f"Phòng trọ nổi bật tại {dist} được nhiều người xem và liên hệ nhất."
            ))

        return RecommendationResponse(
            user_id=user_id,
            recommended_room_ids=recommended_ids,
            strategy="cold_start_popular",
            items=items,
            recommendations=items
        )

    def get_personalized_recommendations(self, req: PersonalizedRecommendRequest) -> RecommendationResponse:
        """
        Gợi ý theo ngữ cảnh thời gian thực khi Backend gửi trực tiếp danh sách phòng yêu thích/đã xem
        """
        # Nếu Backend gửi danh sách phòng yêu thích hoặc đã xem
        vectors = []
        weights = []

        # Trọng số phòng yêu thích (3.0), phòng đã xem (1.0)
        for rid in (req.favorite_room_ids or []):
            if rid in self.room_vectors_map:
                vectors.append(self.room_vectors_map[rid] * 3.0)
                weights.append(3.0)

        for rid in (req.viewed_room_ids or []):
            if rid in self.room_vectors_map:
                vectors.append(self.room_vectors_map[rid] * 1.0)
                weights.append(1.0)

        # Nếu không có tương tác nào được gửi kèm, fallback về lịch sử đã lưu
        if not vectors:
            return self.get_recommendations(req.user_id, limit=req.limit, district=req.preferred_district)

        # Tính vector sở thích tức thời
        user_vec = np.sum(vectors, axis=0) / sum(weights)

        # Lọc danh sách ứng viên (loại trừ các phòng người dùng đã lưu/xem nếu muốn)
        candidate_ids = [
            rid for rid in self._build_candidate_pool(
                district=req.preferred_district, max_price=req.preferred_max_price
            )
            if rid not in (req.favorite_room_ids or [])
        ]
        if not candidate_ids:
            candidate_ids = list(self.room_vectors_map.keys())

        candidate_matrix = np.array([self.room_vectors_map[rid] for rid in candidate_ids])
        sims = cosine_similarity([user_vec], candidate_matrix)[0]
        sorted_indices = np.argsort(-sims)[:req.limit]

        items = []
        recommended_ids = []
        for idx in sorted_indices:
            rid = candidate_ids[idx]
            score = float(sims[idx])
            meta = self.room_metadata_map.get(rid, {})

            recommended_ids.append(rid)
            items.append(RecommendationItem(
                room_id=rid,
                title=meta.get("title", f"Phòng trọ #{rid}"),
                score=round(max(0.0, min(1.0, score)), 4),
                district=meta.get("district"),
                price=meta.get("price"),
                area=meta.get("area"),
                reason=self._generate_reason(meta, score)
            ))

        return RecommendationResponse(
            user_id=req.user_id,
            recommended_room_ids=recommended_ids,
            strategy="content_based",
            items=items,
            recommendations=items
        )

    def record_interaction(self, interaction: UserInteractionSync):
        """Cập nhật tức thời hồ sơ sở thích của người dùng khi có hành vi xem/thích phòng"""
        room_id = interaction.room_id
        user_id = interaction.user_id
        if room_id not in self.room_vectors_map:
            return

        room_vec = self.room_vectors_map[room_id]
        weight_map = {"FAVORITE": 3.0, "CONTACT": 2.0, "VIEW": 1.0, "SEARCH": 0.5}
        w = weight_map.get(interaction.interaction_type.upper(), 1.0)

        # Cập nhật Exponential Moving Average (EMA) để vector thích ứng dần
        current_prof = self.user_profiles.get(user_id)
        if current_prof is None:
            self.user_profiles[user_id] = room_vec.copy()
        else:
            alpha = min(0.35, 0.10 * w)
            self.user_profiles[user_id] = (1 - alpha) * current_prof + alpha * room_vec

    def sync_room(self, room: RoomFeatureItem):
        """Đồng bộ một phòng mới từ Backend vào không gian vector gợi ý"""
        meta = {
            "room_id": room.room_id,
            "title": room.title,
            "district": room.district,
            "price": room.price,
            "area": room.area,
            "amenities": room.amenities
        }
        self.room_metadata_map[room.room_id] = meta

        if self.encoder:
            vec = self.encoder.transform_single(meta)
            self.room_vectors_map[room.room_id] = vec

    def sync_all_from_database(self) -> int:
        """Đồng bộ toàn bộ danh mục phòng trọ từ PostgreSQL qua Backend API và nạp lại mô hình"""
        from app.ml.sync_pg_rooms import sync_and_rebuild_models
        count = sync_and_rebuild_models()
        self.load_models()
        return count


recommendation_service = RecommendationService()

