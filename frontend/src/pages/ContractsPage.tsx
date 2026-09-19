import React, { useState, useEffect, useCallback } from 'react';
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
import type { Contract, ContractStatus } from '../types/contract';

export default function ContractsPage() {
  const { user } = useAuth();

  // Active Tab: 'tenant' (Hợp đồng thuê của tôi) hoặc 'landlord' (Quản lý khách thuê)
  const isLandlordOrAdmin = user?.role === 'LANDLORD' || user?.role === 'ADMIN';
  const [activeTab, setActiveTab] = useState<'tenant' | 'landlord'>(
    isLandlordOrAdmin ? 'landlord' : 'tenant'
  );

  const [contracts, setContracts] = useState<Contract[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Pagination
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Filter status
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');

  // Dialog cập nhật trạng thái (Từ chối / Hủy)
  const [actionTarget, setActionTarget] = useState<{
    contract: Contract;
    targetStatus: ContractStatus;
    title: string;
  } | null>(null);
  const [actionReason, setActionReason] = useState<string>('');
  const [isProcessingAction, setIsProcessingAction] = useState<boolean>(false);

  const loadContracts = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      let res;
      if (activeTab === 'tenant') {
        res = await contractService.getMyTenantContracts(page, 10);
      } else {
        res = await contractService.getMyLandlordContracts(page, 10);
      }
      setContracts(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      console.error('Lỗi tải danh sách hợp đồng:', err);
      setError(err.response?.data?.message || 'Không thể tải danh sách hợp đồng.');
    } finally {
      setIsLoading(false);
    }
  }, [activeTab, page]);

  useEffect(() => {
    loadContracts();
  }, [loadContracts]);

  // Lọc theo trạng thái ở client side (hoặc kết hợp với backend)
  const filteredContracts = contracts.filter((c) => {
    if (selectedStatus === 'ALL') return true;
    return c.status === selectedStatus;
  });

  // Xử lý Phê duyệt hợp đồng (Landlord)
  const handleApprove = async (contract: Contract) => {
    const code = contract.contractCode || contract.contractNumber || contract.id.slice(0, 8);
    if (
      !window.confirm(
        `Xác nhận phê duyệt yêu cầu thuê phòng #${code}? Phòng trọ sẽ được chuyển sang trạng thái "ĐÃ CHO THUÊ".`
      )
    ) {
      return;
    }

    try {
      setIsProcessingAction(true);
      await contractService.updateContractStatus(contract.id, {
        status: 'ACTIVE',
      });
      alert('Đã phê duyệt hợp đồng thành công!');
      loadContracts();
    } catch (err: any) {
      console.error('Lỗi phê duyệt hợp đồng:', err);
      alert(err.response?.data?.message || 'Có lỗi xảy ra khi phê duyệt hợp đồng.');
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
    if (!actionTarget) return;

    try {
      setIsProcessingAction(true);
      await contractService.updateContractStatus(actionTarget.contract.id, {
        status: actionTarget.targetStatus,
        reason: actionReason.trim() || undefined,
      });
      alert(`Thao tác thành công: ${actionTarget.title}`);
      setActionTarget(null);
      loadContracts();
    } catch (err: any) {
      console.error('Lỗi xử lý hợp đồng:', err);
      alert(err.response?.data?.message || 'Có lỗi xảy ra.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  // Helper render status badge
  const renderStatusBadge = (status: ContractStatus) => {
    switch (status) {
      case 'PENDING':
        return <span className="status-badge status-pending"><Clock size={13} /> Chờ duyệt</span>;
      case 'ACTIVE':
        return <span className="status-badge status-active"><CheckCircle2 size={13} /> Đang hiệu lực</span>;
      case 'REJECTED':
        return <span className="status-badge status-rejected"><XCircle size={13} /> Bị từ chối</span>;
      case 'EXPIRED':
        return <span className="status-badge status-expired"><Clock size={13} /> Đã hết hạn</span>;
      case 'TERMINATED':
        return <span className="status-badge status-terminated"><AlertTriangle size={13} /> Đã thanh lý</span>;
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
              Quản lý Hợp đồng & Đặt cọc
            </h1>
            <p style={{ margin: 0, color: '#64748b', fontSize: '14px' }}>
              Theo dõi tiến độ thuê phòng, hợp đồng điện tử và trạng thái đặt cọc minh bạch.
            </p>
          </div>

          <Link to="/rooms" className="btn btn-outline" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Building size={16} />
            Tìm thêm phòng mới
          </Link>
        </div>

        {/* Tabs: Tenant vs Landlord */}
        <div className="contract-tabs">
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
            { key: 'ACTIVE', label: 'Đang hiệu lực' },
            { key: 'REJECTED', label: 'Bị từ chối' },
            { key: 'TERMINATED', label: 'Đã thanh lý' },
            { key: 'CANCELLED', label: 'Đã hủy' },
          ].map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setSelectedStatus(item.key)}
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
                          <span>Tiền đặt cọc:</span>
                          <strong>{contract.depositAmount?.toLocaleString('vi-VN')} đ</strong>
                        </div>
                        <div className="price-row">
                          <span>Tổng ban đầu:</span>
                          <span>{(contract.monthlyRent + contract.depositAmount)?.toLocaleString('vi-VN')} đ</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Actions Bar */}
                  <div className="contract-actions">
                    {/* Đối với Chủ nhà khi có yêu cầu PENDING */}
                    {activeTab === 'landlord' && contract.status === 'PENDING' && (
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
                          Phê duyệt hợp đồng
                        </button>
                      </>
                    )}

                    {/* Đối với Người thuê khi đơn còn PENDING */}
                    {activeTab === 'tenant' && contract.status === 'PENDING' && (
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
                    {contract.status === 'ACTIVE' && (
                      <button
                        type="button"
                        className="btn btn-outline"
                        style={{ fontSize: '13px', padding: '6px 14px' }}
                        onClick={() => handleOpenActionModal(contract, 'TERMINATED', 'Chấm dứt / Thanh lý hợp đồng')}
                        disabled={isProcessingAction}
                      >
                        <AlertTriangle size={15} style={{ marginRight: '4px', verticalAlign: 'middle' }} />
                        Thanh lý hợp đồng
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

        {/* Modal nhập lý do Từ chối / Hủy / Thanh lý */}
        {actionTarget && (
          <div className="modal-backdrop" onClick={() => setActionTarget(null)}>
            <div className="modal-container" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h3>{actionTarget.title}</h3>
                <button
                  type="button"
                  className="modal-close-btn"
                  onClick={() => setActionTarget(null)}
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
                    onClick={() => setActionTarget(null)}
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
