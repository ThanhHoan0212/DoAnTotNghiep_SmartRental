import numpy as np
from sklearn.preprocessing import MinMaxScaler


class RoomFeatureEncoder:
    """Bộ trích xuất và chuẩn hóa vector đặc trưng phòng trọ"""
    def __init__(self, districts: list, amenities: list):
        self.districts = sorted(districts)
        self.amenities = sorted(amenities)
        self.district_to_idx = {d: i for i, d in enumerate(self.districts)}
        self.amenity_to_idx = {a: i for i, a in enumerate(self.amenities)}
        self.price_scaler = MinMaxScaler()
        self.area_scaler = MinMaxScaler()

    def fit(self, rooms: list):
        prices = np.array([[r["price"]] for r in rooms])
        areas = np.array([[r["area"]] for r in rooms])
        self.price_scaler.fit(prices)
        self.area_scaler.fit(areas)
        return self

    def transform_single(self, room: dict) -> np.ndarray:
        # 1. Đặc trưng số: Giá & Diện tích chuẩn hóa (2 chiều)
        p_norm = self.price_scaler.transform([[room["price"]]])[0][0]
        a_norm = self.area_scaler.transform([[room["area"]]])[0][0]

        # 2. Đặc trưng Quận (One-Hot: N_districts chiều)
        dist_vec = np.zeros(len(self.districts))
        if room["district"] in self.district_to_idx:
            dist_vec[self.district_to_idx[room["district"]]] = 1.0

        # 3. Đặc trưng Tiện ích (Multi-Hot: N_amenities chiều)
        amenity_vec = np.zeros(len(self.amenities))
        for am in room.get("amenities", []):
            if am in self.amenity_to_idx:
                amenity_vec[self.amenity_to_idx[am]] = 1.0

        # Ghép nối các đặc trưng:
        # Cân bằng trọng số: Quận (1.5), Tiện ích (1.0), Giá (1.2), Diện tích (0.8)
        feature_vector = np.concatenate([
            [p_norm * 1.2, a_norm * 0.8],
            dist_vec * 1.5,
            amenity_vec * 1.0
        ])
        return feature_vector

    def transform_all(self, rooms: list) -> np.ndarray:
        return np.array([self.transform_single(r) for r in rooms])
