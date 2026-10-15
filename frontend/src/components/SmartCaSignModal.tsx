import { useState, useEffect, useRef } from 'react';
import {
  ShieldCheck,
  Smartphone,
  KeyRound,
  FileCheck2,
  Copy,
  Check,
  AlertCircle,
  Loader2,
  X,
  Sparkles
} from 'lucide-react';
import type { Contract } from '../types/contract';
import type { SmartCaInitiateResponse, SmartCaStatusResponse } from '../types/smartca';
import { smartcaService } from '../services/smartcaService';
import './SmartCaSignModal.css';

interface SmartCaSignModalProps {
  contract: Contract;
  userCccd?: string;
  userName?: string;
  userRole: 'TENANT' | 'LANDLORD';
  onSuccess: (updatedContract: Contract) => void;
  onClose: () => void;
}

export default function SmartCaSignModal({
  contract,
  userCccd = '',
  userName = '',
  userRole,
  onSuccess,
  onClose,
}: SmartCaSignModalProps) {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [customCccd, setCustomCccd] = useState(userCccd || '');

  const [initData, setInitData] = useState<SmartCaInitiateResponse | null>(null);
  const [statusData, setStatusData] = useState<SmartCaStatusResponse | null>(null);

  const [activeTab, setActiveTab] = useState<'app' | 'pin'>('app');
  const [pin, setPin] = useState('');
  const [copiedHash, setCopiedHash] = useState(false);

  const pollIntervalRef = useRef<number | null>(null);

  // Clear polling interval on unmount
  useEffect(() => {
    return () => {
      if (pollIntervalRef.current) {
        window.clearInterval(pollIntervalRef.current);
      }
    };
  }, []);

  // Initiate signing request
  const handleInitiate = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await smartcaService.initiate(contract.id, {
        idCardNumber: customCccd.trim() || undefined,
      });
      setInitData(res);
      setStep(2);
      startPolling(res.transactionId);
    } catch (err: any) {
      setError(err?.message || 'Không thể kết nối dịch vụ VNPT SmartCA. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  // Start polling SmartCA status
  const startPolling = (transactionId: string) => {
    if (pollIntervalRef.current) {
      window.clearInterval(pollIntervalRef.current);
    }

    pollIntervalRef.current = window.setInterval(async () => {
      try {
        const res = await smartcaService.getStatus(contract.id, transactionId);
        if (res.status === 'SIGNED') {
          if (pollIntervalRef.current) {
            window.clearInterval(pollIntervalRef.current);
          }
          setStatusData(res);
          setStep(3);
          if (res.contract) {
            onSuccess(res.contract);
          }
        }
      } catch (e) {
        // Polling error silently tolerated
      }
    }, 3500);
  };

  // Confirm via PIN
  const handleConfirmPin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!initData) return;
    setLoading(true);
    setError(null);

    try {
      const res = await smartcaService.confirm(contract.id, {
        transactionId: initData.transactionId,
        pin: pin.trim(),
        sandboxConfirm: false,
      });

      if (res.status === 'SIGNED') {
        if (pollIntervalRef.current) window.clearInterval(pollIntervalRef.current);
        setStatusData(res);
        setStep(3);
        if (res.contract) onSuccess(res.contract);
      } else {
        setError(res.message || 'Mã PIN hoặc mật khẩu SmartCA không chính xác.');
      }
    } catch (err: any) {
      setError(err?.message || 'Xác nhận ký số không thành công. Vui lòng kiểm tra lại PIN.');
    } finally {
      setLoading(false);
    }
  };

  // Sandbox / Demo confirm (for examiners & testers without physical VNPT SIM)
  const handleSandboxConfirm = async () => {
    if (!initData) return;
    setLoading(true);
    setError(null);

    try {
      const res = await smartcaService.confirm(contract.id, {
        transactionId: initData.transactionId,
        sandboxConfirm: true,
      });

      if (res.status === 'SIGNED') {
        if (pollIntervalRef.current) window.clearInterval(pollIntervalRef.current);
        setStatusData(res);
        setStep(3);
        if (res.contract) onSuccess(res.contract);
      } else {
        setError(res.message || 'Lỗi xác thực Sandbox.');
      }
    } catch (err: any) {
      setError(err?.message || 'Không thể hoàn tất ký Sandbox.');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedHash(true);
    setTimeout(() => setCopiedHash(false), 2000);
  };

  return (
    <div className="smartca-backdrop" onClick={onClose}>
      <div className="smartca-modal" onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="smartca-header">
          <div className="smartca-brand">
            <div className="smartca-logo-badge">
              <ShieldCheck size={26} color="#ffffff" />
            </div>
            <div className="smartca-title-group">
              <h3>Ký số từ xa VNPT SmartCA</h3>
              <span>Chứng thực chữ ký điện tử Cloud Signing theo chuẩn VNPT-CA</span>
            </div>
          </div>
          <button className="smartca-close-btn" onClick={onClose} disabled={loading}>
            <X size={18} />
          </button>
        </div>

        {/* Steps */}
        <div className="smartca-body">
          <div className="smartca-steps">
            <div className={`smartca-step-item ${step === 1 ? 'active' : ''} ${step > 1 ? 'completed' : ''}`}>
              <div className="smartca-step-num">{step > 1 ? <Check size={16} /> : 1}</div>
              <span className="smartca-step-label">Khởi tạo</span>
            </div>
            <div className={`smartca-step-item ${step === 2 ? 'active' : ''} ${step > 2 ? 'completed' : ''}`}>
              <div className="smartca-step-num">{step > 2 ? <Check size={16} /> : 2}</div>
              <span className="smartca-step-label">Xác nhận ký</span>
            </div>
            <div className={`smartca-step-item ${step === 3 ? 'completed' : ''}`}>
              <div className="smartca-step-num">{step === 3 ? <Check size={16} /> : 3}</div>
              <span className="smartca-step-label">Hoàn tất</span>
            </div>
          </div>

          {error && (
            <div style={{
              background: '#fef2f2',
              border: '1px solid #fecaca',
              color: '#b91c1c',
              padding: '12px 14px',
              borderRadius: '8px',
              fontSize: '13px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          {/* STEP 1: Verify identity & initiate */}
          {step === 1 && (
            <>
              <div className="smartca-info-card">
                <div className="smartca-info-row">
                  <span className="smartca-info-label">Mã hợp đồng:</span>
                  <span className="smartca-info-value">{contract.contractCode || contract.id.slice(0, 8)}</span>
                </div>
                <div className="smartca-info-row">
                  <span className="smartca-info-label">Người ký:</span>
                  <span className="smartca-info-value">{userName || 'Chưa cập nhật'}</span>
                </div>
                <div className="smartca-info-row">
                  <span className="smartca-info-label">Vai trò:</span>
                  <span className="smartca-info-value">
                    {userRole === 'TENANT' ? 'Bên thuê (Tenant)' : 'Bên cho thuê (Landlord)'}
                  </span>
                </div>
                <div className="smartca-info-row">
                  <span className="smartca-info-label">Đơn vị chứng thực:</span>
                  <span className="smartca-info-value">VNPT-CA Remote Signing Service</span>
                </div>
              </div>

              <div className="smartca-input-group">
                <label>Số CCCD / CMND đăng ký chữ ký số SmartCA:</label>
                <input
                  type="text"
                  className="smartca-input"
                  value={customCccd}
                  onChange={e => setCustomCccd(e.target.value)}
                  placeholder="Ví dụ: 0131930813216"
                  disabled={loading}
                />
                <span style={{ fontSize: '11px', color: '#64748b' }}>
                  * Hệ thống sẽ tự động đối chiếu chứng thư số VNPT SmartCA theo số CCCD này.
                </span>
              </div>
            </>
          )}

          {/* STEP 2: Authorization / Polling / PIN */}
          {step === 2 && initData && (
            <>
              <div className="smartca-info-card">
                <div className="smartca-info-row">
                  <span className="smartca-info-label">Mã giao dịch SmartCA:</span>
                  <strong style={{ color: '#0066cc', fontFamily: 'monospace' }}>{initData.transactionId}</strong>
                </div>
                <div className="smartca-info-row">
                  <span className="smartca-info-label">Mã băm SHA-256 tài liệu:</span>
                  <button
                    type="button"
                    onClick={() => copyToClipboard(initData.dataHash)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: '#0066cc', fontSize: '12px' }}
                  >
                    {copiedHash ? <Check size={14} color="#16a34a" /> : <Copy size={14} />}
                    {copiedHash ? 'Đã sao chép' : 'Sao chép'}
                  </button>
                </div>
                <div className="smartca-hash-box">
                  {initData.dataHash}
                </div>
              </div>

              {/* Tabs: Mobile App vs Direct PIN */}
              <div className="smartca-tabs">
                <button
                  type="button"
                  className={`smartca-tab-btn ${activeTab === 'app' ? 'active' : ''}`}
                  onClick={() => setActiveTab('app')}
                >
                  <Smartphone size={15} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
                  Xác nhận qua App VNPT SmartCA
                </button>
                <button
                  type="button"
                  className={`smartca-tab-btn ${activeTab === 'pin' ? 'active' : ''}`}
                  onClick={() => setActiveTab('pin')}
                >
                  <KeyRound size={15} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
                  Xác nhận bằng mã PIN / Mật khẩu
                </button>
              </div>

              {activeTab === 'app' && (
                <div className="smartca-radar-container">
                  <div className="smartca-radar">
                    <Smartphone size={32} />
                  </div>
                  <div className="smartca-radar-text">
                    <h4>Đang chờ bạn phê duyệt trên ứng dụng VNPT SmartCA</h4>
                    <p>
                      Vui lòng mở ứng dụng <strong>VNPT SmartCA</strong> trên điện thoại, kiểm tra thông báo yêu cầu ký số cho giao dịch <code>{initData.transactionId}</code> và bấm <strong>Đồng ý ký</strong>.
                    </p>
                  </div>
                </div>
              )}

              {activeTab === 'pin' && (
                <form onSubmit={handleConfirmPin} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  <div className="smartca-input-group">
                    <label>Nhập mã PIN / Mật khẩu ký số SmartCA:</label>
                    <input
                      type="password"
                      className="smartca-input"
                      value={pin}
                      onChange={e => setPin(e.target.value)}
                      placeholder="Nhập 4-8 ký tự PIN"
                      autoFocus
                      required
                    />
                  </div>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={loading || !pin.trim()}
                    style={{ padding: '10px 16px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
                  >
                    {loading ? <Loader2 size={16} className="animate-spin" /> : <KeyRound size={16} />}
                    Xác nhận ký số với PIN
                  </button>
                </form>
              )}
            </>
          )}

          {/* STEP 3: Complete / Success */}
          {step === 3 && statusData && (
            <div className="smartca-success-card">
              <div className="smartca-success-icon">
                <FileCheck2 size={32} />
              </div>
              <h3 className="smartca-success-title">Ký số điện tử VNPT SmartCA thành công!</h3>
              <p style={{ margin: 0, fontSize: '13px', color: '#15803d' }}>
                Chữ ký số hợp lệ đã được đính kèm và niêm phong toàn vẹn vào hợp đồng.
              </p>

              <div className="smartca-stamp-preview">
                <div><strong>Chứng thư số (Serial):</strong> {statusData.certSerial || 'VNPT-CA-AUTO'}</div>
                <div><strong>Chủ thể:</strong> {statusData.certSubject || userName}</div>
                <div><strong>Tổ chức chứng thực:</strong> VNPT-CA Remote Signing Sub-CA</div>
                <div><strong>Thời gian ký:</strong> {new Date().toLocaleString('vi-VN')}</div>
                <div style={{ wordBreak: 'break-all', marginTop: '4px' }}>
                  <strong>Mã xác thực chữ ký:</strong> <code style={{ fontSize: '11px', color: '#0066cc' }}>{statusData.signatureValue}</code>
                </div>
                {statusData.contractCompleted && (
                  <div style={{ marginTop: '8px', padding: '6px 10px', background: '#dcfce7', borderRadius: '6px', color: '#166534', fontWeight: 600 }}>
                    🎉 Cả hai bên đã hoàn tất ký số! Hợp đồng chính thức có hiệu lực (ACTIVE).
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="smartca-footer">
          <div>
            {step === 2 && (
              <button
                type="button"
                className="btn btn-outline"
                style={{
                  borderColor: '#f59e0b',
                  color: '#b45309',
                  background: '#fffbeb',
                  fontSize: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
                onClick={handleSandboxConfirm}
                disabled={loading}
                title="Sử dụng khi thử nghiệm chấm điểm đồ án mà không có SIM/app VNPT SmartCA thật"
              >
                <Sparkles size={14} color="#d97706" />
                Hoàn tất ký số Sandbox / Demo
              </button>
            )}
          </div>

          <div className="smartca-btn-group">
            {step === 1 && (
              <>
                <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>
                  Hủy
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleInitiate}
                  disabled={loading}
                  style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
                >
                  {loading ? <Loader2 size={16} className="animate-spin" /> : <ShieldCheck size={16} />}
                  Tạo yêu cầu ký số
                </button>
              </>
            )}

            {step === 2 && (
              <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>
                Đóng
              </button>
            )}

            {step === 3 && (
              <button type="button" className="btn btn-primary" onClick={onClose}>
                Đóng & Xem hợp đồng
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
