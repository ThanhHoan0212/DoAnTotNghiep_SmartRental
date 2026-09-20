import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  FileText,
  Calendar,
  User,
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ExternalLink,
  ShieldCheck,
  Building,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { contractService } from '../services/contractService';
import ContractDetailDialog from '../components/ContractDetailDialog';
import type { Contract, ContractStatus } from '../types/contract';

export default function ContractsPage() {
  const { user } = useAuth();

  // Active Tab: 'tenant' (Hợp đồng thuê của tôi) hoặc 'landlord' (Quản lý khách thuê)
  const isLandlordOrAdmin = user?.role === 'LANDLORD' || user?.role === 'ADMIN';
  const [activeTab, setActiveTab] = useState<'tenant' | 'landlord' | 'admin'>(
    user?.role === 'ADMIN' ? 'admin' : isLandlordOrAdmin ? 'landlord' : 'tenant'
  );

  const [contracts, setContracts] = useState<Contract[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const loadSequence = useRef(0);

  // Pagination
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Filter status
  const [selectedStatus, setSelectedStatus] = useState<ContractStatus | 'ALL'>('ALL');

  // Dialog cập nhật trạng thái (Từ chối / Hủy)
  const [actionTarget, setActionTarget] = useState<{
    contract: Contract;
    targetStatus: ContractStatus;
    title: string;
  } | null>(null);
  const [actionReason, setActionReason] = useState<string>('');
  const [isProcessingAction, setIsProcessingAction] = useState<boolean>(false);

  const [now, setNow] = useState(() => Date.now());
  const [notice, setNotice] = useState<string | null>(null);
  const loadContracts = useCallback(async (background = false) => {
    const sequence = ++loadSequence.current;
    if (!background) setIsLoading(true);
    setError(null);
    try {
      let res;
      const status = selectedStatus === 'ALL' ? undefined : selectedStatus;
      if (activeTab === 'tenant') {
        res = await contractService.getMyTenantContracts(page, 10, status);
      } else if (activeTab === 'admin') {
        res = await contractService.getAllContractsAdmin(page, 10, status);
      } else {
        res = await contractService.getMyLandlordContracts(page, 10, status);
      }
      if (sequence !== loadSequence.current) return;
      if (page > 0 && page >= res.totalPages) {
        setPage(Math.max(0, res.totalPages - 1));
        return;
      }
      setNow(Date.now());
      setContracts(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: unknown) {
      if (sequence !== loadSequence.current) return;
      console.error('Lỗi tải danh sách hợp đồng:', err);
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách hợp đồng.');
    } finally {
      if (sequence === loadSequence.current) setIsLoading(false);
    }
  }, [activeTab, page, selectedStatus]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadContracts(), 0);
    return () => { window.clearTimeout(timer); loadSequence.current++; };
  }, [loadContracts]);

  const filteredContracts = contracts;
  const [documentTarget, setDocumentTarget] = useState<Contract | null>(null);
  const [acceptedDocument, setAcceptedDocument] = useState(false);

  const detailId = documentTarget?.id;
  useEffect(() => {
    let disposed = false;
    let running = false;
    const refresh = async () => {
      if (document.hidden || running || isProcessingAction) return;
      running = true;
      try {
        await Promise.all([loadContracts(true), detailId ? contractService.getContractDetail(detailId).then(detail => {
          if (!disposed) setDocumentTarget(current => current?.id === detail.id ? detail : current);
        }) : Promise.resolve()]);
      } catch (err) {
        if (!disposed) setError(err instanceof Error ? err.message : 'Không thể đồng bộ chi tiết hợp đồng.');
      } finally { running = false; }
    };
    const initial = window.setTimeout(() => void refresh(), 0);
    const timer = window.setInterval(() => void refresh(), 5000);
    window.addEventListener('focus', refresh);
    window.addEventListener('online', refresh);
    document.addEventListener('visibilitychange', refresh);
    return () => {
      disposed = true;
      window.clearTimeout(initial);
      window.clearInterval(timer);
      window.removeEventListener('focus', refresh);
      window.removeEventListener('online', refresh);
      document.removeEventListener('visibilitychange', refresh);
    };
  }, [loadContracts, detailId, isProcessingAction]);

  const applyUpdate = (updated: Contract) => {
    loadSequence.current++;
    setContracts(current => current.map(item => item.id === updated.id ? updated : item));
    setDocumentTarget(current => current?.id === updated.id ? updated : current);
    setAcceptedDocument(false);
  };

  const handleSimulation = async (contract: Contract, action: 'deposit' | 'signature') => {
    if (action === 'deposit' && !window.confirm(`Xác nhận thanh toán cọc giả lập ${contract.depositAmount.toLocaleString('vi-VN')} đ? Không có tiền thật được chuyển.`)) return;
    setIsProcessingAction(true);
    try {
      const updated = action === 'deposit' ? await contractService.simulateDeposit(contract.id) : await contractService.simulateSignature(contract.id);
      applyUpdate(updated);
      setNotice(action === 'deposit' ? 'Đã thanh toán cọc giả lập. Hợp đồng đã sẵn sàng để ký.' : 'Đã ghi nhận chữ ký giả lập của bạn.');
      await loadContracts(true);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Không thể thực hiện thao tác giả lập.');
      await loadContracts();
    } finally {
      setIsProcessingAction(false);
    }
  };

  // Xử lý Phê duyệt hợp đồng (Landlord)
  const handleApprove = async (contract: Contract) => {
    const code = contract.contractCode || contract.contractNumber || contract.id.slice(0, 8);
    if (
      !window.confirm(
        `Xác nhận phê duyệt yêu cầu thuê phòng #${code}? Phòng sẽ được giữ chỗ trong 24 giờ để người thuê thanh toán cọc giả lập.`
      )
    ) {
      return;
    }

    try {
      setIsProcessingAction(true);
      const updated = await contractService.updateContractStatus(contract.id, {
        status: 'AWAITING_DEPOSIT',
      });
      applyUpdate(updated);
      setNotice('Đã chấp nhận yêu cầu và mở thanh toán cọc giả lập.');
      await loadContracts(true);
    } catch (err: unknown) {
      console.error('Lỗi phê duyệt hợp đồng:', err);
      alert(err instanceof Error ? err.message : 'Có lỗi xảy ra khi phê duyệt hợp đồng.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  // Xử lý Mở modal Từ chối / Hủy / Chấm dứt
  const handleOpenActionModal = (
    contract: Contract,
    targetStatus: ContractStatus,
    title: string
  ) => {
    setActionTarget({ contract, targetStatus, title });
    setActionReason('');
  };

  // Xác nhận thực thi Từ chối / Hủy / Chấm dứt
  const handleConfirmAction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!actionTarget || !actionReason.trim()) return;

    try {
      setIsProcessingAction(true);
      const updated = await contractService.updateContractStatus(actionTarget.contract.id, {
        status: actionTarget.targetStatus,
        reason: actionReason.trim() || undefined,
      });
      applyUpdate(updated);
      setNotice(`Thao tác thành công: ${actionTarget.title}`);
      setActionTarget(null);
      await loadContracts(true);
    } catch (err: unknown) {
      console.error('Lỗi xử lý hợp đồng:', err);
      alert(err instanceof Error ? err.message : 'Có lỗi xảy ra.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  // Helper render status badge
  const renderStatusBadge = (status: ContractStatus) => {
    switch (status) {
      case 'PENDING':
        return <span className="status-badge status-pending"><Clock size={13} /> Chờ duyệt</span>;
      case 'AWAITING_DEPOSIT':
        return <span className="status-badge status-pending">Chờ thanh toán cọc</span>;
      case 'AWAITING_SIGNATURES':
        return <span className="status-badge status-pending">Chờ hai bên ký</span>;
      case 'ACTIVE':
        return <span className="status-badge status-active"><CheckCircle2 size={13} /> Đang hiệu lực</span>;
      case 'REJECTED':
        return <span className="status-badge status-rejected"><XCircle size={13} /> Bị từ chối</span>;
      case 'EXPIRED':
        return <span className="status-badge status-expired"><Clock size={13} /> Đã hết hạn</span>;
      case 'TERMINATED':
        return <span className="status-badge status-terminated"><AlertTriangle size={13} /> Đã chấm dứt</span>;
      case 'CANCELLED':
        return <span className="status-badge status-cancelled"><XCircle size={13} /> Đã hủy</span>;
      default:
        return <span className="status-badge">{status}</span>;
    }
  };

  return (
    <main className="page" style={{ background: '#f8fafc', minHeight: 'calc(100vh - 72px)', padding: '32px 0 60px' }}>
      <div className="container">
        {/* Page Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h1 style={{ margin: '0 0 6px', fontSize: '26px', color: '#0f172a', fontWeight: 800 }}>
              Quản lý hợp đồng thuê phòng
            </h1>
            <p style={{ margin: 0, color: '#64748b', fontSize: '14px' }}>
              Yêu cầu thuê → Chấp nhận → Thanh toán cọc → Tạo hợp đồng → Hai bên ký → Có hiệu lực. Thanh toán và chữ ký đều là giả lập.
            </p>
          </div>

          <Link to="/rooms" className="btn btn-outline" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Building size={16} />
            Tìm thêm phòng mới
          </Link>
        </div>

        <p style={{ fontSize: 12, color: '#64748b' }}>Tự động đồng bộ mỗi 5 giây và khi quay lại trang.</p>
        {notice && <div role="status" style={{ padding: 14, background: '#ecfdf5', color: '#065f46', borderRadius: 10, marginBottom: 16 }}>{notice}</div>}
        {/* Tabs: Tenant vs Landlord */}
        <div className="contract-tabs">
          {user?.role === 'ADMIN' && (
            <button type="button" className={`contract-tab-btn ${activeTab === 'admin' ? 'active' : ''}`}
              onClick={() => { setActiveTab('admin'); setPage(0); }}>
              <ShieldCheck size={17} /> Toàn bộ hợp đồng
            </button>
          )}
          <button
            type="button"
            className={`contract-tab-btn ${activeTab === 'tenant' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('tenant');
              setPage(0);
            }}
          >
            <User size={17} />
            Hợp đồng tôi thuê (Tenant)
            {activeTab === 'tenant' && totalElements > 0 && (
              <span className="tab-count">{totalElements}</span>
            )}
          </button>

          {isLandlordOrAdmin && (
            <button
              type="button"
              className={`contract-tab-btn ${activeTab === 'landlord' ? 'active' : ''}`}
              onClick={() => {
                setActiveTab('landlord');
                setPage(0);
              }}
            >
              <ShieldCheck size={17} />
              Quản lý cho thuê (Chủ nhà)
              {activeTab === 'landlord' && totalElements > 0 && (
                <span className="tab-count">{totalElements}</span>
              )}
            </button>
          )}
        </div>

        {/* Filter Status Bar */}
        <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', flexWrap: 'wrap', alignItems: 'center' }}>
          <span style={{ fontSize: '13px', fontWeight: 600, color: '#475569', marginRight: '8px' }}>
            Lọc trạng thái:
          </span>
          {[
            { key: 'ALL', label: 'Tất cả' },
            { key: 'PENDING', label: 'Chờ duyệt' },
            { key: 'AWAITING_DEPOSIT', label: 'Chờ cọc' },
            { key: 'AWAITING_SIGNATURES', label: 'Chờ ký' },
            { key: 'ACTIVE', label: 'Đang hiệu lực' },
            { key: 'EXPIRED', label: 'Đã hết hạn' },
            { key: 'REJECTED', label: 'Bị từ chối' },
            { key: 'TERMINATED', label: 'Đã chấm dứt' },
            { key: 'CANCELLED', label: 'Đã hủy' },
          ].map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => { setSelectedStatus(item.key as ContractStatus | 'ALL'); setPage(0); }}
              style={{
                padding: '6px 12px',
                borderRadius: '9999px',
                fontSize: '12px',
                fontWeight: 600,
                border: selectedStatus === item.key ? '1px solid #167c5a' : '1px solid #cbd5e1',
                background: selectedStatus === item.key ? '#ecfdf5' : '#ffffff',
                color: selectedStatus === item.key ? '#065f46' : '#475569',
                cursor: 'pointer',
                transition: 'all 0.15s',
              }}
            >
              {item.label}
            </button>
          ))}
        </div>

        {/* Loading State */}
        {isLoading && (
          <div style={{ textAlign: 'center', padding: '60px 0', color: '#64748b' }}>
            <p>Đang tải danh sách hợp đồng...</p>
          </div>
        )}

        {/* Error State */}
        {error && (
          <div
            style={{
              background: '#fef2f2',
              color: '#b91c1c',
              border: '1px solid #fee2e2',
              padding: '16px',
              borderRadius: '10px',
              marginBottom: '20px',
            }}
          >
            {error}
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !error && filteredContracts.length === 0 && (
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              padding: '60px 24px',
              textAlign: 'center',
            }}
          >
            <FileText size={48} color="#94a3b8" style={{ marginBottom: '16px' }} />
            <h3 style={{ margin: '0 0 8px', color: '#1e293b' }}>
              {selectedStatus === 'ALL'
                ? activeTab === 'tenant'
                  ? 'Bạn chưa có hợp đồng thuê phòng nào'
                  : 'Chưa có yêu cầu thuê phòng nào gửi tới bạn'
                : 'Không có hợp đồng nào với trạng thái đã chọn'}
            </h3>
            <p style={{ color: '#64748b', fontSize: '14px', maxWidth: '460px', margin: '0 auto 20px' }}>
              {activeTab === 'tenant'
                ? 'Hãy khám phá danh sách phòng trọ đã được kiểm duyệt tại TP. Hồ Chí Minh và gửi yêu cầu thuê ngay.'
                : 'Các yêu cầu đặt cọc & thuê phòng từ khách hàng sẽ hiển thị tại đây khi có người quan tâm.'}
            </p>
            {activeTab === 'tenant' && (
              <Link to="/rooms" className="btn btn-primary">
                Khám phá phòng trọ ngay
              </Link>
            )}
          </div>
        )}

        {/* Contracts List */}
        {!isLoading && filteredContracts.length > 0 && (
          <div className="contracts-list">
            {filteredContracts.map((contract) => {
              const code = contract.contractCode || contract.contractNumber || `HD-${contract.id.slice(0, 8)}`;
              const roomId = contract.roomId || contract.room?.id;
              const roomTitle = contract.roomTitle || contract.room?.title || 'Phòng trọ';
              const roomAddress = contract.roomAddress || contract.room?.address || '';
              const roomDistrict = contract.roomDistrict || contract.room?.district || '';
              const fullAddress = [roomAddress, roomDistrict].filter(Boolean).join(', ');

              const isTenantTab = activeTab === 'tenant';
              const otherPartyRole = isTenantTab ? 'Chủ nhà' : 'Người thuê';
              const otherPartyName = isTenantTab
                ? (contract.landlordName || contract.landlord?.fullName || 'Chủ nhà')
                : (contract.tenantName || contract.tenant?.fullName || 'Người thuê');
              const otherPartyPhone = isTenantTab
                ? (contract.landlordPhone || contract.landlord?.phoneNumber)
                : (contract.tenantPhone || contract.tenant?.phoneNumber);
              const otherPartyEmail = isTenantTab
                ? (contract.landlordEmail || contract.landlord?.email)
                : (contract.tenantEmail || contract.tenant?.email);

              return (
                <div key={contract.id} className="contract-card">
                  {/* Header: Contract Number & Status */}
                  <div className="contract-card-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                      <span className="contract-code">{code}</span>
                      {renderStatusBadge(contract.status)}
                      <span style={{ fontSize: '12px', color: '#64748b' }}>
                        Tạo ngày {new Date(contract.createdAt).toLocaleDateString('vi-VN')}
                      </span>
                    </div>

                    {roomId && (
                      <Link
                        to={`/rooms/${roomId}`}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          fontSize: '13px',
                          color: '#167c5a',
                          fontWeight: 600,
                        }}
                      >
                        Xem tin phòng <ExternalLink size={14} />
                      </Link>
                    )}
                  </div>

                  {/* Body: Room, Opposite Party, Pricing */}
                  <div className="contract-card-body">
                    {/* Cột trái: Thông tin phòng & Đối tác */}
                    <div>
                      <h4 style={{ margin: '0 0 6px', fontSize: '16px', color: '#0f172a' }}>
                        {roomTitle}
                      </h4>
                      {fullAddress && (
                        <p style={{ margin: '0 0 14px', fontSize: '13px', color: '#64748b' }}>
                          {fullAddress}
                        </p>
                      )}

                      {/* Đối tác (Landlord hoặc Tenant) */}
                      {activeTab === 'admin' && (
                        <p>Chủ nhà: <strong>{contract.landlordName || contract.landlord?.fullName}</strong></p>
                      )}
                      <div className="contract-parties">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span style={{ color: '#64748b' }}>{otherPartyRole}:</span>
                          <strong style={{ color: '#0f172a' }}>{otherPartyName}</strong>
                        </div>
                        {otherPartyPhone && (
                          <div style={{ color: '#475569' }}>
                            Số điện thoại: <strong>{otherPartyPhone}</strong>
                          </div>
                        )}
                        {otherPartyEmail && (
                          <div style={{ color: '#475569' }}>
                            Email: {otherPartyEmail}
                          </div>
                        )}
                      </div>

                      {/* Ghi chú hoặc lý do hủy nếu có */}
                      {contract.terms && (
                        <div style={{ marginTop: '12px', fontSize: '12px', background: '#f8fafc', padding: '8px 12px', borderRadius: '6px', border: '1px solid #f1f5f9' }}>
                          <strong style={{ color: '#334155' }}>Ghi chú gửi kèm:</strong> {contract.terms}
                        </div>
                      )}

                      {contract.cancellationReason && (
                        <div style={{ marginTop: '10px', fontSize: '12px', background: '#fef2f2', color: '#b91c1c', padding: '8px 12px', borderRadius: '6px', border: '1px solid #fee2e2' }}>
                          <strong>Lý do từ chối/hủy:</strong> {contract.cancellationReason}
                        </div>
                      )}
                    </div>

                    {/* Cột phải: Thời hạn & Tài chính */}
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '12px', fontSize: '13px', color: '#334155' }}>
                        <Calendar size={15} color="#167c5a" />
                        <span>Thời hạn thuê: <strong>{contract.startDate}</strong> đến <strong>{contract.endDate}</strong></span>
                      </div>

                      <div className="contract-pricing">
                        <div className="price-row">
                          <span>Tiền thuê hàng tháng:</span>
                          <strong>{contract.monthlyRent?.toLocaleString('vi-VN')} đ/tháng</strong>
                        </div>
                        <div className="price-row">
                          <span>Tiền cọc thỏa thuận:</span>
                          <strong>{contract.depositAmount?.toLocaleString('vi-VN')} đ</strong>
                        </div>
                        <div className="price-row">
                          <span>Tổng ban đầu:</span>
                          <span>{(contract.monthlyRent + contract.depositAmount)?.toLocaleString('vi-VN')} đ</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div style={{ padding: '12px 20px', background: '#f8fafc', fontSize: '13px' }}>
                    {contract.requestCode && <p>Mã yêu cầu: {contract.requestCode}</p>}
                    {contract.status === 'AWAITING_DEPOSIT' && contract.depositDeadline && <p>Hạn thanh toán cọc: {new Date(contract.depositDeadline).toLocaleString('vi-VN')}. Hết hạn sẽ giải phóng phòng.</p>}
                    {contract.depositPaidAt && <p>Đã thanh toán giả lập: {new Date(contract.depositPaidAt).toLocaleString('vi-VN')} · {contract.paymentReference}</p>}
                    {contract.formalizedAt && <p>Hợp đồng được tạo: {new Date(contract.formalizedAt).toLocaleString('vi-VN')}</p>}
                    {contract.documentContent && <p>Người thuê: {contract.tenantSignedAt ? 'Đã ký giả lập' : 'Chưa ký'} · Chủ phòng: {contract.landlordSignedAt ? 'Đã ký giả lập' : 'Chưa ký'}</p>}
                    {contract.closureRequests?.some(r => r.status === 'PENDING') && <p style={{ color: '#b45309', fontWeight: 600 }}>Có yêu cầu hủy / chấm dứt đang chờ bên còn lại xác nhận. Xem chi tiết để phản hồi.</p>}
                    {contract.agreedEndDate && <p>Ngày chấm dứt đã thống nhất: <strong>{new Date(contract.agreedEndDate).toLocaleDateString('vi-VN')}</strong>{contract.status === 'ACTIVE' ? ' · Hợp đồng vẫn đang có hiệu lực.' : ''}</p>}
                    {contract.activatedAt && <p>Có hiệu lực từ: {new Date(contract.activatedAt).toLocaleString('vi-VN')}</p>}
                  </div>
                  {/* Actions Bar */}
                  <div className="contract-actions">
                    {contract.status === 'AWAITING_DEPOSIT' && user?.id === contract.tenantId && (
                      <button className="btn btn-primary" disabled={isProcessingAction || Boolean(contract.depositDeadline && new Date(contract.depositDeadline).getTime() <= now)}
                        onClick={() => handleSimulation(contract, 'deposit')}>Thanh toán cọc (giả lập)</button>
                    )}
                    {(
                      <button className="btn btn-outline" disabled={isProcessingAction}
                        onClick={() => { setDocumentTarget(contract); setAcceptedDocument(false); }}>Xem chi tiết hợp đồng</button>
                    )}
                    {/* Đối với Chủ nhà khi có yêu cầu PENDING */}
                    {(activeTab === 'landlord' || activeTab === 'admin') && contract.status === 'PENDING' && (
                      <>
                        <button
                          type="button"
                          className="btn btn-outline"
                          style={{ borderColor: '#ef4444', color: '#ef4444', fontSize: '13px', padding: '6px 14px' }}
                          onClick={() => handleOpenActionModal(contract, 'REJECTED', 'Từ chối yêu cầu thuê phòng')}
                          disabled={isProcessingAction}
                        >
                          <XCircle size={15} style={{ marginRight: '4px', verticalAlign: 'middle' }} />
                          Từ chối
                        </button>
                        <button
                          type="button"
                          className="btn btn-primary"
                          style={{ fontSize: '13px', padding: '6px 16px' }}
                          onClick={() => handleApprove(contract)}
                          disabled={isProcessingAction}
                        >
                          <CheckCircle2 size={15} style={{ marginRight: '4px', verticalAlign: 'middle' }} />
                          Chấp nhận & mở đặt cọc
                        </button>
                      </>
                    )}

                    {/* Đối với Người thuê khi đơn còn PENDING */}
                    {activeTab === 'tenant' && !contract.depositPaidAt && (contract.status === 'PENDING' || contract.status === 'AWAITING_DEPOSIT') && (
                      <button
                        type="button"
                        className="btn btn-outline"
                        style={{ borderColor: '#ef4444', color: '#ef4444', fontSize: '13px', padding: '6px 14px' }}
                        onClick={() => handleOpenActionModal(contract, 'CANCELLED', 'Hủy yêu cầu thuê phòng')}
                        disabled={isProcessingAction}
                      >
                        <XCircle size={15} style={{ marginRight: '4px', verticalAlign: 'middle' }} />
                        Hủy yêu cầu
                      </button>
                    )}

                    {/* Đối với Hợp đồng đang ACTIVE: Cho phép thanh lý / chấm dứt */}
                    {(contract.status === 'ACTIVE' || (contract.status === 'AWAITING_SIGNATURES' && contract.depositPaidAt)) && (user?.id === contract.tenantId || user?.id === contract.landlordId) && (
                      <button
                        type="button"
                        className="btn btn-outline"
                        style={{ fontSize: '13px', padding: '6px 14px' }}
                        onClick={() => { setDocumentTarget(contract); setAcceptedDocument(false); }}
                        disabled={isProcessingAction}
                      >
                        <AlertTriangle size={15} style={{ marginRight: '4px', verticalAlign: 'middle' }} />
                        {contract.closureRequests?.some(r => r.status === 'PENDING' || r.status === 'ACCEPTED') ? 'Xem yêu cầu hủy / chấm dứt' : contract.status === 'ACTIVE' ? 'Yêu cầu chấm dứt hợp đồng' : 'Yêu cầu hủy sau đặt cọc'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Phân trang */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '30px' }}>
            <button
              type="button"
              className="btn btn-outline"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Trang trước
            </button>
            <span style={{ display: 'flex', alignItems: 'center', padding: '0 12px', fontSize: '14px', color: '#475569' }}>
              Trang {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              className="btn btn-outline"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Trang sau
            </button>
          </div>
        )}

        {documentTarget && (
          <ContractDetailDialog contract={documentTarget} userId={user?.id}
            onBusy={setIsProcessingAction} onUpdated={applyUpdate}
            busy={isProcessingAction} accepted={acceptedDocument} onAccept={setAcceptedDocument}
            onClose={() => { setDocumentTarget(null); setAcceptedDocument(false); }}
            onSign={() => handleSimulation(documentTarget, 'signature')}
            statusBadge={renderStatusBadge(documentTarget.status)} />
        )}
        {/* Modal nhập lý do Từ chối / Hủy / Thanh lý */}
        {actionTarget && (
          <div className="modal-backdrop" onClick={() => { if (!isProcessingAction) setActionTarget(null); }}>
            <div className="modal-container" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h3>{actionTarget.title}</h3>
                <button
                  type="button"
                  className="modal-close-btn"
                  onClick={() => { if (!isProcessingAction) setActionTarget(null); }}
                >
                  ✕
                </button>
              </div>

              <form onSubmit={handleConfirmAction}>
                <div className="modal-body">
                  <p style={{ margin: '0 0 12px', fontSize: '14px', color: '#475569' }}>
                    Mã hợp đồng: <strong>{actionTarget.contract.contractCode || actionTarget.contract.contractNumber || actionTarget.contract.id.slice(0, 8)}</strong>
                  </p>
                  <p style={{ margin: '0 0 16px', fontSize: '13px', color: '#64748b' }}>
                    Phòng trọ: {actionTarget.contract.roomTitle || actionTarget.contract.room?.title || 'Phòng trọ'}
                  </p>

                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                    Lý do {actionTarget.title.toLowerCase()}:
                  </label>
                  <textarea
                    value={actionReason}
                    onChange={(e) => setActionReason(e.target.value)}
                    required
                    rows={3}
                    placeholder="Vui lòng nhập lý do cụ thể..."
                    style={{
                      width: '100%',
                      padding: '10px 12px',
                      border: '1px solid #cbd5e1',
                      borderRadius: '8px',
                      fontSize: '13px',
                      fontFamily: 'inherit',
                    }}
                  />
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => { if (!isProcessingAction) setActionTarget(null); }}
                    disabled={isProcessingAction}
                  >
                    Đóng
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    style={{ background: '#ef4444', borderColor: '#ef4444' }}
                    disabled={isProcessingAction}
                  >
                    {isProcessingAction ? 'Đang thực hiện...' : 'Xác nhận'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
