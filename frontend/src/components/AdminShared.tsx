import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import { labels } from '../services/adminFormat';
import type { PageResponse } from '../services/roomService';

export function Badge({ value }: { value: string }) { return <span className={`admin-badge badge-${value.toLowerCase()}`}>{labels[value] || value}</span>; }

export function LoadState({ loading, error, retry }: { loading: boolean; error: string; retry: () => void }) {
  return <>{loading && <p className="admin-empty" role="status">Đang tải dữ liệu…</p>}
    {error && <div className="admin-alert" role="alert">{error} <button onClick={retry}>Thử lại</button></div>}</>;
}
export function Pager({ data, change }: { data: Pick<PageResponse<unknown>, 'pageNumber' | 'totalPages' | 'totalElements'>; change: (page: number) => void }) {
  return <div className="admin-pager"><span>{data.totalElements.toLocaleString('vi-VN')} kết quả · Trang {data.totalPages ? data.pageNumber + 1 : 0}/{data.totalPages}</span>
    <div><button disabled={data.pageNumber === 0} onClick={() => change(data.pageNumber - 1)}>Trước</button>
      <button disabled={data.pageNumber + 1 >= data.totalPages} onClick={() => change(data.pageNumber + 1)}>Sau</button></div></div>;
}
export function Modal({ title, children, close }: { title: string; children: ReactNode; close: () => void }) {
  const closeRef = useRef(close);
  useEffect(() => { closeRef.current = close; }, [close]);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const oldOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const dialog = document.querySelector<HTMLElement>('.admin-dialog');
    dialog?.focus();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeRef.current();
      if (e.key === 'Tab' && dialog) {
        const elements = Array.from(dialog.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href]'));
        const first = elements[0], last = elements[elements.length - 1];
        if (!first) { e.preventDefault(); return; }
        if (e.shiftKey && (document.activeElement === first || document.activeElement === dialog)) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && (document.activeElement === last || document.activeElement === dialog)) { e.preventDefault(); first.focus(); }
      }
    };
    document.addEventListener('keydown', onKey);
    return () => { document.body.style.overflow = oldOverflow; document.removeEventListener('keydown', onKey); previous?.focus(); };
  }, []);
  return <div className="admin-overlay"><section className="admin-dialog" role="dialog" aria-modal="true" aria-label={title} tabIndex={-1}>
    <div className="admin-dialog-title"><h2>{title}</h2><button aria-label="Đóng" onClick={close}>×</button></div>{children}</section></div>;
}
export function Empty({ children = 'Chưa có dữ liệu phù hợp.' }: { children?: ReactNode }) { return <p className="admin-empty">{children}</p>; }
