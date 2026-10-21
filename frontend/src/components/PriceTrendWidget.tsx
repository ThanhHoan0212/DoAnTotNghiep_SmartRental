import { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, Minus, Info, Sparkles } from 'lucide-react';
import { aiService } from '../services/aiService';
import type { PricePredictionResponse } from '../types/ai';

interface PriceTrendWidgetProps {
  district?: string;
  roomType?: string;
}

export default function PriceTrendWidget({ district = 'Quận 7', roomType = 'phòng trọ' }: PriceTrendWidgetProps) {
  const [data, setData] = useState<PricePredictionResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    setError(null);

    aiService.predictPrice(district, roomType)
      .then((res) => {
        if (isMounted) {
          setData(res);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.warn('Không thể tải dự báo giá AI:', err);
          setError('Tạm thời chưa có dữ liệu dự báo cho khu vực này');
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [district, roomType]);

  if (isLoading) {
    return (
      <div style={{ padding: '16px', borderRadius: '12px', background: '#f8fafc', border: '1px solid #e2e8f0' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#64748b', fontSize: '13px' }}>
          <Sparkles size={16} color="#3b82f6" />
          <span>AI đang phân tích dữ liệu thị trường và tính toán xu hướng...</span>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return null;
  }

  const renderTrendBadge = () => {
    switch (data.trend) {
      case 'increasing':
        return (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: '#fef2f2', color: '#dc2626', padding: '4px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 600 }}>
            <TrendingUp size={14} /> Tăng nhẹ ({data.change_percentage > 0 ? `+${data.change_percentage}%` : `${data.change_percentage}%`})
          </span>
        );
      case 'decreasing':
        return (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: '#f0fdf4', color: '#16a34a', padding: '4px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 600 }}>
            <TrendingDown size={14} /> Giảm nhẹ ({data.change_percentage}%)
          </span>
        );
      default:
        return (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: '#f1f5f9', color: '#475569', padding: '4px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 600 }}>
            <Minus size={14} /> Ổn định ({data.change_percentage}%)
          </span>
        );
    }
  };

  return (
    <div
      style={{
        background: 'linear-gradient(145deg, #ffffff 0%, #f8fafc 100%)',
        border: '1px solid #e2e8f0',
        borderRadius: '16px',
        padding: '20px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.03)',
        marginTop: '16px',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ background: '#eff6ff', padding: '6px', borderRadius: '8px' }}>
            <Sparkles size={18} color="#2563eb" />
          </div>
          <div>
            <h4 style={{ margin: 0, fontSize: '15px', fontWeight: 700, color: '#1e293b' }}>
              Dự báo giá thuê AI • {data.district}
            </h4>
            <div style={{ fontSize: '12px', color: '#64748b' }}>Phân khúc {data.room_type} tháng tới</div>
          </div>
        </div>
        {renderTrendBadge()}
      </div>

      {/* Grid thống kê giá */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '16px' }}>
        <div style={{ background: '#ffffff', padding: '12px', borderRadius: '10px', border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: '12px', color: '#64748b' }}>Giá trung bình hiện tại</div>
          <div style={{ fontSize: '17px', fontWeight: 700, color: '#1e293b', marginTop: '2px' }}>
            {data.current_avg_price.toLocaleString('vi-VN')} đ/tháng
          </div>
        </div>

        <div style={{ background: '#ffffff', padding: '12px', borderRadius: '10px', border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: '12px', color: '#64748b' }}>Dự báo tháng kế tiếp</div>
          <div style={{ fontSize: '17px', fontWeight: 700, color: '#2563eb', marginTop: '2px' }}>
            {data.predicted_next_month.toLocaleString('vi-VN')} đ/tháng
          </div>
        </div>
      </div>

      {/* Khoảng tin cậy 95% */}
      {data.confidence_interval && data.confidence_interval.length === 2 && (
        <div style={{ marginBottom: '14px', background: '#f1f5f9', padding: '10px 14px', borderRadius: '10px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#475569', fontWeight: 500 }}>
            <span>Khoảng giá dự báo (độ tin cậy 95%):</span>
            <span style={{ fontWeight: 700, color: '#1e293b' }}>
              {data.confidence_interval[0].toLocaleString('vi-VN')} đ - {data.confidence_interval[1].toLocaleString('vi-VN')} đ
            </span>
          </div>
        </div>
      )}

      {/* Các yếu tố phân tích */}
      {data.factors && data.factors.length > 0 && (
        <div>
          <div style={{ fontSize: '12px', fontWeight: 600, color: '#475569', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <Info size={13} color="#64748b" /> Nhận định thị trường:
          </div>
          <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '12px', color: '#64748b', lineHeight: '1.6' }}>
            {data.factors.map((f, i) => (
              <li key={i}>{f}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
