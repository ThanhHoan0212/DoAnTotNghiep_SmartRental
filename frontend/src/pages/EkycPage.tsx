import React, { useState, useEffect, useRef } from 'react';
import {
  Camera,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  ArrowRight,
  ArrowLeft,
  RefreshCw,
  FileText,
  CreditCard,
  Smile,
  Check,
  Lock,
  VideoOff,
  ShieldCheck,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ekycService } from '../services/ekycService';
import type { EkycVerificationResponse, EkycPersonalInfo } from '../types/ekyc';

export default function EkycPage() {
  const { user, refreshUser } = useAuth();

  // Wizard state: 1 (Info) | 2 (CCCD Camera) | 3 (Selfie Camera) | 4 (Result)
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3 | 4>(1);

  // Bước 1: Thông tin cá nhân
  const [personalInfo, setPersonalInfo] = useState<EkycPersonalInfo>({
    fullName: user?.fullName || '',
    idCardNumber: user?.idCardNumber || '',
    dob: '2000-01-01',
    gender: 'Nam',
    hometown: 'Thành phố Hồ Chí Minh',
    address: 'Quận 1, Thành phố Hồ Chí Minh',
    phone: user?.phone || '',
  });
  const [infoError, setInfoError] = useState<string | null>(null);

  // Bước 2: Chụp CCCD (mặt trước & mặt sau)
  const [cccdSubStep, setCccdSubStep] = useState<'front' | 'back'>('front');
  const [frontFile, setFrontFile] = useState<File | null>(null);
  const [backFile, setBackFile] = useState<File | null>(null);
  const [frontPreview, setFrontPreview] = useState<string | null>(null);
  const [backPreview, setBackPreview] = useState<string | null>(null);

  // Bước 3: Xác thực khuôn mặt Selfie
  const [selfieFile, setSelfieFile] = useState<File | null>(null);
  const [selfiePreview, setSelfiePreview] = useState<string | null>(null);

  // Camera stream controls
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [isCameraActive, setIsCameraActive] = useState<boolean>(false);
  const [cameraError, setCameraError] = useState<string | null>(null);

  // Trạng thái gọi AI & Kết quả
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [processingStage, setProcessingStage] = useState<string>('');
  const [result, setResult] = useState<EkycVerificationResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Dọn dẹp object URLs khi component unmount
  useEffect(() => {
    return () => {
      stopCamera();
      if (frontPreview) URL.revokeObjectURL(frontPreview);
      if (backPreview) URL.revokeObjectURL(backPreview);
      if (selfiePreview) URL.revokeObjectURL(selfiePreview);
    };
  }, []);

  // Tự động khởi động camera khi chuyển sang Bước 2 hoặc Bước 3
  useEffect(() => {
    if (currentStep === 2) {
      startCamera('environment');
    } else if (currentStep === 3) {
      startCamera('user');
    } else {
      stopCamera();
    }
  }, [currentStep, cccdSubStep]);

  // Khởi động Camera thiết bị
  const startCamera = async (facingMode: 'user' | 'environment') => {
    stopCamera();
    setCameraError(null);

    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('Trình duyệt không hỗ trợ truy cập Camera trực tiếp.');
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: facingMode },
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: false,
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setIsCameraActive(true);
    } catch (err: any) {
      console.warn('Không thể truy cập camera:', err);
      setIsCameraActive(false);
      setCameraError(
        err.name === 'NotAllowedError'
          ? 'Quyền truy cập Camera bị từ chối. Vui lòng cấp quyền Camera trên trình duyệt để chụp ảnh.'
          : 'Không tìm thấy thiết bị Camera khả dụng hoặc Camera đang được ứng dụng khác sử dụng.'
      );
    }
  };

  // Tắt Camera
  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setIsCameraActive(false);
  };

  // Hàm chụp ảnh từ video stream chuyển sang File
  const capturePhoto = (filename: string): Promise<{ file: File; preview: string }> => {
    return new Promise((resolve, reject) => {
      if (!videoRef.current || !canvasRef.current) {
        reject(new Error('Video stream chưa sẵn sàng'));
        return;
      }

      const video = videoRef.current;
      const canvas = canvasRef.current;
      canvas.width = video.videoWidth || 1280;
      canvas.height = video.videoHeight || 720;

      const ctx = canvas.getContext('2d');
      if (!ctx) {
        reject(new Error('Không thể khởi tạo canvas 2D'));
        return;
      }

      // Vẽ frame từ video vào canvas
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error('Chuyển đổi ảnh thất bại'));
            return;
          }
          const file = new File([blob], filename, { type: 'image/jpeg' });
          const preview = URL.createObjectURL(blob);
          resolve({ file, preview });
        },
        'image/jpeg',
        0.95
      );
    });
  };

  // Xử lý chụp ảnh mặt trước CCCD
  const handleCaptureFront = async () => {
    try {
      if (!isCameraActive) {
        alert('Camera chưa sẵn sàng hoặc bị chặn. Vui lòng cấp quyền truy cập Camera trên trình duyệt để chụp ảnh thật.');
        return;
      }
      const captured = await capturePhoto('cccd_front.jpg');
      setFrontFile(captured.file);
      setFrontPreview(captured.preview);
    } catch (err) {
      alert('Không thể chụp ảnh: ' + (err as Error).message);
    }
  };

  // Xử lý chụp ảnh mặt sau CCCD
  const handleCaptureBack = async () => {
    try {
      if (!isCameraActive) {
        alert('Camera chưa sẵn sàng hoặc bị chặn. Vui lòng cấp quyền truy cập Camera trên trình duyệt để chụp ảnh thật.');
        return;
      }
      const captured = await capturePhoto('cccd_back.jpg');
      setBackFile(captured.file);
      setBackPreview(captured.preview);
    } catch (err) {
      alert('Không thể chụp ảnh: ' + (err as Error).message);
    }
  };

  // Xử lý chụp ảnh chân dung Selfie
  const handleCaptureSelfie = async () => {
    try {
      if (!isCameraActive) {
        alert('Camera chưa sẵn sàng hoặc bị chặn. Vui lòng cấp quyền truy cập Camera trên trình duyệt để chụp ảnh chân dung selfie.');
        return;
      }
      const captured = await capturePhoto('selfie.jpg');
      setSelfieFile(captured.file);
      setSelfiePreview(captured.preview);
    } catch (err) {
      alert('Không thể chụp ảnh: ' + (err as Error).message);
    }
  };

  // Xử lý hoàn tất Bước 1 và chuyển sang Bước 2
  const handleStep1Submit = (e: React.FormEvent) => {
    e.preventDefault();
    setInfoError(null);

    if (!personalInfo.fullName.trim()) {
      setInfoError('Vui lòng nhập họ và tên đầy đủ theo CCCD.');
      return;
    }
    if (!personalInfo.idCardNumber.trim() || !/^\d{9,12}$/.test(personalInfo.idCardNumber.trim())) {
      setInfoError('Số Căn cước công dân phải là dãy gồm 9 hoặc 12 chữ số.');
      return;
    }
    if (!personalInfo.dob) {
      setInfoError('Vui lòng chọn ngày tháng năm sinh.');
      return;
    }
    if (!personalInfo.address.trim()) {
      setInfoError('Vui lòng nhập địa chỉ thường trú.');
      return;
    }

    setCurrentStep(2);
  };

  // Xử lý gửi toàn bộ dữ liệu đi xác thực AI (FPT.AI)
  const handleFinalVerify = async () => {
    setErrorMessage(null);

    if (!frontFile || !backFile || !selfieFile) {
      setErrorMessage('Vui lòng hoàn tất chụp ảnh trực tiếp cả 3 ảnh: CCCD mặt trước, CCCD mặt sau và ảnh Selfie.');
      return;
    }

    try {
      stopCamera();
      setIsProcessing(true);

      // Mô phỏng hiệu ứng quét AI từng giai đoạn trực quan
      setProcessingStage('Đang kết nối hạ tầng FPT.AI và truyền dữ liệu mã hóa...');
      await new Promise((r) => setTimeout(r, 600));

      setProcessingStage('Đang bóc tách thông tin thẻ Căn cước công dân (FPT.AI OCR)...');
      await new Promise((r) => setTimeout(r, 700));

      setProcessingStage('Đang so khớp vector khuôn mặt CCCD với ảnh Selfie (Face Matching v4)...');
      const res = await ekycService.verifyEkyc(
        frontFile,
        backFile,
        selfieFile,
        personalInfo
      );

      setProcessingStage('Đang đối chiếu thông tin người dùng và cấp chứng nhận eKYC...');
      await new Promise((r) => setTimeout(r, 400));

      setResult(res);
      await refreshUser();
      setCurrentStep(4);
    } catch (err: any) {
      console.error('Lỗi khi gọi API eKYC:', err);
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Xác thực eKYC không thành công. Độ tương đồng khuôn mặt chưa đạt ngưỡng tin cậy (> 85%). Vui lòng chụp lại.';
      setErrorMessage(msg);
      setCurrentStep(4);
    } finally {
      setIsProcessing(false);
    }
  };

  // Thiết lập lại để thử lại quy trình
  const handleResetFlow = () => {
    setResult(null);
    setErrorMessage(null);
    setFrontFile(null);
    setBackFile(null);
    setSelfieFile(null);
    if (frontPreview) URL.revokeObjectURL(frontPreview);
    if (backPreview) URL.revokeObjectURL(backPreview);
    if (selfiePreview) URL.revokeObjectURL(selfiePreview);
    setFrontPreview(null);
    setBackPreview(null);
    setSelfiePreview(null);
    setCccdSubStep('front');
    setCurrentStep(1);
  };

  return (
    <main className="page" style={{ padding: '40px 0', minHeight: '85vh', background: '#f8fafc' }}>
      {/* Canvas ẩn phục vụ chụp frame từ video */}
      <canvas ref={canvasRef} style={{ display: 'none' }} />

      <div className="container" style={{ maxWidth: '860px', margin: '0 auto' }}>
        {/* Banner giới thiệu */}
        <div style={{ marginBottom: '28px', textAlign: 'center' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              background: '#ecfdf5',
              color: '#059669',
              padding: '6px 14px',
              borderRadius: '20px',
              fontSize: '13px',
              fontWeight: 700,
              marginBottom: '10px',
              border: '1px solid #a7f3d0',
            }}
          >
            <Sparkles size={16} />
            <span>HỆ THỐNG XÁC THỰC NGƯỜI DÙNG THẬT eKYC CHUẨN FPT.AI</span>
          </div>
          <h1 style={{ fontSize: '28px', fontWeight: 800, color: '#0f172a', margin: '4px 0 8px 0' }}>
            Định Danh Điện Tử An Toàn (eKYC)
          </h1>
          <p style={{ color: '#64748b', fontSize: '15px', maxWidth: '640px', margin: '0 auto' }}>
            Quy trình gồm 3 bước bắt buộc: <strong>Khai báo thông tin</strong> $\rightarrow${' '}
            <strong>Chụp CCCD trực tiếp</strong> $\rightarrow$ <strong>Selfie khuôn mặt trực tiếp</strong>. Điểm tin cậy
            yêu cầu <strong>&gt; 85%</strong>.
          </p>
        </div>

        {/* Thanh tiến trình Stepper 3 bước */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: '#ffffff',
            padding: '16px 24px',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            marginBottom: '28px',
            position: 'relative',
          }}
        >
          {/* Bước 1 */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              color: currentStep >= 1 ? '#167c5a' : '#94a3b8',
              fontWeight: 700,
              fontSize: '14px',
            }}
          >
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: currentStep > 1 ? '#167c5a' : currentStep === 1 ? '#e0f2fe' : '#f1f5f9',
                color: currentStep > 1 ? '#ffffff' : currentStep === 1 ? '#0369a1' : '#64748b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '14px',
              }}
            >
              {currentStep > 1 ? <Check size={18} /> : '1'}
            </div>
            <span>1. Thông tin cá nhân</span>
          </div>

          <div
            style={{
              flex: 1,
              height: '2px',
              background: currentStep > 1 ? '#167c5a' : '#e2e8f0',
              margin: '0 12px',
            }}
          />

          {/* Bước 2 */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              color: currentStep >= 2 ? '#167c5a' : '#94a3b8',
              fontWeight: 700,
              fontSize: '14px',
            }}
          >
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: currentStep > 2 ? '#167c5a' : currentStep === 2 ? '#e0f2fe' : '#f1f5f9',
                color: currentStep > 2 ? '#ffffff' : currentStep === 2 ? '#0369a1' : '#64748b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '14px',
              }}
            >
              {currentStep > 2 ? <Check size={18} /> : '2'}
            </div>
            <span>2. Chụp CCCD trực tiếp</span>
          </div>

          <div
            style={{
              flex: 1,
              height: '2px',
              background: currentStep > 2 ? '#167c5a' : '#e2e8f0',
              margin: '0 12px',
            }}
          />

          {/* Bước 3 */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              color: currentStep >= 3 ? '#167c5a' : '#94a3b8',
              fontWeight: 700,
              fontSize: '14px',
            }}
          >
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: currentStep >= 3 ? (result?.isSuccess ? '#167c5a' : '#e0f2fe') : '#f1f5f9',
                color: currentStep >= 3 ? (result?.isSuccess ? '#ffffff' : '#0369a1') : '#64748b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '14px',
              }}
            >
              {result?.isSuccess ? <Check size={18} /> : '3'}
            </div>
            <span>3. Selfie khuôn mặt</span>
          </div>
        </div>

        {/* NỘI DUNG TỪNG BƯỚC */}

        {/* BƯỚC 1: NHẬP THÔNG TIN CÁ NHÂN */}
        {currentStep === 1 && (
          <div
            style={{
              background: '#ffffff',
              padding: '32px',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              boxShadow: '0 4px 16px rgba(0,0,0,0.03)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <FileText size={22} color="#167c5a" />
              <h2 style={{ fontSize: '20px', fontWeight: 700, margin: 0, color: '#0f172a' }}>
                Bước 1: Khai báo thông tin cá nhân theo CCCD
              </h2>
            </div>
            <p style={{ color: '#64748b', fontSize: '14px', marginBottom: '24px' }}>
              Vui lòng nhập chính xác thông tin in trên thẻ Căn cước công dân của bạn. Hệ thống AI sẽ tự động đối chiếu
              thông tin này với kết quả quét thẻ ở Bước 2.
            </p>

            {infoError && (
              <div
                style={{
                  background: '#fef2f2',
                  border: '1px solid #fecaca',
                  color: '#b91c1c',
                  padding: '12px 16px',
                  borderRadius: '8px',
                  fontSize: '14px',
                  marginBottom: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <AlertCircle size={18} />
                <span>{infoError}</span>
              </div>
            )}

            <form onSubmit={handleStep1Submit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                    Họ và tên đầy đủ (in hoa) *
                  </label>
                  <input
                    type="text"
                    required
                    value={personalInfo.fullName}
                    onChange={(e) => setPersonalInfo({ ...personalInfo, fullName: e.target.value.toUpperCase() })}
                    placeholder="VD: NGUYỄN VĂN A"
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                    Số Căn cước công dân (12 số) *
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={12}
                    value={personalInfo.idCardNumber}
                    onChange={(e) => setPersonalInfo({ ...personalInfo, idCardNumber: e.target.value.replace(/\D/g, '') })}
                    placeholder="VD: 079201012345"
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                      letterSpacing: '1px',
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', marginBottom: '16px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                    Ngày sinh *
                  </label>
                  <input
                    type="date"
                    required
                    value={personalInfo.dob}
                    onChange={(e) => setPersonalInfo({ ...personalInfo, dob: e.target.value })}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                    Giới tính *
                  </label>
                  <select
                    value={personalInfo.gender}
                    onChange={(e) => setPersonalInfo({ ...personalInfo, gender: e.target.value as any })}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                    }}
                  >
                    <option value="Nam">Nam</option>
                    <option value="Nữ">Nữ</option>
                    <option value="Khác">Khác</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                    Số điện thoại
                  </label>
                  <input
                    type="tel"
                    value={personalInfo.phone}
                    onChange={(e) => setPersonalInfo({ ...personalInfo, phone: e.target.value })}
                    placeholder="0987654321"
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                    }}
                  />
                </div>
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                  Quê quán (theo CCCD)
                </label>
                <input
                  type="text"
                  value={personalInfo.hometown}
                  onChange={(e) => setPersonalInfo({ ...personalInfo, hometown: e.target.value })}
                  placeholder="VD: Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh"
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    fontSize: '14px',
                  }}
                />
              </div>

              <div style={{ marginBottom: '24px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                  Nơi thường trú (theo CCCD) *
                </label>
                <input
                  type="text"
                  required
                  value={personalInfo.address}
                  onChange={(e) => setPersonalInfo({ ...personalInfo, address: e.target.value })}
                  placeholder="VD: 123 Nguyễn Thị Minh Khai, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh"
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    fontSize: '14px',
                  }}
                />
              </div>

              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '8px',
                  padding: '12px 16px',
                  marginBottom: '24px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  fontSize: '13px',
                  color: '#475569',
                }}
              >
                <Lock size={16} color="#167c5a" />
                <span>
                  Thông tin của bạn được mã hóa AES-256 và chỉ phục vụ việc đối chiếu xác minh danh tính người dùng thật.
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '12px 24px', fontSize: '15px' }}
                >
                  <span>Tiếp tục: Bước 2 - Chụp ảnh CCCD</span>
                  <ArrowRight size={18} />
                </button>
              </div>
            </form>
          </div>
        )}

        {/* BƯỚC 2: CHỤP CCCD TRỰC TIẾP (BẮT BUỘC CAMERA, KHÔNG CHO TẢI ẢNH) */}
        {currentStep === 2 && (
          <div
            style={{
              background: '#ffffff',
              padding: '32px',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              boxShadow: '0 4px 16px rgba(0,0,0,0.03)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <CreditCard size={22} color="#167c5a" />
                <h2 style={{ fontSize: '20px', fontWeight: 700, margin: 0, color: '#0f172a' }}>
                  Bước 2: Chụp ảnh Căn cước công dân trực tiếp qua Camera
                </h2>
              </div>

              <div style={{ display: 'flex', gap: '8px' }}>
                <span
                  style={{
                    fontSize: '12px',
                    fontWeight: 700,
                    padding: '4px 10px',
                    borderRadius: '6px',
                    background: cccdSubStep === 'front' ? '#167c5a' : '#f1f5f9',
                    color: cccdSubStep === 'front' ? '#ffffff' : '#64748b',
                  }}
                >
                  Mặt trước {frontFile && '✓'}
                </span>
                <span
                  style={{
                    fontSize: '12px',
                    fontWeight: 700,
                    padding: '4px 10px',
                    borderRadius: '6px',
                    background: cccdSubStep === 'back' ? '#167c5a' : '#f1f5f9',
                    color: cccdSubStep === 'back' ? '#ffffff' : '#64748b',
                  }}
                >
                  Mặt sau {backFile && '✓'}
                </span>
              </div>
            </div>

            <p style={{ color: '#b45309', fontSize: '13px', background: '#fffbeb', padding: '10px 14px', borderRadius: '8px', border: '1px solid #fde68a', marginBottom: '20px' }}>
              ⚠️ <strong>Lưu ý bảo mật:</strong> Hệ thống bắt buộc người dùng <strong>chụp ảnh trực tiếp</strong> qua Camera thiết bị, không cho phép tải ảnh có sẵn để ngăn chặn hành vi sử dụng CCCD giả mạo hoặc ảnh trên mạng.
            </p>

            {cameraError && (
              <div
                style={{
                  background: '#fef2f2',
                  border: '1px solid #fecaca',
                  color: '#b91c1c',
                  padding: '14px',
                  borderRadius: '10px',
                  marginBottom: '20px',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '10px',
                  fontSize: '13px',
                }}
              >
                <VideoOff size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
                <div style={{ flex: 1 }}>
                  <strong>Thông báo Camera:</strong> {cameraError}
                  <div style={{ marginTop: '8px', display: 'flex', gap: '8px' }}>
                    <button
                      type="button"
                      onClick={() => startCamera('environment')}
                      style={{
                        background: '#dc2626',
                        color: '#fff',
                        border: 'none',
                        padding: '6px 12px',
                        borderRadius: '6px',
                        fontSize: '12px',
                        cursor: 'pointer',
                        fontWeight: 600,
                      }}
                    >
                      🔄 Thử lại kết nối Camera
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Khung Camera Viewport trực tiếp */}
            <div
              style={{
                position: 'relative',
                background: '#0f172a',
                borderRadius: '12px',
                overflow: 'hidden',
                aspectRatio: '16/9',
                maxHeight: '440px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '20px',
              }}
            >
              {/* Video Element nhận luồng camera */}
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover',
                  display:
                    (cccdSubStep === 'front' && !frontPreview) || (cccdSubStep === 'back' && !backPreview)
                      ? 'block'
                      : 'none',
                }}
              />

              {/* Lớp phủ hướng dẫn căn chỉnh khung thẻ CCCD */}
              {((cccdSubStep === 'front' && !frontPreview) || (cccdSubStep === 'back' && !backPreview)) && (
                <div
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    pointerEvents: 'none',
                  }}
                >
                  {/* Khung viền thẻ CCCD */}
                  <div
                    style={{
                      width: '78%',
                      height: '75%',
                      border: '2px dashed #10b981',
                      borderRadius: '16px',
                      boxShadow: '0 0 0 9999px rgba(15, 23, 42, 0.55)',
                      position: 'relative',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {/* Góc viền sáng */}
                    <div style={{ position: 'absolute', top: '-2px', left: '-2px', width: '24px', height: '24px', borderTop: '4px solid #34d399', borderLeft: '4px solid #34d399', borderTopLeftRadius: '14px' }} />
                    <div style={{ position: 'absolute', top: '-2px', right: '-2px', width: '24px', height: '24px', borderTop: '4px solid #34d399', borderRight: '4px solid #34d399', borderTopRightRadius: '14px' }} />
                    <div style={{ position: 'absolute', bottom: '-2px', left: '-2px', width: '24px', height: '24px', borderBottom: '4px solid #34d399', borderLeft: '4px solid #34d399', borderBottomLeftRadius: '14px' }} />
                    <div style={{ position: 'absolute', bottom: '-2px', right: '-2px', width: '24px', height: '24px', borderBottom: '4px solid #34d399', borderRight: '4px solid #34d399', borderBottomRightRadius: '14px' }} />

                    <span
                      style={{
                        background: 'rgba(0,0,0,0.65)',
                        color: '#ffffff',
                        padding: '6px 14px',
                        borderRadius: '20px',
                        fontSize: '13px',
                        fontWeight: 600,
                        letterSpacing: '0.5px',
                      }}
                    >
                      {cccdSubStep === 'front' ? '🪪 Căn chỉnh MẶT TRƯỚC CCCD' : '🪪 Căn chỉnh MẶT SAU (CHIP & VÂN TAY)'}
                    </span>
                  </div>
                </div>
              )}

              {/* Hiển thị xem trước ảnh vừa chụp nếu đã chụp xong mặt hiện tại */}
              {cccdSubStep === 'front' && frontPreview && (
                <img
                  src={frontPreview}
                  alt="Mặt trước CCCD vừa chụp"
                  style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                />
              )}

              {cccdSubStep === 'back' && backPreview && (
                <img
                  src={backPreview}
                  alt="Mặt sau CCCD vừa chụp"
                  style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                />
              )}
            </div>

            {/* Các nút bấm thao tác chụp ảnh */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px', marginBottom: '24px' }}>
              {cccdSubStep === 'front' && !frontPreview && (
                <button
                  type="button"
                  onClick={handleCaptureFront}
                  className="btn btn-primary"
                  style={{
                    padding: '14px 28px',
                    fontSize: '16px',
                    fontWeight: 700,
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '10px',
                    borderRadius: '30px',
                    boxShadow: '0 4px 14px rgba(22, 124, 90, 0.4)',
                  }}
                >
                  <Camera size={20} />
                  <span>Chụp ảnh MẶT TRƯỚC CCCD</span>
                </button>
              )}

              {cccdSubStep === 'front' && frontPreview && (
                <>
                  <button
                    type="button"
                    onClick={() => {
                      setFrontPreview(null);
                      setFrontFile(null);
                      startCamera('environment');
                    }}
                    className="btn btn-outline"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <RefreshCw size={16} />
                    <span>Chụp lại mặt trước</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setCccdSubStep('back');
                      startCamera('environment');
                    }}
                    className="btn btn-primary"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <span>Tiếp tục chụp mặt sau</span>
                    <ArrowRight size={16} />
                  </button>
                </>
              )}

              {cccdSubStep === 'back' && !backPreview && (
                <>
                  <button
                    type="button"
                    onClick={() => setCccdSubStep('front')}
                    className="btn btn-outline"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <ArrowLeft size={16} />
                    <span>Xem lại mặt trước</span>
                  </button>

                  <button
                    type="button"
                    onClick={handleCaptureBack}
                    className="btn btn-primary"
                    style={{
                      padding: '14px 28px',
                      fontSize: '16px',
                      fontWeight: 700,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '10px',
                      borderRadius: '30px',
                      boxShadow: '0 4px 14px rgba(22, 124, 90, 0.4)',
                    }}
                  >
                    <Camera size={20} />
                    <span>Chụp ảnh MẶT SAU CCCD</span>
                  </button>
                </>
              )}

              {cccdSubStep === 'back' && backPreview && (
                <>
                  <button
                    type="button"
                    onClick={() => {
                      setBackPreview(null);
                      setBackFile(null);
                      startCamera('environment');
                    }}
                    className="btn btn-outline"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <RefreshCw size={16} />
                    <span>Chụp lại mặt sau</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setCurrentStep(3)}
                    className="btn btn-primary"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontWeight: 700 }}
                  >
                    <span>Tiếp tục: Bước 3 - Xác thực khuôn mặt</span>
                    <ArrowRight size={16} />
                  </button>
                </>
              )}
            </div>

            {/* Bảng xem trước tóm tắt 2 mặt đã chụp */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', borderTop: '1px solid #e2e8f0', paddingTop: '20px' }}>
              <div
                style={{
                  border: '1px solid #cbd5e1',
                  borderRadius: '10px',
                  padding: '12px',
                  background: frontFile ? '#f0fdf4' : '#f8fafc',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '13px', fontWeight: 600, color: frontFile ? '#166534' : '#64748b', marginBottom: '8px' }}>
                  {frontFile ? '✓ Mặt trước: Đã chụp' : '○ Mặt trước: Chưa chụp'}
                </div>
                {frontPreview ? (
                  <img src={frontPreview} alt="CCCD Front" style={{ height: '110px', width: '100%', objectFit: 'contain', borderRadius: '6px' }} />
                ) : (
                  <div style={{ height: '110px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8', fontSize: '12px' }}>
                    Chưa có ảnh
                  </div>
                )}
              </div>

              <div
                style={{
                  border: '1px solid #cbd5e1',
                  borderRadius: '10px',
                  padding: '12px',
                  background: backFile ? '#f0fdf4' : '#f8fafc',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '13px', fontWeight: 600, color: backFile ? '#166534' : '#64748b', marginBottom: '8px' }}>
                  {backFile ? '✓ Mặt sau: Đã chụp' : '○ Mặt sau: Chưa chụp'}
                </div>
                {backPreview ? (
                  <img src={backPreview} alt="CCCD Back" style={{ height: '110px', width: '100%', objectFit: 'contain', borderRadius: '6px' }} />
                ) : (
                  <div style={{ height: '110px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8', fontSize: '12px' }}>
                    Chưa có ảnh
                  </div>
                )}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '24px' }}>
              <button
                type="button"
                onClick={() => setCurrentStep(1)}
                className="btn btn-outline"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
              >
                <ArrowLeft size={16} />
                <span>Quay lại Bước 1</span>
              </button>

              {frontFile && backFile && (
                <button
                  type="button"
                  onClick={() => setCurrentStep(3)}
                  className="btn btn-primary"
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontWeight: 700 }}
                >
                  <span>Chuyển sang Bước 3</span>
                  <ArrowRight size={16} />
                </button>
              )}
            </div>
          </div>
        )}

        {/* BƯỚC 3: XÁC THỰC KHUÔN MẶT SELFIE (BẮT BUỘC CAMERA, KHÔNG CHO TẢI ẢNH) */}
        {currentStep === 3 && (
          <div
            style={{
              background: '#ffffff',
              padding: '32px',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              boxShadow: '0 4px 16px rgba(0,0,0,0.03)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <Smile size={22} color="#167c5a" />
              <h2 style={{ fontSize: '20px', fontWeight: 700, margin: 0, color: '#0f172a' }}>
                Bước 3: Chụp ảnh chân dung Selfie trực tiếp (Face Matching)
              </h2>
            </div>
            <p style={{ color: '#64748b', fontSize: '14px', marginBottom: '20px' }}>
              Vui lòng nhìn thẳng vào ống kính camera phía trước. Không đeo khẩu trang hoặc kính râm. Hệ thống FPT.AI sẽ
              đối chiếu khuôn mặt selfie này với ảnh chân dung trên thẻ CCCD vừa chụp (ngưỡng an toàn &gt; 85%).
            </p>

            {cameraError && (
              <div
                style={{
                  background: '#fef2f2',
                  border: '1px solid #fecaca',
                  color: '#b91c1c',
                  padding: '14px',
                  borderRadius: '10px',
                  marginBottom: '20px',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '10px',
                  fontSize: '13px',
                }}
              >
                <VideoOff size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
                <div style={{ flex: 1 }}>
                  <strong>Thông báo Camera:</strong> {cameraError}
                  <div style={{ marginTop: '8px', display: 'flex', gap: '8px' }}>
                    <button
                      type="button"
                      onClick={() => startCamera('user')}
                      style={{
                        background: '#dc2626',
                        color: '#fff',
                        border: 'none',
                        padding: '6px 12px',
                        borderRadius: '6px',
                        fontSize: '12px',
                        cursor: 'pointer',
                        fontWeight: 600,
                      }}
                    >
                      🔄 Thử lại kết nối Camera
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Khung Camera Selfie */}
            <div
              style={{
                position: 'relative',
                background: '#0f172a',
                borderRadius: '12px',
                overflow: 'hidden',
                aspectRatio: '4/3',
                maxHeight: '440px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 24px auto',
                maxWidth: '560px',
              }}
            >
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover',
                  display: !selfiePreview ? 'block' : 'none',
                  transform: 'scaleX(-1)', // Hiệu ứng gương tự nhiên cho camera trước
                }}
              />

              {/* Khung hướng dẫn Elip Oval khuôn mặt */}
              {!selfiePreview && (
                <div
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    pointerEvents: 'none',
                  }}
                >
                  <div
                    style={{
                      width: '240px',
                      height: '320px',
                      border: '3px dashed #10b981',
                      borderRadius: '50%',
                      boxShadow: '0 0 0 9999px rgba(15, 23, 42, 0.65)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <span
                      style={{
                        background: 'rgba(0,0,0,0.7)',
                        color: '#ffffff',
                        padding: '6px 14px',
                        borderRadius: '20px',
                        fontSize: '13px',
                        fontWeight: 600,
                      }}
                    >
                      👤 Giữ thẳng khuôn mặt
                    </span>
                  </div>
                </div>
              )}

              {/* Ảnh xem trước sau khi chụp */}
              {selfiePreview && (
                <img
                  src={selfiePreview}
                  alt="Ảnh chân dung Selfie vừa chụp"
                  style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                />
              )}
            </div>

            {/* Các nút bấm thao tác chụp Selfie */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px', marginBottom: '28px' }}>
              {!selfiePreview ? (
                <button
                  type="button"
                  onClick={handleCaptureSelfie}
                  className="btn btn-primary"
                  style={{
                    padding: '14px 28px',
                    fontSize: '16px',
                    fontWeight: 700,
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '10px',
                    borderRadius: '30px',
                    boxShadow: '0 4px 14px rgba(22, 124, 90, 0.4)',
                  }}
                >
                  <Camera size={20} />
                  <span>📸 Chụp ảnh chân dung Selfie</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => {
                    setSelfiePreview(null);
                    setSelfieFile(null);
                    startCamera('user');
                  }}
                  className="btn btn-outline"
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                >
                  <RefreshCw size={16} />
                  <span>Chụp lại ảnh Selfie</span>
                </button>
              )}
            </div>

            {/* Thẻ tóm tắt tổng thể dữ liệu trước khi gửi xác thực */}
            {selfieFile && frontFile && backFile && (
              <div
                style={{
                  background: '#f0fdf4',
                  border: '1px solid #bbf7d0',
                  borderRadius: '12px',
                  padding: '20px',
                  marginBottom: '24px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#166534', fontWeight: 700, fontSize: '15px', marginBottom: '12px' }}>
                  <CheckCircle2 size={18} />
                  <span>Đã thu thập đầy đủ dữ liệu 3 bước. Sẵn sàng thẩm định AI!</span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '16px', fontSize: '13px', color: '#334155' }}>
                  <div>
                    <div><strong>Họ tên:</strong> {personalInfo.fullName}</div>
                    <div><strong>Số CCCD:</strong> {personalInfo.idCardNumber}</div>
                    <div><strong>Ngày sinh:</strong> {personalInfo.dob} • <strong>Giới tính:</strong> {personalInfo.gender}</div>
                    <div><strong>Thường trú:</strong> {personalInfo.address}</div>
                  </div>

                  <div style={{ display: 'flex', gap: '8px' }}>
                    <div style={{ flex: 1, textAlign: 'center' }}>
                      <img src={frontPreview!} alt="Front" style={{ width: '100%', height: '60px', objectFit: 'cover', borderRadius: '4px', border: '1px solid #cbd5e1' }} />
                      <span style={{ fontSize: '10px', color: '#166534', fontWeight: 600 }}>CCCD Trước</span>
                    </div>
                    <div style={{ flex: 1, textAlign: 'center' }}>
                      <img src={backPreview!} alt="Back" style={{ width: '100%', height: '60px', objectFit: 'cover', borderRadius: '4px', border: '1px solid #cbd5e1' }} />
                      <span style={{ fontSize: '10px', color: '#166534', fontWeight: 600 }}>CCCD Sau</span>
                    </div>
                    <div style={{ flex: 1, textAlign: 'center' }}>
                      <img src={selfiePreview!} alt="Selfie" style={{ width: '100%', height: '60px', objectFit: 'cover', borderRadius: '4px', border: '1px solid #cbd5e1' }} />
                      <span style={{ fontSize: '10px', color: '#166534', fontWeight: 600 }}>Ảnh Selfie</span>
                    </div>
                  </div>
                </div>

                {/* Cổng xác thực FPT.AI Trực Tiếp (Live Production) */}
                <div
                  style={{
                    background: '#f8fafc',
                    border: '1px solid #e2e8f0',
                    borderRadius: '10px',
                    padding: '14px 18px',
                    marginTop: '16px',
                    fontSize: '13px',
                    color: '#334155',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    textAlign: 'left',
                  }}
                >
                  <ShieldCheck size={26} color="#167c5a" style={{ flexShrink: 0 }} />
                  <div>
                    <div style={{ fontWeight: 700, color: '#0f172a', marginBottom: '2px' }}>
                      Cổng xác thực FPT.AI Trực Tiếp (Live Production)
                    </div>
                    <div style={{ fontSize: '12px', color: '#64748b' }}>
                      Hình ảnh chụp trực tiếp sẽ được gửi tới FPT.AI IDR VNM và FaceMatch v4. Yêu cầu độ tương đồng khuôn mặt đạt trên 85.0% và thông tin CCCD trùng khớp 100% để cấp chứng nhận.
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: '20px', textAlign: 'center' }}>
                  <button
                    type="button"
                    onClick={handleFinalVerify}
                    className="btn btn-primary"
                    style={{
                      padding: '14px 32px',
                      fontSize: '16px',
                      fontWeight: 700,
                      borderRadius: '8px',
                      boxShadow: '0 4px 16px rgba(22, 124, 90, 0.4)',
                    }}
                  >
                    🚀 GỬI DỮ LIỆU XÁC THỰC VỚI FPT.AI (&gt; 85%)
                  </button>
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
              <button
                type="button"
                onClick={() => setCurrentStep(2)}
                className="btn btn-outline"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
              >
                <ArrowLeft size={16} />
                <span>Quay lại Bước 2</span>
              </button>
            </div>
          </div>
        )}

        {/* BƯỚC 4: MÀN HÌNH ĐANG XỬ LÝ HOẶC KẾT QUẢ XÁC THỰC */}
        {isProcessing && (
          <div
            style={{
              background: '#ffffff',
              padding: '48px 32px',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              textAlign: 'center',
              boxShadow: '0 4px 20px rgba(0,0,0,0.04)',
            }}
          >
            <div style={{ width: '64px', height: '64px', margin: '0 auto 20px auto', borderRadius: '50%', background: '#ecfdf5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <RefreshCw size={32} color="#167c5a" style={{ animation: 'spin 1.5s linear infinite' }} />
            </div>
            <h3 style={{ fontSize: '20px', fontWeight: 700, color: '#0f172a', marginBottom: '8px' }}>
              Đang phân tích trí tuệ nhân tạo (FPT.AI Vision & Face Matching)...
            </h3>
            <p style={{ color: '#64748b', fontSize: '14px', maxWidth: '500px', margin: '0 auto 24px auto' }}>
              {processingStage}
            </p>
            <div style={{ width: '280px', height: '6px', background: '#e2e8f0', borderRadius: '3px', margin: '0 auto', overflow: 'hidden' }}>
              <div style={{ width: '70%', height: '100%', background: '#167c5a', borderRadius: '3px', animation: 'indeterminate 2s infinite linear' }} />
            </div>
          </div>
        )}

        {!isProcessing && currentStep === 4 && (
          <div
            style={{
              background: '#ffffff',
              padding: '36px',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              boxShadow: '0 4px 20px rgba(0,0,0,0.04)',
            }}
          >
            {result?.isSuccess ? (
              <div>
                <div style={{ textAlign: 'center', marginBottom: '28px' }}>
                  <div
                    style={{
                      width: '72px',
                      height: '72px',
                      borderRadius: '50%',
                      background: '#dcfce7',
                      color: '#15803d',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      margin: '0 auto 16px auto',
                    }}
                  >
                    <CheckCircle2 size={42} />
                  </div>
                  <h2 style={{ fontSize: '24px', fontWeight: 800, color: '#166534', margin: '0 0 6px 0' }}>
                    Xác Thực Danh Tính eKYC Thành Công!
                  </h2>
                  <p style={{ color: '#475569', fontSize: '15px' }}>
                    Tài khoản của bạn đã được chứng nhận là <strong>Người dùng thật</strong> với độ tin cậy đạt chuẩn an toàn.
                  </p>
                </div>

                {/* Đồng hồ điểm tin cậy */}
                <div
                  style={{
                    background: '#f0fdf4',
                    border: '1px solid #bbf7d0',
                    borderRadius: '12px',
                    padding: '20px',
                    marginBottom: '28px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-around',
                    flexWrap: 'wrap',
                    gap: '16px',
                  }}
                >
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '13px', color: '#166534', fontWeight: 600 }}>Độ tương đồng khuôn mặt</div>
                    <div style={{ fontSize: '36px', fontWeight: 800, color: '#15803d' }}>
                      {result.confidenceScore.toFixed(1)}%
                    </div>
                    <div style={{ fontSize: '12px', color: '#15803d', fontWeight: 700 }}>✓ Vượt ngưỡng yêu cầu (&gt; 85.0%)</div>
                  </div>

                  <div style={{ height: '60px', width: '1px', background: '#bbf7d0' }} />

                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '13px', color: '#166534', fontWeight: 600 }}>Trạng thái hồ sơ</div>
                    <div style={{ fontSize: '18px', fontWeight: 700, color: '#15803d', marginTop: '6px' }}>
                      ĐÃ ĐƯỢC PHÊ DUYỆT
                    </div>
                    <div style={{ fontSize: '12px', color: '#64748b' }}>Cập nhật lúc: {new Date().toLocaleTimeString('vi-VN')}</div>
                  </div>
                </div>

                {/* Bảng đối chiếu thông tin đã bóc tách OCR */}
                <div style={{ marginBottom: '28px' }}>
                  <h3 style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a', marginBottom: '12px' }}>
                    Kết quả bóc tách dữ liệu CCCD từ FPT.AI (OCR):
                  </h3>
                  <div
                    style={{
                      border: '1px solid #e2e8f0',
                      borderRadius: '10px',
                      overflow: 'hidden',
                      fontSize: '14px',
                    }}
                  >
                    <div style={{ display: 'grid', gridTemplateColumns: '180px 1fr', padding: '12px 16px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <strong style={{ color: '#475569' }}>Số thẻ CCCD:</strong>
                      <span style={{ fontWeight: 700, color: '#0f172a' }}>{result.idCardNumberMasked || personalInfo.idCardNumber}</span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '180px 1fr', padding: '12px 16px', background: '#ffffff', borderBottom: '1px solid #e2e8f0' }}>
                      <strong style={{ color: '#475569' }}>Họ và tên chủ thẻ:</strong>
                      <span style={{ fontWeight: 700, color: '#0f172a' }}>{result.idCardName || personalInfo.fullName}</span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '180px 1fr', padding: '12px 16px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <strong style={{ color: '#475569' }}>Ngày tháng năm sinh:</strong>
                      <span>{result.idCardDob || personalInfo.dob}</span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '180px 1fr', padding: '12px 16px', background: '#ffffff' }}>
                      <strong style={{ color: '#475569' }}>Địa chỉ thường trú:</strong>
                      <span>{result.idCardAddress || personalInfo.address}</span>
                    </div>
                  </div>
                </div>

                {/* Quyền lợi đã mở khóa */}
                <div style={{ background: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: '10px', padding: '16px', marginBottom: '28px' }}>
                  <div style={{ fontWeight: 700, color: '#065f46', fontSize: '14px', marginBottom: '6px' }}>
                    Quyền hạn đã kích hoạt thành công:
                  </div>
                  <ul style={{ margin: 0, paddingLeft: '20px', color: '#047857', fontSize: '13px', lineHeight: 1.6 }}>
                    <li>Được phép đăng tin cho thuê phòng trọ (dành cho Chủ nhà).</li>
                    <li>Được phép gửi yêu cầu thuê phòng và thực hiện hợp đồng đặt cọc an toàn.</li>
                    <li>Huy hiệu chứng nhận <strong>✓ eKYC</strong> hiển thị công khai trên hồ sơ tăng độ tin cậy 100%.</li>
                  </ul>
                </div>

                {/* Nút hành động sau khi thành công */}
                <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
                  <Link to="/post-room" className="btn btn-primary" style={{ padding: '12px 24px', fontSize: '14px' }}>
                    Đăng tin phòng trọ ngay
                  </Link>
                  <Link to="/rooms" className="btn btn-outline" style={{ padding: '12px 24px', fontSize: '14px' }}>
                    Tìm phòng trọ
                  </Link>
                  <Link to="/profile" className="btn btn-outline" style={{ padding: '12px 24px', fontSize: '14px' }}>
                    Xem hồ sơ cá nhân
                  </Link>
                </div>
              </div>
            ) : (
              <div>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                  <div
                    style={{
                      width: '72px',
                      height: '72px',
                      borderRadius: '50%',
                      background: '#fee2e2',
                      color: '#b91c1c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      margin: '0 auto 16px auto',
                    }}
                  >
                    <AlertCircle size={42} />
                  </div>
                  <h2 style={{ fontSize: '24px', fontWeight: 800, color: '#991b1b', margin: '0 0 6px 0' }}>
                    Xác Thực eKYC Chưa Đạt Yêu Cầu
                  </h2>
                  <p style={{ color: '#64748b', fontSize: '14px' }}>
                    Hệ thống không thể cấp chứng nhận do không vượt qua bài kiểm tra đối chiếu an toàn.
                  </p>
                </div>

                <div
                  style={{
                    background: '#fef2f2',
                    border: '1px solid #fecaca',
                    color: '#b91c1c',
                    padding: '16px 20px',
                    borderRadius: '10px',
                    marginBottom: '24px',
                    fontSize: '14px',
                    lineHeight: 1.6,
                  }}
                >
                  <strong>Lý do từ chối:</strong> {errorMessage}
                </div>

                <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '10px', padding: '16px', marginBottom: '28px' }}>
                  <div style={{ fontWeight: 700, color: '#334155', fontSize: '14px', marginBottom: '6px' }}>
                    Gợi ý khắc phục khi chụp lại:
                  </div>
                  <ul style={{ margin: 0, paddingLeft: '20px', color: '#64748b', fontSize: '13px', lineHeight: 1.6 }}>
                    <li>Đảm bảo nơi chụp có đủ ánh sáng tự nhiên, không bị bóng mờ che khuất khuôn mặt.</li>
                    <li>Căn chỉnh toàn bộ 4 góc của thẻ CCCD nằm gọn bên trong khung hướng dẫn.</li>
                    <li>Khi chụp selfie, nhìn thẳng trực diện vào ống kính, không đeo kính râm hay khẩu trang.</li>
                  </ul>
                </div>

                <div style={{ textAlign: 'center' }}>
                  <button
                    type="button"
                    onClick={handleResetFlow}
                    className="btn btn-primary"
                    style={{ padding: '12px 28px', fontSize: '15px', fontWeight: 700 }}
                  >
                    🔄 Thực hiện lại quy trình từ đầu
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
