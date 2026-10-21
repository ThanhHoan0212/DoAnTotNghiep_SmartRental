import numpy as np


class PriceFeatureEncoder:
    """Mã hóa các đặc trưng bảng cho mô hình hồi quy dự đoán giá"""
    def __init__(self, districts: list, room_types: list):
        self.districts = sorted(districts)
        self.room_types = sorted(room_types)
        self.dist_to_idx = {d: i for i, d in enumerate(self.districts)}
        self.type_to_idx = {t: i for i, t in enumerate(self.room_types)}

    def transform_single(
        self,
        district: str,
        room_type: str,
        month: int,
        is_student_season: int,
        is_tet_season: int,
        lag_1_price: float,
        lag_change_rate: float
    ) -> np.ndarray:
        dist_vec = np.zeros(len(self.districts))
        if district in self.dist_to_idx:
            dist_vec[self.dist_to_idx[district]] = 1.0

        type_vec = np.zeros(len(self.room_types))
        if room_type in self.type_to_idx:
            type_vec[self.type_to_idx[room_type]] = 1.0

        month_norm = month / 12.0
        price_scaled = lag_1_price / 10_000_000.0  # Chuẩn hóa về tầm 0.2 - 1.5

        features = np.concatenate([
            dist_vec,
            type_vec,
            [month_norm, float(is_student_season), float(is_tet_season), price_scaled, lag_change_rate]
        ])
        return features
