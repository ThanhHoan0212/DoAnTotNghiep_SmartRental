import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional
import joblib
import numpy as np

from app.core.config import settings
from app.models.price_model import PriceFeatureEncoder
from app.schemas.price import (
    DistrictPriceOverview,
    MarketTrendSummaryResponse,
    PricePredictionResponse,
    TrendDirection,
)
from app.services.nlp_service import DISTRICT_ALIASES, remove_accents

logger = logging.getLogger("ai_service.price")


class PricePredictionService:
    def __init__(self):
        self.models_dir = settings.MODELS_DIR
        self.regressor = None
        self.rmse: float = 85000.0
        self.encoder: Optional[PriceFeatureEncoder] = None
        self.district_stats: Dict[str, Dict[str, Any]] = {}
        self.load_models()

    def load_models(self):
        """Tải mô hình dự đoán giá và thống kê thị trường vào bộ nhớ"""
        try:
            model_path = self.models_dir / "price_prediction_model.joblib"
            stats_path = self.models_dir / "price_district_market_stats.joblib"
            encoder_path = self.models_dir / "price_feature_encoder.joblib"

            if model_path.exists():
                artifact = joblib.load(model_path)
                self.regressor = artifact["regressor"]
                self.rmse = artifact.get("rmse", 85000.0)

            if stats_path.exists():
                self.district_stats = joblib.load(stats_path)

            if encoder_path.exists():
                self.encoder = joblib.load(encoder_path)

            logger.info("Price prediction models loaded successfully. RMSE: %.0f VND", self.rmse)
        except Exception as e:
            logger.error("Error loading price prediction models: %s", str(e), exc_info=True)

    def _normalize_district(self, district: str) -> str:
        """Chuẩn hóa tên quận (hỗ trợ cả alias như q7, binh thanh, thu duc)"""
        raw = district.strip()
        norm = remove_accents(raw.lower())
        if norm in DISTRICT_ALIASES:
            return DISTRICT_ALIASES[norm]
        for alias, std_name in DISTRICT_ALIASES.items():
            if alias == norm:
                return std_name
        return raw

    def predict_price_trend(
        self,
        district: str,
        room_type: Optional[str] = None
    ) -> PricePredictionResponse:
        """
        Dự đoán giá thuê trung bình tháng tiếp theo và xác định xu hướng thị trường
        """
        std_district = self._normalize_district(district)
        std_room_type = room_type.strip().lower() if room_type else "phòng trọ"
        if std_room_type not in ["phòng trọ", "chung cư mini", "studio"]:
            std_room_type = "phòng trọ"

        # Lấy thống kê giá hiện tại của quận
        dist_stat = self.district_stats.get(std_district)
        if not dist_stat:
            # Fallback nếu quận chưa có trong từ điển
            current_avg_price = 4000000.0
        else:
            base_p = float(dist_stat["avg_price"])
            type_factors = {"phòng trọ": 0.85, "chung cư mini": 1.15, "studio": 1.35}
            current_avg_price = round(base_p * type_factors.get(std_room_type, 1.0) / 10000) * 10000

        # Xác định tháng hiện tại và tháng tiếp theo
        now = datetime.now()
        cur_month = now.month
        next_month = (cur_month % 12) + 1

        is_student_season = 1 if next_month in [8, 9, 10] else 0
        is_tet_season = 1 if next_month in [1, 2] else 0

        # Dự đoán giá bằng mô hình Gradient Boosting Regressor nếu sẵn sàng
        if self.regressor is not None and self.encoder is not None:
            features = self.encoder.transform_single(
                district=std_district,
                room_type=std_room_type,
                month=next_month,
                is_student_season=is_student_season,
                is_tet_season=is_tet_season,
                lag_1_price=current_avg_price,
                lag_change_rate=0.01
            )
            raw_pred = float(self.regressor.predict([features])[0])
            predicted_next_month = round(raw_pred / 50000) * 50000
        else:
            # Heuristic fallback dựa trên hệ số mùa vụ
            growth = 0.04 if is_student_season else (-0.03 if is_tet_season else 0.005)
            predicted_next_month = round((current_avg_price * (1 + growth)) / 50000) * 50000

        # Tính tỷ lệ biến động (%)
        change_pct = round(((predicted_next_month - current_avg_price) / current_avg_price) * 100, 2)

        # Xác định xu hướng
        if change_pct >= 1.5:
            trend = TrendDirection.INCREASING
        elif change_pct <= -1.5:
            trend = TrendDirection.DECREASING
        else:
            trend = TrendDirection.STABLE

        # Tính 95% Khoảng tin cậy (Confidence Interval: y ± 1.96 * RMSE)
        margin = 1.96 * self.rmse
        ci_lower = round(max(0.0, predicted_next_month - margin) / 50000) * 50000
        ci_upper = round((predicted_next_month + margin) / 50000) * 50000

        # Sinh các yếu tố tác động
        factors = []
        if is_student_season:
            factors.append("Mùa cao điểm tân sinh viên và người mới đi làm tìm phòng thúc đẩy giá tăng.")
        elif is_tet_season:
            factors.append("Dịp cận Tết Nguyên Đán nhiều sinh viên trả phòng về quê, nhu cầu hạ nhiệt nhẹ.")
        else:
            factors.append("Nhu cầu thuê duy trì mức ổn định trên địa bàn.")

        if std_district in ["Quận 1", "Quận 3", "Bình Thạnh"]:
            factors.append(f"Khu vực {std_district} có quỹ phòng trung tâm khan hiếm, giữ giá trị cao.")
        else:
            factors.append(f"Phân khúc {std_room_type} tại {std_district} có nguồn cung đa dạng và thanh khoản tốt.")

        return PricePredictionResponse(
            district=std_district,
            room_type=std_room_type,
            current_avg_price=float(current_avg_price),
            predicted_next_month=float(predicted_next_month),
            trend=trend,
            confidence_interval=[float(ci_lower), float(ci_upper)],
            change_percentage=change_pct,
            factors=factors
        )

    def get_market_overview(self) -> MarketTrendSummaryResponse:
        """Tổng quan xu hướng thị trường toàn TP.HCM theo từng quận"""
        overviews: List[DistrictPriceOverview] = []

        for district, stat in self.district_stats.items():
            pred = self.predict_price_trend(district=district, room_type="phòng trọ")
            overviews.append(DistrictPriceOverview(
                district=district,
                avg_price=float(stat["avg_price"]),
                min_price=float(stat["min_price"]),
                max_price=float(stat["max_price"]),
                avg_price_per_m2=float(stat["avg_price_per_m2"]),
                trend=pred.trend,
                sample_count=int(stat.get("sample_count", 100))
            ))

        # Xu hướng toàn thị trường
        inc_count = sum(1 for o in overviews if o.trend == TrendDirection.INCREASING)
        dec_count = sum(1 for o in overviews if o.trend == TrendDirection.DECREASING)
        if inc_count > len(overviews) // 2:
            overall_trend = TrendDirection.INCREASING
        elif dec_count > len(overviews) // 2:
            overall_trend = TrendDirection.DECREASING
        else:
            overall_trend = TrendDirection.STABLE

        return MarketTrendSummaryResponse(
            updated_at=datetime.now(timezone.utc).isoformat(),
            overall_market_trend=overall_trend,
            districts=overviews
        )


price_service = PricePredictionService()
