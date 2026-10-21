import sys
from pathlib import Path

# Ensure UTF-8 output on Windows console
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split

from app.ml.generate_fraud_data import generate_synthetic_dataset

BASE_DIR = Path(__file__).resolve().parent.parent.parent
DATA_PATH = BASE_DIR / "data" / "synthetic_listings.csv"
MODELS_DIR = BASE_DIR / "app" / "models"


def compute_district_price_stats(df: pd.DataFrame) -> dict:
    """Tính toán thống kê giá theo từng quận (Mean, Std, Median, IQR, Min, Max)
    Chỉ tính trên các tin đăng bình thường (is_fraud == 0) để làm chuẩn mực thị trường.
    """
    clean_df = df[df["is_fraud"] == 0]
    stats = {}
    for district, group in clean_df.groupby("district"):
        prices_per_m2 = group["price_per_m2"].values
        q1 = float(np.percentile(prices_per_m2, 25))
        q3 = float(np.percentile(prices_per_m2, 75))
        iqr = q3 - q1
        stats[district] = {
            "mean_price_per_m2": float(np.mean(prices_per_m2)),
            "std_price_per_m2": float(np.std(prices_per_m2)),
            "median_price_per_m2": float(np.median(prices_per_m2)),
            "q1_price_per_m2": q1,
            "q3_price_per_m2": q3,
            "iqr_price_per_m2": iqr,
            "lower_bound_price_per_m2": max(0.0, q1 - 1.5 * iqr),
            "upper_bound_price_per_m2": q3 + 1.5 * iqr,
            "mean_price": float(group["price"].mean()),
            "median_price": float(group["price"].median()),
            "mean_area": float(group["area"].mean()),
            "sample_count": len(group)
        }
    return stats


def extract_features(df: pd.DataFrame, district_stats: dict) -> np.ndarray:
    """Trích xuất vector đặc trưng số học cho Isolation Forest:
    1. log(price)
    2. area
    3. price_per_m2
    4. Tỷ lệ so với trung vị quận (price_per_m2 / median_district)
    5. Độ lệch chuẩn z-score theo quận
    """
    features = []
    default_median = 200000.0
    default_mean = 200000.0
    default_std = 50000.0

    for _, row in df.iterrows():
        dist = row["district"]
        dist_stat = district_stats.get(dist, {})
        median_m2 = dist_stat.get("median_price_per_m2", default_median)
        mean_m2 = dist_stat.get("mean_price_per_m2", default_mean)
        std_m2 = dist_stat.get("std_price_per_m2", default_std)
        if std_m2 <= 0:
            std_m2 = default_std

        price_m2 = row["price_per_m2"]
        ratio_median = price_m2 / median_m2
        z_score = (price_m2 - mean_m2) / std_m2

        features.append([
            np.log1p(row["price"]),
            row["area"],
            price_m2,
            ratio_median,
            z_score
        ])

    return np.array(features)


