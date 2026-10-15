import { useState } from 'react';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import {
  Download,
  Printer,
  X,
  FileText,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Eye,
  Loader2
} from 'lucide-react';
import type { Contract } from '../types/contract';
import './ContractPdfModal.css';

interface ContractPdfModalProps {
  contract: Contract;
  onClose: () => void;
}

export default function ContractPdfModal({ contract: c, onClose }: ContractPdfModalProps) {
  const [isDownloading, setIsDownloading] = useState(false);
  const [showRawText, setShowRawText] = useState(false);

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '…';
    const d = new Date(dateStr);
    return `ngày ${d.getDate()} tháng ${d.getMonth() + 1} năm ${d.getFullYear()}`;
  };

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return 'Chưa ghi nhận';
    return new Date(dateStr).toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const formatMoney = (amount?: number) => {
    if (amount === undefined || amount === null) return '0 đ';
    return `${amount.toLocaleString('vi-VN')} VNĐ`;
  };

  const handlePrint = () => {
    window.print();
  };

  const handleDownloadPdf = async () => {
    const element = document.getElementById('contract-pdf-render-area');
    if (!element) return;

    setIsDownloading(true);
    try {
      // Chụp phần tử văn bản A4 với độ phân giải cao
      const canvas = await html2canvas(element, {
        scale: 2,
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff'
      });

      const imgData = canvas.toDataURL('image/jpeg', 0.95);
      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'mm',
        format: 'a4'
      });

      const imgWidth = 210; // Khổ ngang A4 (mm)
      const pageHeight = 297; // Khổ dọc A4 (mm)
      const imgHeight = (canvas.height * imgWidth) / canvas.width;
      let heightLeft = imgHeight;
      let position = 0;

      // Trang 1
      pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;

      // Thêm trang nếu văn bản dài hơn 1 trang A4
      while (heightLeft > 0) {
        position = heightLeft - imgHeight;
        pdf.addPage();
        pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      const code = c.contractCode || c.contractNumber || c.requestCode || c.id.slice(0, 8);
      pdf.save(`Hop_dong_thue_phong_${code}.pdf`);
    } catch (err) {
      console.error('Lỗi xuất file PDF:', err);
      alert('Không thể tạo file PDF tự động. Bạn có thể sử dụng nút "In / Lưu PDF" để lưu văn bản dưới dạng PDF qua trình duyệt.');
    } finally {
      setIsDownloading(false);
    }
  };

  const code = c.contractCode || c.contractNumber || c.requestCode || c.id.slice(0, 8);
  const isBothSigned = Boolean(c.tenantSignedAt && c.landlordSignedAt);

  return (
    <div className="contract-pdf-overlay" onClick={onClose}>
      <div className="contract-pdf-modal" onClick={e => e.stopPropagation()}>
        {/* Toolbar điều khiển */}
        <div className="contract-pdf-toolbar">
          <div className="contract-pdf-title-info">
            <FileText size={20} color="#167c5a" />
            <div>
              <h3>Hợp đồng thuê phòng: {code}</h3>
            </div>
            <span
              className="contract-pdf-status-tag"
              style={{
                backgroundColor: isBothSigned ? '#dcfce7' : c.depositPaidAt ? '#e0f2fe' : '#fef3c7',
                color: isBothSigned ? '#15803d' : c.depositPaidAt ? '#0369a1' : '#b45309'
              }}
            >
              {isBothSigned
                ? 'Đã hoàn tất ký số hai bên'
                : c.depositPaidAt
                ? 'Đã đặt cọc - Chờ ký số'
                : 'Chờ thanh toán cọc'}
            </span>
          </div>

          <div className="contract-pdf-actions">
            <button
              className="contract-pdf-btn"
              onClick={() => setShowRawText(!showRawText)}
              title="Xem văn bản niêm phong thô"
            >
              <Eye size={15} />
              {showRawText ? 'Xem định dạng A4' : 'Xem mã niêm phong'}
            </button>

            <button
              className="contract-pdf-btn"
              onClick={handlePrint}
              title="In hoặc lưu PDF qua trình duyệt"
            >
              <Printer size={15} />
              In / Lưu PDF
            </button>

            <button
              className="contract-pdf-btn contract-pdf-btn-primary"
              onClick={handleDownloadPdf}
              disabled={isDownloading}
              title="Tải trực tiếp file PDF về máy"
            >
              {isDownloading ? <Loader2 size={15} className="spin-animation" /> : <Download size={15} />}
              {isDownloading ? 'Đang tạo PDF…' : 'Tải file PDF'}
            </button>

            <button className="contract-pdf-btn" onClick={onClose} aria-label="Đóng">
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Khung cuộn tài liệu */}
        <div className="contract-pdf-scroll-container">
          {showRawText ? (
            <div
              style={{
                background: '#ffffff',
                padding: '30px',
                borderRadius: '8px',
                maxWidth: '794px',
                width: '100%',
                fontFamily: 'monospace',
                fontSize: '13px',
                lineHeight: 1.6,
                whiteSpace: 'pre-wrap',
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
              }}
            >
              {c.documentContent || 'Chưa có nội dung văn bản ký số.'}
            </div>
          ) : (
            <div id="contract-pdf-render-area" className="contract-a4-page">
              {/* Tiêu ngữ Quốc gia */}
              <div className="contract-legal-header">
                <div className="national-motto-country">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</div>
                <div className="national-motto-independence">Độc lập – Tự do – Hạnh phúc</div>
                <div className="national-motto-divider" />
                <h1 className="contract-document-title">HỢP ĐỒNG THUÊ PHÒNG TRỌ</h1>
                <div className="contract-document-code">Mã số hợp đồng: {code}</div>
                <p className="contract-preamble">
                  Hôm nay, {formatDate(c.createdAt || c.formalizedAt)}, tại Hệ thống quản lý và giao dịch thuê phòng thông minh SmartRental, chúng tôi gồm có các bên ký kết sau đây:
                </p>
              </div>

              {/* Thông tin hai bên */}
              <div className="contract-section">
                <div className="contract-section-heading">I. CÁC BÊN THAM GIA HỢP ĐỒNG</div>
                <div className="contract-parties-grid">
                  {/* Bên cho thuê */}
                  <div className="contract-party-box">
                    <div className="contract-party-role">BÊN CHO THUÊ (BÊN A)</div>
                    <p><strong>Họ và tên:</strong> {c.landlordName || c.landlord?.fullName || 'Chưa cung cấp'}</p>
                    <p><strong>Số điện thoại:</strong> {c.landlordPhone || c.landlord?.phoneNumber || 'Chưa cung cấp'}</p>
                    <p><strong>Email:</strong> {c.landlordEmail || c.landlord?.email || 'Chưa cung cấp'}</p>
                    <p><strong>Vai trò:</strong> Chủ sở hữu / Người đại diện quản lý phòng trọ</p>
                  </div>

                  {/* Bên thuê */}
                  <div className="contract-party-box">
                    <div className="contract-party-role">BÊN THUÊ (BÊN B)</div>
                    <p><strong>Họ và tên:</strong> {c.tenantName || c.tenant?.fullName || 'Chưa cung cấp'}</p>
                    <p><strong>Số điện thoại:</strong> {c.tenantPhone || c.tenant?.phoneNumber || 'Chưa cung cấp'}</p>
                    <p><strong>Email:</strong> {c.tenantEmail || c.tenant?.email || 'Chưa cung cấp'}</p>
                    <p><strong>Vai trò:</strong> Khách thuê phòng trực tiếp</p>
                  </div>
                </div>
              </div>

              {/* Điều khoản hợp đồng */}
              <div className="contract-section">
                <div className="contract-section-heading">II. CÁC ĐIỀU KHOẢN THỎA THUẬN</div>

                <div className="contract-clause-item">
                  <span className="contract-clause-title">Điều 1. Đối tượng và mục đích thuê:</span>
                  <br />
                  - Bên A đồng ý cho Bên B thuê phòng trọ: <strong>{c.roomTitle || c.room?.title || 'Phòng trọ'}</strong>.
                  <br />
                  - Địa chỉ tài sản: {[c.roomAddress || c.room?.address, c.roomDistrict || c.room?.district, c.room?.city].filter(Boolean).join(', ') || 'Theo mô tả trên hệ thống'}.
                  <br />
                  - Mục đích thuê: Dùng để ở và sinh hoạt cá nhân hợp pháp theo quy định của pháp luật.
                </div>

                <div className="contract-clause-item">
                  <span className="contract-clause-title">Điều 2. Thời hạn thuê:</span>
                  <br />
                  - Thời hạn thuê bắt đầu từ: <strong>{c.startDate}</strong> đến hết ngày <strong>{c.endDate}</strong>.
                  <br />
                  - Hợp đồng có thể gia hạn theo sự thỏa thuận của hai bên trước khi kết thúc thời hạn 30 ngày.
                </div>

                <div className="contract-clause-item">
                  <span className="contract-clause-title">Điều 3. Giá thuê và phương thức thanh toán tiền cọc:</span>
                  <table className="contract-table">
                    <thead>
                      <tr>
                        <th>Nội dung thanh toán</th>
                        <th>Số tiền</th>
                        <th>Tình trạng</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>Tiền thuê hàng tháng</td>
                        <td><strong>{formatMoney(c.monthlyRent)}</strong></td>
                        <td>Thanh toán định kỳ vào đầu mỗi tháng</td>
                      </tr>
                      <tr>
                        <td>Tiền đặt cọc bảo đảm</td>
                        <td><strong>{formatMoney(c.depositAmount)}</strong></td>
                        <td style={{ color: c.depositPaidAt ? '#15803d' : '#b45309', fontWeight: 600 }}>
                          {c.depositPaidAt
                            ? `✓ Đã thanh toán qua VNPAY (${formatDateTime(c.depositPaidAt)})`
                            : 'Chưa thanh toán cọc'}
                        </td>
                      </tr>
                      {c.paymentReference && (
                        <tr>
                          <td>Mã giao dịch đối soát VNPAY</td>
                          <td colSpan={2} style={{ fontFamily: 'monospace' }}>{c.paymentReference}</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="contract-clause-item">
                  <span className="contract-clause-title">Điều 4. Quyền và nghĩa vụ của các bên:</span>
                  <br />
                  - <strong>Quyền và nghĩa vụ Bên A:</strong> Bàn giao phòng đúng hẹn, đảm bảo trang thiết bị hoạt động tốt; bảo đảm an ninh và không tự ý xâm nhập vào phòng thuê khi chưa có sự đồng ý của Bên B (trừ trường hợp khẩn cấp).
                  <br />
                  - <strong>Quyền và nghĩa vụ Bên B:</strong> Thanh toán tiền thuê đúng thời hạn; sử dụng phòng đúng mục đích; giữ gìn vệ sinh chung, tuân thủ nội quy phòng cháy chữa cháy và quy định đăng ký tạm trú của cơ quan chức năng.
                </div>

                <div className="contract-clause-item">
                  <span className="contract-clause-title">Điều 5. Thỏa thuận và ghi chú riêng:</span>
                  <br />
                  {c.terms ? (
                    <p style={{ margin: '4px 0', padding: '6px 10px', background: '#f8fafc', borderLeft: '3px solid #167c5a' }}>
                      {c.terms}
                    </p>
                  ) : (
                    <span>Hai bên cam kết tuân thủ đầy đủ các thỏa thuận theo quy chế thuê trọ của hệ thống.</span>
                  )}
                </div>

                <div className="contract-clause-item">
                  <span className="contract-clause-title">Điều 6. Ký kết điện tử và Giá trị pháp lý (VNPT SmartCA):</span>
                  <br />
                  - Hợp đồng này được tạo lập, thỏa thuận và ký kết dưới dạng thông điệp dữ liệu điện tử theo quy định của <strong>Luật Giao dịch điện tử số 20/2023/QH15</strong>.
                  <br />
                  - Hai bên thực hiện ký số điện tử thông qua <strong>Dịch vụ Chữ ký số từ xa VNPT SmartCA</strong> được Bộ Thông tin và Truyền thông cấp phép. Chữ ký số của các bên gắn liền với văn bản có giá trị pháp lý tương đương chữ ký tay và con dấu.
                  <br />
                  - Hợp đồng có hiệu lực kể từ thời điểm cả Bên A và Bên B hoàn tất ký số điện tử trên hệ thống.
                </div>
              </div>

              {/* Khung con dấu chữ ký số điện tử */}
              <div className="contract-signatures-container">
                {/* Chữ ký Bên A (Chủ nhà) */}
                <div className="contract-signature-column">
                  <div className="signature-role-title">ĐẠI DIỆN BÊN CHO THUÊ (BÊN A)</div>
                  <div className="signature-role-subtitle">(Ký số qua VNPT SmartCA)</div>

                  {c.landlordSignedAt ? (
                    <div className="vnpt-smartca-stamp">
                      <div className="vnpt-stamp-header">
                        <CheckCircle2 size={16} />
                        <span>ĐÃ KÝ SỐ ĐIỆN TỬ</span>
                      </div>
                      <div className="vnpt-stamp-signer">Ký bởi: {c.landlordName || c.landlord?.fullName}</div>
                      <div className="vnpt-stamp-detail">Dịch vụ: VNPT SmartCA Remote Signing</div>
                      {c.landlordCertSerial && (
                        <div className="vnpt-stamp-detail">Serial CTS: {c.landlordCertSerial}</div>
                      )}
                      {c.landlordSignTxnId && (
                        <div className="vnpt-stamp-detail">Mã GD ký: {c.landlordSignTxnId}</div>
                      )}
                      <div className="vnpt-stamp-detail">Thời gian: {formatDateTime(c.landlordSignedAt)}</div>
                      <div className="vnpt-stamp-footer">
                        <ShieldCheck size={14} />
                        <span>CHỨNG THỰC BỞI VNPT-CA</span>
                      </div>
                    </div>
                  ) : (
                    <div className="unsigned-stamp-placeholder">
                      <AlertCircle size={20} style={{ margin: '0 auto 6px', display: 'block' }} />
                      Chưa hoàn tất ký số điện tử
                    </div>
                  )}
                </div>

                {/* Chữ ký Bên B (Người thuê) */}
                <div className="contract-signature-column">
                  <div className="signature-role-title">ĐẠI DIỆN BÊN THUÊ (BÊN B)</div>
                  <div className="signature-role-subtitle">(Ký số qua VNPT SmartCA)</div>

                  {c.tenantSignedAt ? (
                    <div className="vnpt-smartca-stamp">
                      <div className="vnpt-stamp-header">
                        <CheckCircle2 size={16} />
                        <span>ĐÃ KÝ SỐ ĐIỆN TỬ</span>
                      </div>
                      <div className="vnpt-stamp-signer">Ký bởi: {c.tenantName || c.tenant?.fullName}</div>
                      <div className="vnpt-stamp-detail">Dịch vụ: VNPT SmartCA Remote Signing</div>
                      {c.tenantCertSerial && (
                        <div className="vnpt-stamp-detail">Serial CTS: {c.tenantCertSerial}</div>
                      )}
                      {c.tenantSignTxnId && (
                        <div className="vnpt-stamp-detail">Mã GD ký: {c.tenantSignTxnId}</div>
                      )}
                      <div className="vnpt-stamp-detail">Thời gian: {formatDateTime(c.tenantSignedAt)}</div>
                      <div className="vnpt-stamp-footer">
                        <ShieldCheck size={14} />
                        <span>CHỨNG THỰC BỞI VNPT-CA</span>
                      </div>
                    </div>
                  ) : (
                    <div className="unsigned-stamp-placeholder">
                      <AlertCircle size={20} style={{ margin: '0 auto 6px', display: 'block' }} />
                      Chưa hoàn tất ký số điện tử
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
