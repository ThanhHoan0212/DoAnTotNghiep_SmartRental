"""Script huấn luyện và đánh giá mô hình Dự đoán Xu hướng Giá thuê (Price Prediction).
Sử dụng thuật toán Gradient Boosting Regressor trên chuỗi thời gian kết hợp các yếu tố mùa vụ (Sinh viên nhập học, Tết).
Đánh giá độ chính xác bằng các chỉ số học thuật: MAE, RMSE, MAPE, R² Score.
Lưu trữ mô hình và thống kê thị trường vào 'app/models/'.
"""

import sys
from pathlib import Path

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_absolute_percentage_error, mean_squared_error, r2_score
from app.models.price_model import PriceFeatureEncoder
from app.ml.generate_price_history import DISTRICT_BASE_PRICES, ROOM_TYPE_FACTORS, generate_price_history_dataset

BASE_DIR = Path(__file__).resolve().parent.parent.parent
DATA_PATH = BASE_DIR / "data" / "historical_rental_prices.csv"
MODELS_DIR = BASE_DIR / "app" / "models"


def prepare_time_series_features(df: pd.DataFrame, encoder: PriceFeatureEncoder):
    """Xây dựng ma trận đặc trưng với các biến trễ (Lag 1, Lag 2)"""
    X_list = []
    y_list = []
    date_list = []

    # Nhóm theo từng cặp (district, room_type) để tạo lag đúng thứ tự thời gian
    for (district, room_type), group in df.groupby(["district", "room_type"]):
        sorted_g = group.sort_values(by="date").reset_index(drop=True)

        for i in range(2, len(sorted_g)):
            lag_1 = sorted_g.loc[i - 1, "avg_price"]
            lag_2 = sorted_g.loc[i - 2, "avg_price"]
            lag_change = (lag_1 - lag_2) / lag_2 if lag_2 > 0 else 0.0

            cur_row = sorted_g.loc[i]
            target_p = cur_row["avg_price"]

            feat = encoder.transform_single(
                district=district,
                room_type=room_type,
                month=int(cur_row["month"]),
                is_student_season=int(cur_row["is_student_season"]),
                is_tet_season=int(cur_row["is_tet_season"]),
                lag_1_price=lag_1,
                lag_change_rate=lag_change
            )

            X_list.append(feat)
            y_list.append(target_p)
            date_list.append(cur_row["date"])

    return np.array(X_list), np.array(y_list), date_list


def compute_latest_district_stats(df: pd.DataFrame) -> dict:
    """Tính toán thống kê thị trường mới nhất của từng quận tại tháng gần nhất"""
    latest_date = df["date"].max()
    latest_df = df[df["date"] == latest_date]
    stats = {}

    for district, group in latest_df.groupby("district"):
        mean_p = float(group["avg_price"].mean())
        stats[district] = {
            "district": district,
            "avg_price": round(mean_p),
            "min_price": float(group["avg_price"].min()),
            "max_price": float(group["avg_price"].max()),
            "avg_price_per_m2": round(float(group["avg_price_per_m2"].mean())),
            "sample_count": len(group) * 15,
            "latest_month_date": latest_date
        }
    return stats


def train_and_evaluate():
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    if not DATA_PATH.exists():
        print("[INFO] Price history dataset not found. Generating synthetic data...")
        df = generate_price_history_dataset()
    else:
        df = pd.read_csv(DATA_PATH)
        print(f"[INFO] Loaded {len(df)} historical price records.")

    districts = list(DISTRICT_BASE_PRICES.keys())
    room_types = list(ROOM_TYPE_FACTORS.keys())

    # 1. Khởi tạo và lưu Feature Encoder
    encoder = PriceFeatureEncoder(districts=districts, room_types=room_types)
    joblib.dump(encoder, MODELS_DIR / "price_feature_encoder.joblib")

    # 2. Tạo ma trận đặc trưng
    X, y, dates = prepare_time_series_features(df, encoder)

    # 3. Phân chia Train/Test theo chuỗi thời gian (80% Train quá khứ, 20% Test các tháng gần nhất)
    split_idx = int(len(X) * 0.80)
    X_train, X_test = X[:split_idx], X[split_idx:]
    y_train, y_test = y[:split_idx], y[split_idx:]

    print(f"[INFO] Training Gradient Boosting Regressor (n_estimators=120, max_depth=4)...")
    regressor = GradientBoostingRegressor(
        n_estimators=120,
        learning_rate=0.08,
        max_depth=4,
        random_state=42
    )
    regressor.fit(X_train, y_train)

    # 4. Đánh giá chất lượng mô hình trên tập Test
    y_pred = regressor.predict(X_test)
    mae = mean_absolute_error(y_test, y_pred)
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    mape = mean_absolute_percentage_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)

    # 5. Lưu mô hình và chỉ số sai số chuẩn (RMSE dùng để tính 95% Confidence Interval)
    model_artifact = {
        "regressor": regressor,
        "rmse": float(rmse),
        "mae": float(mae),
        "mape": float(mape),
        "r2": float(r2)
    }
    joblib.dump(model_artifact, MODELS_DIR / "price_prediction_model.joblib")
    print(f"[OK] Saved Price Prediction model to '{MODELS_DIR / 'price_prediction_model.joblib'}'.")

    # 6. Lưu thống kê thị trường theo quận
    district_stats = compute_latest_district_stats(df)
    joblib.dump(district_stats, MODELS_DIR / "price_district_market_stats.joblib")
    print(f"[OK] Saved district market statistics to '{MODELS_DIR / 'price_district_market_stats.joblib'}'.")

    # 7. In bảng kết quả thực nghiệm chi tiết cho báo cáo tốt nghiệp
    print("\n" + "=" * 65)
    print("     BÁO CÁO KẾT QUẢ THỰC NGHIỆM - PRICE PREDICTION")
    print("=" * 65)
    print(f"Tổng số mẫu huấn luyện (Train): {len(X_train)}")
    print(f"Tổng số mẫu kiểm thử (Test):     {len(X_test)}")
    print(f"Mô hình thuật toán:              Gradient Boosting Regressor (GBR)")
    print("-" * 65)
    print(f"MAE (Sai số tuyệt đối trung bình):     {mae:,.0f} VND")
    print(f"RMSE (Căn bậc hai sai số bình phương):  {rmse:,.0f} VND")
    print(f"MAPE (Tỷ lệ sai số phần trăm):         {mape * 100:.2f}%")
    print(f"R² Score (Hệ số xác định):             {r2:.4f}")
    print("=" * 65)

    return model_artifact


if __name__ == "__main__":
    train_and_evaluate()