def train_and_evaluate():
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    if not DATA_PATH.exists():
        print(f"[INFO] Dataset not found at '{DATA_PATH}'. Generating synthetic data...")
        df = generate_synthetic_dataset(output_path=str(DATA_PATH))
    else:
        df = pd.read_csv(DATA_PATH)
        print(f"[INFO] Loaded {len(df)} listings from '{DATA_PATH}'.")

    # 1. Tính toán thống kê giá theo quận
    district_stats = compute_district_price_stats(df)
    stats_file = MODELS_DIR / "district_price_stats.joblib"
    joblib.dump(district_stats, stats_file)
    print(f"[OK] Saved district price statistics to '{stats_file}'.")

    # 2. Huấn luyện TF-IDF Vectorizer cho phát hiện trùng lặp nội dung
    text_corpus = (df["title"] + " " + df["description"]).tolist()
    tfidf = TfidfVectorizer(max_features=2500, ngram_range=(1, 2), min_df=2)
    tfidf_matrix = tfidf.fit_transform(text_corpus)

    tfidf_file = MODELS_DIR / "fraud_tfidf_vectorizer.joblib"
    joblib.dump(tfidf, tfidf_file)
    print(f"[OK] Saved TF-IDF Vectorizer to '{tfidf_file}'.")

    # Lưu corpus tin đăng mẫu làm cơ sở đối chiếu trùng lặp
    clean_sample_listings = df[df["is_fraud"] == 0].head(500)[["room_id", "title", "description", "district", "price", "area"]].to_dict(orient="records")
    corpus_file = MODELS_DIR / "existing_listings_corpus.joblib"
    joblib.dump(clean_sample_listings, corpus_file)
    print(f"[OK] Saved {len(clean_sample_listings)} reference listings to '{corpus_file}'.")

    # 3. Phân chia Train / Test (80% / 20%)
    train_df, test_df = train_test_split(df, test_size=0.20, random_state=42, stratify=df["is_fraud"])

    X_train = extract_features(train_df, district_stats)
    X_test = extract_features(test_df, district_stats)
    y_test = test_df["is_fraud"].values

    # 4. Huấn luyện Isolation Forest
    contamination_rate = float(train_df["is_fraud"].mean())
    print(f"[INFO] Training Isolation Forest (n_estimators=150, contamination={contamination_rate:.3f})...")

    iso_forest = IsolationForest(
        n_estimators=150,
        max_samples="auto",
        contamination=contamination_rate,
        random_state=42,
        n_jobs=-1
    )
    iso_forest.fit(X_train)

    iso_forest_file = MODELS_DIR / "fraud_isolation_forest.joblib"
    joblib.dump(iso_forest, iso_forest_file)
    print(f"[OK] Saved Isolation Forest model to '{iso_forest_file}'.")

    # 5. Đánh giá mô hình trên tập Test
    # Trong sklearn Isolation Forest: -1 là outlier (gian lận), 1 là inlier (bình thường)
    raw_preds = iso_forest.predict(X_test)
    y_pred_iso = np.where(raw_preds == -1, 1, 0)

    # Lấy anomaly score (càng âm thì càng bất thường)
    anomaly_scores = -iso_forest.decision_function(X_test)

    precision = precision_score(y_test, y_pred_iso, zero_division=0)
    recall = recall_score(y_test, y_pred_iso, zero_division=0)
    f1 = f1_score(y_test, y_pred_iso, zero_division=0)
    roc_auc = roc_auc_score(y_test, anomaly_scores)
    cm = confusion_matrix(y_test, y_pred_iso)

    # 6. In bảng kết quả thực nghiệm chi tiết cho báo cáo tốt nghiệp
    print("\n" + "=" * 65)
    print("       BÁO CÁO KẾT QUẢ THỰC NGHIỆM - FRAUD DETECTION")
    print("=" * 65)
    print(f"Tổng số mẫu tập Test:  {len(y_test)} (Normal: {sum(y_test == 0)}, Fraud: {sum(y_test == 1)})")
    print(f"Precision (Độ chính xác): {precision * 100:.2f}%")
    print(f"Recall (Độ phủ bắt gian):  {recall * 100:.2f}%")
    print(f"F1-Score:                 {f1 * 100:.2f}%")
    print(f"ROC-AUC Score:            {roc_auc:.4f}")
    print("-" * 65)
    print("CONFUSION MATRIX (Ma trận nhầm lẫn):")
    print(f"                Dự đoán Bình thường  |  Dự đoán Gian lận")
    print(f"Thực tế Hợp lệ:        {cm[0][0]:<10}    |       {cm[0][1]:<10}")
    print(f"Thực tế Gian lận:      {cm[1][0]:<10}    |       {cm[1][1]:<10}")
    print("-" * 65)
    print("Classification Report chi tiết:")
    print(classification_report(y_test, y_pred_iso, target_names=["Hợp lệ (0)", "Gian lận (1)"]))
    print("=" * 65)

    return {
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "roc_auc": roc_auc,
        "confusion_matrix": cm.tolist()
    }


if __name__ == "__main__":
    train_and_evaluate()
