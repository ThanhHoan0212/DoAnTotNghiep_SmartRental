import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import ts from 'typescript';

const require = createRequire(import.meta.url);
function compile(source) {
  const { outputText } = ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX,
  } });
  const resolved = outputText.replace(/from ["'](react(?:\/jsx-runtime)?|lucide-react)["']/g,
    (_, name) => `from ${JSON.stringify(pathToFileURL(require.resolve(name)).href)}`);
  return `data:text/javascript;base64,${Buffer.from(resolved).toString('base64')}`;
}
const panelSource = readFileSync(new URL('../src/components/ContractClosurePanel.tsx', import.meta.url), 'utf8')
  .replace(/import \{ contractService \} from .*?;/, 'const contractService = {};');
const panelUrl = compile(panelSource);
const { default: Panel } = await import(panelUrl);
const detailSource = readFileSync(new URL('../src/components/ContractDetailDialog.tsx', import.meta.url), 'utf8')
  .replace(/import '\.\/ContractDetailDialog.css';/, '')
  .replace("'./ContractClosurePanel'", JSON.stringify(panelUrl));
const { default: Detail } = await import(compile(detailSource));
const contract = {
  id: 'contract', tenantId: 'tenant', landlordId: 'landlord', status: 'ACTIVE',
  tenantName: 'Người thuê A', landlordName: 'Chủ nhà B',
  depositPaidAt: '2026-09-01T00:00:00Z', monthlyRent: 4000000, depositAmount: 4000000,
  startDate: '2026-09-01', endDate: '2027-09-01', createdAt: '2026-09-01T00:00:00Z',
};
const request = {
  requestId: 'request', kind: 'EARLY_TERMINATION', status: 'PENDING', requestedBy: 'tenant',
  requestedAt: '2026-09-20T00:00:00Z', requestedEndDate: '2026-10-01', reason: 'Chuyển nơi làm việc',
  refundType: 'PARTIAL', refundAmount: 2000000, settlementNote: 'Hai bên thống nhất giữ lại một nửa',
};
const render = (c, userId) => renderToStaticMarkup(createElement(Panel, {
  contract: c, userId, busy: false, onBusy() {}, onUpdated() {},
}));

test('active agreement offers a request rather than immediate termination', () => {
  const html = render(contract, 'tenant');
  assert.match(html, /Yêu cầu chấm dứt hợp đồng trước hạn/);
  assert.doesNotMatch(html, /Thanh lý hợp đồng/);
});

test('only the counterparty sees response controls', () => {
  const c = { ...contract, closureRequests: [request] };
  assert.match(render(c, 'landlord'), /Đồng ý yêu cầu/);
  for (const user of ['tenant', 'unrelated-admin', undefined]) {
    assert.doesNotMatch(render(c, user), /Đồng ý yêu cầu/);
  }
});

test('an accepted future termination displays continuing effect', () => {
  const html = render({ ...contract, agreedEndDate: '2026-10-01', closureRequests: [{ ...request, status: 'ACCEPTED' }] }, 'tenant');
  assert.match(html, /Hợp đồng vẫn có hiệu lực/);
  assert.match(html, /chờ ngày chấm dứt/);
  assert.doesNotMatch(html, /Gửi yêu cầu cho bên còn lại|Đồng ý yêu cầu/);
});

test('completed closure preserves settlement without claiming a refund payment', () => {
  const html = render({ ...contract, status: 'TERMINATED', closureRequests: [{ ...request, status: 'COMPLETED' }] }, 'tenant');
  assert.match(html, /Hoàn một phần cọc/);
  assert.match(html, /2\.000\.000/);
  assert.match(html, /không phải xác nhận đã hoàn tiền/);
});

test('rejected requests remain visible and allow a new proposal', () => {
  const html = render({ ...contract, closureRequests: [{ ...request, status: 'REJECTED' }] }, 'tenant');
  assert.match(html, /Đã từ chối/);
  assert.match(html, /Yêu cầu chấm dứt hợp đồng trước hạn/);
});

test('pending paid cancellation suppresses the signature action', () => {
  const props = { contract: { ...contract, status: 'AWAITING_SIGNATURES', documentContent: 'Nội dung đã chốt' }, userId: 'landlord',
    busy: false, accepted: true, onAccept() {}, onClose() {}, onSign() {}, onBusy() {}, onUpdated() {}, statusBadge: 'Chờ ký' };
  assert.match(renderToStaticMarkup(createElement(Detail, props)), /Xác nhận ký \(giả lập\)/);
  const blocked = renderToStaticMarkup(createElement(Detail, { ...props, contract: { ...props.contract,
    closureRequests: [{ ...request, kind: 'CANCELLATION', requestedEndDate: undefined }] } }));
  assert.doesNotMatch(blocked, /Xác nhận ký \(giả lập\)/);
  assert.match(blocked, /Tạm dừng ký/);
});
