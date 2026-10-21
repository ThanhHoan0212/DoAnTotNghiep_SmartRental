import logging
import re
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
import joblib
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from app.core.config import settings
from app.schemas.fraud import FraudCheckRequest, FraudCheckResponse, ListingSyncItem, RiskLevel

import unicodedata

logger = logging.getLogger("ai_service.fraud")

SUSPICIOUS_KEYWORD_RULES = [
    (["cọc", "zalo"], "yêu cầu cọc trước qua Zalo"),
    (["chuyển khoản", "cọc"], "yêu cầu chuyển khoản tiền cọc"),
    (["cọc trước"], "yêu cầu cọc trước khi xem phòng"),
    (["cọc online"], "chỉ nhận đặt cọc online không gặp mặt"),
    (["cọc mới cho xem phòng"], "bắt buộc cọc trước mới cho xem phòng thực tế"),
    (["kẻo mất phòng"], "hối thúc tâm lý đặt cọc nhanh kẻo mất"),
    (["kẻo mất", "cọc"], "hối thúc tâm lý đặt cọc nhanh kẻo mất"),
    (["chủ nhà ở xa", "cọc"], "lấy lý do chủ nhà ở xa yêu cầu chuyển cọc giữ chỗ"),
    (["giữ phòng gấp", "cọc"], "hối thúc cọc giữ phòng gấp")
]


