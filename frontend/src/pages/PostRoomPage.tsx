import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { roomService } from '../services/roomService';
import type { Amenity, RoomImageRequest } from '../types/room';
import { ApiError } from '../services/api';

export default function PostRoomPage() {
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [area, setArea] = useState('');
  const [address, setAddress] = useState('');
  const [ward, setWard] = useState('');
  const [district, setDistrict] = useState('Cầu Giấy');
  const [city] = useState('Hà Nội');

  // Tiện ích
  const [availableAmenities, setAvailableAmenities] = useState<Amenity[]>([]);
  const [selectedAmenityIds, setSelectedAmenityIds] = useState<number[]>([]);

  // Hình ảnh
  const [imageUrlInput, setImageUrlInput] = useState('');
  const [images, setImages] = useState<RoomImageRequest[]>([
    { imageUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', isPrimary: true }
  ]);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const districts = [
    "Cầu Giấy",
    "Đống Đa",
    "Nam Từ Liêm",
    "Thanh Xuân",
    "Ba Đình",
    "Hai Bà Trưng",
    "Tây Hồ",
    "Hà Đông",
    "Hoàng Mai",
    "Bắc Từ Liêm",
    "Long Biên",
  ];

  useEffect(() => {
    roomService.getAllAmenities()
      .then(setAvailableAmenities)
      .catch((err) => console.warn("Lỗi tải tiện ích:", err));
  }, []);

  const toggleAmenity = (id: number) => {
    setSelectedAmenityIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const addImage = () => {
    if (!imageUrlInput.trim()) return;
    setImages((prev) => [
      ...prev,
      { imageUrl: imageUrlInput.trim(), isPrimary: prev.length === 0 }
    ]);
    setImageUrlInput('');
  };

  const removeImage = (index: number) => {
    setImages((prev) => prev.filter((_, idx) => idx !== index));
  };

  const setPrimaryImage = (index: number) => {
    setImages((prev) =>
      prev.map((img, idx) => ({
        ...img,
        isPrimary: idx === index
      }))
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!title.trim() || !price || !area || !address.trim() || !district) {
      setError('Vui lòng điền đầy đủ các trường thông tin bắt buộc (*)');
      return;
    }

    const priceNum = parseFloat(price);
    const areaNum = parseFloat(area);

    if (isNaN(priceNum) || priceNum <= 0) {
      setError('Giá thuê phải là số dương hợp lệ');
      return;
    }

    if (isNaN(areaNum) || areaNum <= 0) {
      setError('Diện tích phòng phải là số dương hợp lệ');
      return;
    }

    setIsSubmitting(true);
    try {
      const created = await roomService.createRoom({
        title: title.trim(),
        description: description.trim() || undefined,
        price: priceNum,
        area: areaNum,
        address: address.trim(),
        ward: ward.trim() || undefined,
        district,
        city,
        amenityIds: selectedAmenityIds,
        images,
      });

      alert('Đăng tin thành công!');
      navigate(`/rooms/${created.id}`);
    } catch (err: any) {
      const msg = err instanceof ApiError ? err.message : err?.message || 'Không thể tạo tin đăng';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="page" style={{ padding: '40px 0', minHeight: '80vh' }}>
      <div className="container" style={{ maxWidth: '840px', margin: '0 auto' }}>
        <div style={{ marginBottom: '28px' }}>
          <span className="section-label">Dành cho chủ nhà</span>
          <h1 style={{ fontSize: '28px', fontWeight: 700, margin: '8px 0' }}>Đăng tin cho thuê phòng trọ</h1>
          <p style={{ color: '#64748b' }}>Điền đầy đủ thông tin để phòng của bạn tiếp cận hàng ngàn người thuê nhanh chóng.</p>
        </div>

        {error && (
          <div style={{
            background: '#fee2e2',
            color: '#b91c1c',
            padding: '12px 16px',
            borderRadius: '8px',
            marginBottom: '24px',
            border: '1px solid #f87171'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ background: '#fff', padding: '32px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          {/* Thông tin cơ bản */}
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#167c5a' }}>
            1. Thông tin cơ bản
          </h2>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px' }}>
              Tiêu đề tin đăng *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: Phòng khép kín full đồ ban công thoáng gần ĐH Quốc Gia"
              required
              style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px' }}>
                Giá thuê hàng tháng (VNĐ) *
              </label>
              <input
                type="number"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="VD: 3500000"
                required
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px' }}>
                Diện tích (m²) *
              </label>
              <input
                type="number"
                value={area}
                onChange={(e) => setArea(e.target.value)}
                placeholder="VD: 25"
                required
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
              />
            </div>
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px' }}>
              Mô tả chi tiết phòng trọ
            </label>
            <textarea
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Mô tả nội thất, giờ giấc, an ninh, tiện ích xung quanh..."
              style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontFamily: 'inherit' }}
            />
          </div>

          {/* Vị trí */}
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#167c5a' }}>
            2. Vị trí phòng trọ
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px' }}>
                Quận / Huyện *
              </label>
              <select
                value={district}
                onChange={(e) => setDistrict(e.target.value)}
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
              >
                {districts.map((d) => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px' }}>
                Phường / Xã
              </label>
              <input
                type="text"
                value={ward}
                onChange={(e) => setWard(e.target.value)}
                placeholder="VD: Dịch Vọng Hậu"
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
              />
            </div>
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px' }}>
              Địa chỉ cụ thể (Số nhà, ngõ ngách, tên đường) *
            </label>
            <input
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="VD: Số 18 Ngõ 175 Cầu Giấy"
              required
              style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
            />
          </div>

          {/* Tiện ích */}
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#167c5a' }}>
            3. Tiện ích có sẵn
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '10px', marginBottom: '24px' }}>
            {availableAmenities.map((amenity) => (
              <label
                key={amenity.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '10px 12px',
                  background: selectedAmenityIds.includes(amenity.id) ? '#dcfce7' : '#f8fafc',
                  borderRadius: '8px',
                  border: `1px solid ${selectedAmenityIds.includes(amenity.id) ? '#86efac' : '#e2e8f0'}`,
                  cursor: 'pointer',
                  fontWeight: selectedAmenityIds.includes(amenity.id) ? 600 : 400
                }}
              >
                <input
                  type="checkbox"
                  checked={selectedAmenityIds.includes(amenity.id)}
                  onChange={() => toggleAmenity(amenity.id)}
                  style={{ accentColor: '#167c5a' }}
                />
                {amenity.name}
              </label>
            ))}
          </div>

          {/* Hình ảnh */}
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#167c5a' }}>
            4. Hình ảnh phòng trọ
          </h2>

          <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
            <input
              type="url"
              value={imageUrlInput}
              onChange={(e) => setImageUrlInput(e.target.value)}
              placeholder="Nhập đường dẫn hình ảnh (URL)"
              style={{ flex: 1, padding: '10px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
            />
            <button
              type="button"
              onClick={addImage}
              className="btn btn-outline"
            >
              + Thêm ảnh
            </button>
          </div>

          {images.length > 0 && (
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginBottom: '28px' }}>
              {images.map((img, idx) => (
                <div
                  key={idx}
                  style={{
                    width: '130px',
                    borderRadius: '8px',
                    overflow: 'hidden',
                    border: img.isPrimary ? '2px solid #167c5a' : '1px solid #e2e8f0',
                    position: 'relative'
                  }}
                >
                  <img src={img.imageUrl} alt="" style={{ width: '100%', height: '90px', objectFit: 'cover' }} />
                  <div style={{ padding: '6px', textAlign: 'center', background: '#f8fafc' }}>
                    <button
                      type="button"
                      onClick={() => setPrimaryImage(idx)}
                      style={{
                        background: 'none',
                        border: 'none',
                        fontSize: '11px',
                        color: img.isPrimary ? '#167c5a' : '#64748b',
                        fontWeight: img.isPrimary ? 700 : 400,
                        cursor: 'pointer'
                      }}
                    >
                      {img.isPrimary ? '★ Ảnh chính' : 'Đặt làm chính'}
                    </button>
                    <button
                      type="button"
                      onClick={() => removeImage(idx)}
                      style={{
                        background: 'none',
                        border: 'none',
                        fontSize: '11px',
                        color: '#dc2626',
                        cursor: 'pointer',
                        display: 'block',
                        margin: '2px auto 0'
                      }}
                    >
                      Xóa
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-full"
            disabled={isSubmitting}
            style={{ minHeight: '48px', fontSize: '16px' }}
          >
            {isSubmitting ? 'Đang tạo tin đăng...' : 'Đăng tin ngay'}
          </button>
        </form>
      </div>
    </main>
  );
}