class FraudDetectionService:
    def __init__(self):
        self.models_dir = settings.MODELS_DIR
        self.iso_forest = None
        self.district_stats: Dict[str, Any] = {}
        self.tfidf_vectorizer = None
        self.corpus_listings: List[Dict[str, Any]] = []
        self.corpus_tfidf_matrix = None
        self.load_models()

    def load_models(self):
        """Tải các mô hình và artifact đã huấn luyện vào bộ nhớ"""
        try:
            stats_path = self.models_dir / "district_price_stats.joblib"
            if stats_path.exists():
                self.district_stats = joblib.load(stats_path)
                logger.info("Loaded district price stats for %d districts.", len(self.district_stats))

            iso_path = self.models_dir / "fraud_isolation_forest.joblib"
            if iso_path.exists():
                self.iso_forest = joblib.load(iso_path)
                logger.info("Loaded Isolation Forest fraud model.")

            tfidf_path = self.models_dir / "fraud_tfidf_vectorizer.joblib"
            corpus_path = self.models_dir / "existing_listings_corpus.joblib"
            if tfidf_path.exists() and corpus_path.exists():
                self.tfidf_vectorizer = joblib.load(tfidf_path)
                self.corpus_listings = joblib.load(corpus_path)
                # Tính trước vector TF-IDF cho toàn bộ tin đăng trong corpus
                texts = [f"{item['title']} {item['description']}" for item in self.corpus_listings]
                if texts:
                    self.corpus_tfidf_matrix = self.tfidf_vectorizer.transform(texts)
                logger.info("Loaded TF-IDF and %d corpus reference listings.", len(self.corpus_listings))

        except Exception as e:
            logger.error("Error loading fraud models: %s. Using heuristic fallbacks.", str(e))

    def _check_price_anomaly(self, price: float, area: float, district: str) -> Tuple[float, List[str], Dict[str, Any]]:
        """Phát hiện giá bất thường dựa trên Thống kê Quận (IQR, Z-score) và Isolation Forest"""
        price_per_m2 = price / area if area > 0 else 0
        reasons = []
        details: Dict[str, Any] = {
            "price_per_m2": round(price_per_m2, 2),
            "district": district
        }

        dist_stat = self.district_stats.get(district)
        if not dist_stat:
            # Fallback nếu quận chưa có trong từ điển thống kê
            median_m2 = 200000.0
            mean_m2 = 200000.0
            std_m2 = 60000.0
            expected_avg_price = median_m2 * area
        else:
            median_m2 = dist_stat["median_price_per_m2"]
            mean_m2 = dist_stat["mean_price_per_m2"]
            std_m2 = dist_stat["std_price_per_m2"]
            expected_avg_price = median_m2 * area
            details.update({
                "district_median_m2": median_m2,
                "district_expected_avg_price": round(expected_avg_price),
                "lower_bound_m2": dist_stat.get("lower_bound_price_per_m2"),
                "upper_bound_m2": dist_stat.get("upper_bound_price_per_m2")
            })

        ratio_to_median = price_per_m2 / median_m2 if median_m2 > 0 else 1.0
        z_score = (price_per_m2 - mean_m2) / std_m2 if std_m2 > 0 else 0.0

        details["price_ratio_to_median"] = round(ratio_to_median, 3)
        details["z_score"] = round(z_score, 2)

        price_score = 0.0

        # Phân tích dấu hiệu giá rẻ bất thường (mồi nhử lừa cọc)
        if ratio_to_median <= 0.45:
            price_score = 0.85
            pct_diff = round((1.0 - ratio_to_median) * 100)
            reasons.append(
                f"Giá thuê thấp bất thường so với khu vực {district} "
                f"({int(price):,}đ so với mức trung bình {int(expected_avg_price):,}đ, thấp hơn {pct_diff}%). "
                f"Cảnh báo bẫy giá ảo để lừa đặt cọc."
            )
        elif ratio_to_median <= 0.65:
            price_score = 0.45
            pct_diff = round((1.0 - ratio_to_median) * 100)
            reasons.append(
                f"Giá thuê thấp hơn đáng kể ({pct_diff}%) so với mặt bằng chung {district} "
                f"(trung bình khoảng {int(expected_avg_price):,}đ)."
            )
        # Phân tích giá cao bất thường
        elif ratio_to_median >= 2.5:
            price_score = 0.70
            reasons.append(
                f"Giá thuê cao bất thường ({int(price):,}đ/tháng) so với diện tích {area}m² tại {district} "
                f"(gấp {ratio_to_median:.1f} lần mặt bằng chung)."
            )

        # Đánh giá bổ sung bằng Isolation Forest nếu model sẵn sàng
        if self.iso_forest is not None:
            feature_vector = np.array([[
                np.log1p(price),
                area,
                price_per_m2,
                ratio_to_median,
                z_score
            ]])
            iso_decision = float(self.iso_forest.decision_function(feature_vector)[0])
            details["isolation_forest_decision"] = round(iso_decision, 4)

            # Anomaly score: decision < 0 biểu thị outlier
            if iso_decision < 0:
                iso_anomaly_weight = min(1.0, max(0.0, -iso_decision * 4.0))
                price_score = max(price_score, iso_anomaly_weight)
                if not reasons:
                    reasons.append(
                        f"Mô hình Isolation Forest phát hiện tương quan giá ({int(price):,}đ) "
                        f"và diện tích ({area}m²) sai lệch lớn so với phân phối chung tại {district}."
                    )

        details["price_anomaly_score"] = round(price_score, 3)
        return price_score, reasons, details

    def _check_duplicate_content(self, title: str, description: str) -> Tuple[float, List[str], Dict[str, Any]]:
        """Phát hiện trùng lặp nội dung mô tả so với cơ sở dữ liệu tin đăng đã có qua TF-IDF Cosine Similarity"""
        reasons = []
        details: Dict[str, Any] = {"max_similarity": 0.0, "matched_listing_id": None}

        if not self.tfidf_vectorizer or self.corpus_tfidf_matrix is None or not self.corpus_listings:
            return 0.0, reasons, details

        try:
            query_text = f"{title} {description}"
            query_vec = self.tfidf_vectorizer.transform([query_text])

            sims = cosine_similarity(query_vec, self.corpus_tfidf_matrix)[0]
            max_idx = int(np.argmax(sims))
            max_sim = float(sims[max_idx])

            details["max_similarity"] = round(max_sim, 4)
            details["matched_listing_id"] = self.corpus_listings[max_idx].get("room_id")
            details["matched_title"] = self.corpus_listings[max_idx].get("title")

            duplicate_score = 0.0
            if max_sim >= 0.88:
                duplicate_score = 0.90
                reasons.append(
                    f"Nội dung mô tả trùng lặp rất cao ({max_sim * 100:.1f}%) với tin đăng #{details['matched_listing_id']} "
                    f"('{details['matched_title']}'). Nghi vấn copy tin giả mạo."
                )
            elif max_sim >= 0.75:
                duplicate_score = 0.55
                reasons.append(
                    f"Nội dung mô tả tương đồng {max_sim * 100:.1f}% với tin đăng #{details['matched_listing_id']}."
                )

            details["duplicate_score"] = duplicate_score
            return duplicate_score, reasons, details

        except Exception as e:
            logger.error("Error in duplicate content detection: %s", str(e))
            return 0.0, reasons, details

    def _check_suspicious_phrases(self, text: str) -> Tuple[float, List[str]]:
        """Quét từ khóa mồi nhử đặt cọc lừa đảo trong tiêu đề và nội dung"""
        reasons = []
        detected_phrases = []

        norm_text = unicodedata.normalize("NFC", text).lower()
        for words, label in SUSPICIOUS_KEYWORD_RULES:
            if all(unicodedata.normalize("NFC", w).lower() in norm_text for w in words):
                detected_phrases.append(label)

        if detected_phrases:
            phrase_str = "; ".join(dict.fromkeys(detected_phrases))
            reasons.append(f"Nội dung chứa cụm từ đáng ngờ về yêu cầu cọc online: {phrase_str}.")
            return 0.85, reasons

        return 0.0, reasons

    def check_listing(self, req: FraudCheckRequest) -> FraudCheckResponse:
        """Kiểm tra toàn diện tin đăng phòng trọ và sinh điểm rủi ro tổng hợp"""
        all_reasons: List[str] = []

        # 1. Kiểm tra bất thường về giá
        price_score, price_reasons, price_details = self._check_price_anomaly(
            price=req.price, area=req.area, district=req.district
        )
        all_reasons.extend(price_reasons)

        # 2. Kiểm tra trùng lặp nội dung
        dup_score, dup_reasons, dup_details = self._check_duplicate_content(
            title=req.title, description=req.description
        )
        all_reasons.extend(dup_reasons)

        # 3. Kiểm tra từ khóa lừa cọc
        full_text = f"{req.title} {req.description}"
        keyword_score, keyword_reasons = self._check_suspicious_phrases(full_text)
        all_reasons.extend(keyword_reasons)

        # Trọng số tính điểm rủi ro tổng hợp (Ensemble weighted score)
        # Giá bất thường: 45%, Trùng lặp: 35%, Từ khóa cọc: 20%
        # Nếu có dấu hiệu nghiêm trọng đơn lẻ (lừa cọc, copy trắng trợn, hoặc giá quá rẻ), nâng điểm rủi ro
        max_subscore = max(price_score, dup_score, keyword_score)
        weighted_score = (price_score * 0.45) + (dup_score * 0.35) + (keyword_score * 0.20)
        if max_subscore >= 0.75:
            final_fraud_score = max(weighted_score, 0.72)
        else:
            final_fraud_score = weighted_score
        final_fraud_score = round(min(1.0, max(0.0, final_fraud_score)), 2)

        # Phân loại mức rủi ro
        if final_fraud_score >= 0.65:
            risk_level = RiskLevel.HIGH
            is_fraud = True
        elif final_fraud_score >= 0.35:
            risk_level = RiskLevel.MEDIUM
            is_fraud = True
        else:
            risk_level = RiskLevel.LOW
            is_fraud = False

        if not all_reasons and not is_fraud:
            all_reasons.append("Giá thuê và thông tin mô tả phù hợp với mặt bằng thị trường khu vực.")

        return FraudCheckResponse(
            room_id=req.room_id,
            fraud_score=final_fraud_score,
            risk_level=risk_level,
            is_fraud=is_fraud,
            reasons=all_reasons,
            details={
                "price_analysis": price_details,
                "duplicate_analysis": dup_details,
                "keyword_score": keyword_score,
                "composite_score": final_fraud_score
            }
        )

    def sync_listing(self, item: ListingSyncItem) -> int:
        """Đồng bộ một tin đăng mới vào tập đối chiếu trùng lặp"""
        self.corpus_listings.append({
            "room_id": item.room_id,
            "title": item.title,
            "description": item.description,
            "price": item.price,
            "district": item.district,
            "area": item.area
        })

        # Cập nhật lại TF-IDF matrix trên toàn bộ corpus
        texts = [f"{x['title']} {x['description']}" for x in self.corpus_listings]
        if self.tfidf_vectorizer is not None:
            try:
                self.corpus_tfidf_matrix = self.tfidf_vectorizer.fit_transform(texts)
            except Exception:
                self.corpus_tfidf_matrix = self.tfidf_vectorizer.transform(texts)

        return len(self.corpus_listings)


# Singleton instance để tái sử dụng trong toàn bộ ứng dụng
fraud_service = FraudDetectionService()
