import { Fragment, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { ArrowLeft, ArrowUpRight, Building2, ChevronRight, Clock3, LoaderCircle, MessageCircle, MessageSquare, Send, ShieldCheck, X, ImagePlus, Play, SmilePlus, Undo2 } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { chatService } from '../services/chatService';
import type { ChatMessage, Conversation } from '../services/chatService';
import './ChatPage.css';

function mergeMessages(previous: ChatMessage[], incoming: ChatMessage[]) {
  const merged = new Map(previous.map(m => [m.id, m]));
  incoming.forEach(m => {
    const current = merged.get(m.id);
    if (!current || (m.revision ?? 0) >= (current.revision ?? 0)) merged.set(m.id, m);
  });
  return [...merged.values()]
    .sort((a, b) => a.sentAt.localeCompare(b.sentAt) || a.id.localeCompare(b.id));
}

function initials(name: string) {
  return name.trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase() || 'SR';
}

function ChatAvatar({ name, url, small = false }: { name: string; url?: string | null; small?: boolean }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  return <span className={`chat-avatar${small ? ' chat-avatar-small' : ''}`}>
    {url && url !== failedUrl ? <img src={url} alt={`Ảnh đại diện của ${name}`} onError={() => setFailedUrl(url)} /> : initials(name)}
  </span>;
}

function dayLabel(value: string) {
  const date = new Date(value);
  const today = new Date();
  if (date.toDateString() === today.toDateString()) return 'Hôm nay';
  const yesterday = new Date(today); yesterday.setDate(today.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) return 'Hôm qua';
  return date.toLocaleDateString('vi-VN', { day: 'numeric', month: 'long', year: 'numeric' });
}

function MessageImage({ conversationId, messageId, onLoad }: { conversationId: string; messageId: string; onLoad?: () => void }) {
  const [src, setSrc] = useState('');
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const viewer = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    let cancelled = false;
    chatService.image(conversationId, messageId).then(result => {
      if (!cancelled) { setSrc(`data:${result.contentType};base64,${result.data}`); setFailed(false); }
    }).catch(() => { if (!cancelled) setFailed(true); });
    return () => { cancelled = true; };
  }, [conversationId, messageId, attempt]);
  if (failed) return <button type="button" className="chat-image-retry" onClick={() => { setFailed(false); setAttempt(a => a + 1); }}>Tải lại ảnh</button>;
  if (!src) return <span className="chat-image-loading"><LoaderCircle size={20} className="chat-spinner" /> Đang tải ảnh...</span>;
  return <>
    <button type="button" className="chat-image-button" aria-label="Xem ảnh kích thước lớn" onClick={() => viewer.current?.showModal()}>
      <img className="chat-message-image" src={src} alt="Ảnh được gửi trong cuộc trò chuyện" onLoad={onLoad} onError={() => setFailed(true)} />
    </button>
    <dialog ref={viewer} className="chat-image-viewer" aria-label="Ảnh trong cuộc trò chuyện" onCancel={event => event.stopPropagation()} onClick={e => { if (e.target === e.currentTarget) viewer.current?.close(); }}>
      <button type="button" aria-label="Đóng ảnh" onClick={() => viewer.current?.close()}><X size={22} /></button>
      <img src={src} alt="Ảnh được gửi trong cuộc trò chuyện, kích thước lớn" />
    </dialog>
  </>;
}

function MessageVideo({ conversationId, messageId, onLoad }: { conversationId: string; messageId: string; onLoad?: () => void }) {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const alive = useRef(true);
  const blobUrl = useRef('');
  useEffect(() => {
    alive.current = true;
    return () => { alive.current = false; if (blobUrl.current) URL.revokeObjectURL(blobUrl.current); };
  }, []);
  async function load() {
    if (loading) return;
    setLoading(true); setError('');
    try {
      const blob = await chatService.video(conversationId, messageId);
      if (!alive.current) return;
      blobUrl.current = URL.createObjectURL(blob); setUrl(blobUrl.current);
    } catch { if (alive.current) setError('Không tải được video. Nhấn để thử lại.'); }
    finally { if (alive.current) setLoading(false); }
  }
  if (!url) return <button type="button" className="chat-video-load" disabled={loading} onClick={() => void load()}>
    {loading ? <LoaderCircle size={28} className="chat-spinner" /> : <Play size={28} />}<span>{loading ? 'Đang tải video...' : error || 'Xem video'}</span>
  </button>;
  return <div className="chat-video-player"><video controls playsInline preload="metadata" src={url} onLoadedMetadata={onLoad}
    onError={() => setError('Trình duyệt không phát được định dạng video này. Bạn có thể tải xuống để xem.')} />
    {error && <p role="alert">{error}</p>}<a href={url} download="video.mp4">Tải video</a></div>;
}

function SharedMediaDialog({ conversation, onClose }: { conversation: Conversation; onClose: () => void }) {
  const { user } = useAuth();
  const dialog = useRef<HTMLDialogElement>(null);
  const [items, setItems] = useState<ChatMessage[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const alive = useRef(true);
  useEffect(() => {
    let cancelled = false;
    alive.current = true;
    const el = dialog.current;
    if (el && !el.open) el.showModal();
    chatService.sharedMedia(conversation.id).then(result => {
      if (!cancelled) { setItems(result.items); setHasMore(result.hasMore); }
    }).catch(() => { if (!cancelled) setError('Không tải được ảnh và video đã chia sẻ.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; alive.current = false; el?.close(); };
  }, [conversation.id]);
  async function loadMore() {
    if (loading) return;
    setLoading(true); setError('');
    const next = items.length ? page + 1 : 0;
    try {
      const result = await chatService.sharedMedia(conversation.id, next);
      if (!alive.current) return;
      setItems(previous => [...new Map([...previous, ...result.items].map(item => [item.id, item])).values()]);
      setPage(next); setHasMore(result.hasMore);
    } catch { if (alive.current) setError('Không tải được tệp đã chia sẻ. Vui lòng thử lại.'); }
    finally { if (alive.current) setLoading(false); }
  }
  return <dialog ref={dialog} className="chat-gallery-dialog" aria-labelledby="chat-gallery-title" onCancel={onClose}
    onClick={event => { if (event.target === event.currentTarget) {
      const rect = event.currentTarget.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) onClose();
    } }}>
    <div className="chat-gallery-heading"><div><h2 id="chat-gallery-title">Ảnh và video đã chia sẻ</h2><p>{conversation.otherUserName}</p></div>
      <button type="button" aria-label="Đóng thư viện ảnh và video" onClick={onClose}><X size={21} /></button></div>
    <div className="chat-gallery-scroll">
      {error && <p className="chat-error" role="alert">{error}</p>}
      {!loading && !error && !items.length && <div className="chat-thread-empty"><ImagePlus size={32} /><h3>Chưa có ảnh hoặc video</h3><p>Ảnh và video hai bên gửi sẽ xuất hiện ở đây.</p></div>}
      <div className="chat-gallery-grid">{items.map(item => <figure key={item.id}>
        {item.hasVideo ? <MessageVideo conversationId={conversation.id} messageId={item.id} /> : <MessageImage conversationId={conversation.id} messageId={item.id} />}
        <figcaption><strong>{item.senderId === user?.id ? 'Bạn' : conversation.otherUserName}</strong>
          <time dateTime={item.sentAt}>{new Date(item.sentAt).toLocaleDateString('vi-VN')}</time></figcaption>
      </figure>)}</div>
      {loading && <div className="chat-loading"><LoaderCircle size={20} className="chat-spinner" /> Đang tải ảnh...</div>}
      {!loading && (hasMore || error) && <button className="chat-history" type="button" onClick={() => void loadMore()}>{error ? 'Thử lại' : 'Xem thêm'}</button>}
    </div>
  </dialog>;
}

function UserProfileDialog({ conversation, onClose }: { conversation: Conversation; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const element = dialog.current;
    if (element && !element.open) element.showModal();
    return () => element?.close();
  }, []);
  const role = conversation.otherUserRole === 'ADMIN' ? 'Quản trị viên' : conversation.otherUserRole === 'LANDLORD' ? 'Chủ nhà' : conversation.otherUserRole === 'TENANT' ? 'Người thuê' : 'Thành viên';
  return <dialog ref={dialog} className="chat-profile-dialog" aria-labelledby="chat-profile-title" onCancel={onClose}
    onClick={event => { if (event.target === event.currentTarget) {
      const bounds = event.currentTarget.getBoundingClientRect();
      if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose();
    } }}>
    <div className="chat-profile-top"><span>THÔNG TIN THÀNH VIÊN</span><button type="button" onClick={onClose} aria-label="Đóng thông tin"><X size={20} /></button></div>
    <div className="chat-profile-identity"><ChatAvatar name={conversation.otherUserName} url={conversation.otherUserAvatarUrl} />
      <h2 id="chat-profile-title">{conversation.otherUserName}</h2><span className="chat-profile-role">{role}</span></div>
    <dl className="chat-profile-details">
      <div><dt>Email</dt><dd className="chat-profile-email">{conversation.otherUserEmail || 'Chưa có thông tin'}</dd></div>
      <div><dt>Xác thực danh tính</dt><dd className={conversation.otherUserIdentityVerified ? 'verified' : ''}>
      {conversation.otherUserIdentityVerified && <ShieldCheck size={16} />}{conversation.otherUserIdentityVerified ? 'Đã xác thực' : 'Chưa xác thực'}</dd></div>
      <div><dt>Ngày tham gia</dt><dd>{conversation.otherUserCreatedAt ? new Date(conversation.otherUserCreatedAt).toLocaleDateString('vi-VN') : 'Chưa có thông tin'}</dd></div>
    </dl>
    <button type="button" className="chat-profile-done" onClick={onClose}>Tiếp tục trò chuyện</button>
  </dialog>;
}

function Thread({ conversation, onBack }: { conversation: Conversation; onBack: () => void }) {
  const { user } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [olderLoading, setOlderLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [hasOlder, setHasOlder] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [galleryOpen, setGalleryOpen] = useState(false);
  const [reactionTarget, setReactionTarget] = useState<string | null>(null);
  const [messageBusy, setMessageBusy] = useState<string | null>(null);
  const [attachment, setAttachment] = useState<File | null>(null);
  const [preview, setPreview] = useState('');
  const fileInput = useRef<HTMLInputElement>(null);
  const historyPages = useRef(1);
  const bottom = useRef<HTMLDivElement>(null);
  const composer = useRef<HTMLTextAreaElement>(null);
  const nearBottom = useRef(true);
  const alive = useRef(true);

  useEffect(() => {
    return () => { if (preview) URL.revokeObjectURL(preview); };
  }, [preview]);

  function selectMedia(file?: File) {
    if (!file) return;
    const video = file.type === 'video/mp4';
    if ((!video && !['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) || file.size > (video ? 25 : 5) * 1024 * 1024 || file.size === 0) {
      setError('Chọn ảnh JPG/PNG/GIF tối đa 5 MB hoặc video MP4 tối đa 25 MB.'); return;
    }
    setError(''); setAttachment(file); setPreview(URL.createObjectURL(file));
  }

  useLayoutEffect(() => {
    const input = composer.current;
    if (!input) return;
    // With wrapping disabled, only explicit newlines change the editor height.
    // Avoid resizing on every keystroke, which can reset horizontal caret scrolling.
    const height = `${Math.min(100, 14 + draft.split('\n').length * 20)}px`;
    if (input.style.height !== height) input.style.height = height;
    if (document.activeElement === input && input.selectionStart === draft.length && input.selectionEnd === draft.length) {
      input.scrollLeft = input.scrollWidth;
      input.scrollTop = input.scrollHeight;
    }
  }, [draft]);

  useEffect(() => {
    alive.current = true;
    let cancelled = false;
    let firstLoad = true;
    let timer: ReturnType<typeof setTimeout>;
    async function refresh() {
      try {
        const pages = await Promise.all(Array.from({ length: historyPages.current }, (_, page) => chatService.history(conversation.id, page)));
        const latest = pages.flat();
        if (cancelled) return;
        setMessages(prev => mergeMessages(prev, latest));
        if (firstLoad) { setHasOlder(latest.length === 50); firstLoad = false; }
        setError('');
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Không tải được tin nhắn.');
      } finally {
        if (!cancelled) { setLoading(false); timer = setTimeout(refresh, 3000); }
      }
    }
    void refresh();
    return () => { cancelled = true; alive.current = false; clearTimeout(timer); };
  }, [conversation.id]);

  const lastId = messages.at(-1)?.id;
  useEffect(() => {
    if (nearBottom.current) bottom.current?.scrollIntoView({ block: 'nearest' });
  }, [lastId]);

  async function loadOlder() {
    setOlderLoading(true);
    try {
      const older = await chatService.history(conversation.id, page + 1);
      if (!alive.current) return;
      nearBottom.current = false;
      setMessages(prev => mergeMessages(prev, older));
      historyPages.current = page + 2;
      setPage(page + 1); setHasOlder(older.length === 50);
    } catch { if (alive.current) setError('Không tải được lịch sử. Vui lòng thử lại.'); }
    finally { if (alive.current) setOlderLoading(false); }
  }

  async function send(event: React.FormEvent) {
    event.preventDefault();
    if ((!draft.trim() && !attachment) || sending) return;
    setSending(true); setError('');
    try {
      const message = attachment
        ? attachment.type === 'video/mp4'
          ? await chatService.sendVideo(conversation.id, attachment, draft.trim())
          : await chatService.sendImage(conversation.id, attachment, draft.trim())
        : await chatService.send(conversation.id, draft.trim());
      if (!alive.current) return;
      nearBottom.current = true;
      setMessages(prev => mergeMessages(prev, [message])); setDraft(''); setAttachment(null); setPreview('');
    } catch (err) {
      if (alive.current) setError(err instanceof Error ? err.message : 'Gửi thất bại. Vui lòng thử lại.');
    } finally { if (alive.current) setSending(false); }
  }

  async function updateMessage(messageId: string, emoji?: string) {
    if (messageBusy) return;
    setMessageBusy(messageId); setError('');
    try {
      const updated = emoji
        ? await chatService.react(conversation.id, messageId, emoji)
        : await chatService.recall(conversation.id, messageId);
      if (alive.current) {
        setMessages(previous => mergeMessages(previous, [updated])); setReactionTarget(null);
      }
    } catch (err) { if (alive.current) setError(err instanceof Error ? err.message : 'Không cập nhật được tin nhắn.'); }
    finally { if (alive.current) setMessageBusy(null); }
  }

  return <section className="chat-thread" aria-label="Cuộc trò chuyện">
    <div className="chat-thread-heading">
      <button type="button" className="chat-back" onClick={onBack} aria-label="Quay lại hộp thư"><ArrowLeft size={20} /></button>
      <button type="button" className="chat-profile-trigger" onClick={() => setProfileOpen(true)} aria-label={`Xem thông tin ${conversation.otherUserName}`} aria-haspopup="dialog">
        <ChatAvatar name={conversation.otherUserName} url={conversation.otherUserAvatarUrl} />
        <span className="chat-person"><strong>{conversation.otherUserName}</strong></span>
      </button>
      <button type="button" className="chat-gallery-open" onClick={() => setGalleryOpen(true)} aria-label="Xem ảnh và video đã chia sẻ" aria-haspopup="dialog" title="Xem lại ảnh và video đã chia sẻ">
        <ImagePlus size={18} /><span>Ảnh và video</span>
      </button>
    </div>
    {profileOpen && <UserProfileDialog conversation={conversation} onClose={() => setProfileOpen(false)} />}
    {galleryOpen && <SharedMediaDialog conversation={conversation} onClose={() => setGalleryOpen(false)} />}
    {conversation.roomId && <Link className="chat-room-context" to={`/rooms/${conversation.roomId}`}>
      <span className="chat-room-icon"><Building2 size={20} /></span>
      <span><small>ĐANG TRAO ĐỔI VỀ</small><strong>{conversation.roomTitle}</strong></span>
      <ArrowUpRight size={19} />
    </Link>}
    {error && <p className="chat-error" role="alert">{error}</p>}
    <div className="chat-messages" role="log" aria-label="Tin nhắn" onScroll={event => {
      const el = event.currentTarget;
      nearBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
    }}>
      {loading ? <div className="chat-loading"><LoaderCircle size={22} className="chat-spinner" /> Đang tải tin nhắn...</div> : <>
        {hasOlder && <button className="chat-history" disabled={olderLoading} onClick={() => void loadOlder()}>
          {olderLoading ? <LoaderCircle size={14} className="chat-spinner" /> : <Clock3 size={14} />}
          {olderLoading ? 'Đang tải...' : 'Xem tin nhắn trước'}</button>}
        {messages.length === 0 && <div className="chat-thread-empty"><span className="chat-empty-icon"><MessageCircle size={28} /></span>
          <h3>Bắt đầu bằng một lời chào</h3><p>Hỏi thêm về phòng, giá thuê hoặc hẹn lịch xem phòng cùng {conversation.otherUserName}.</p></div>}
        {messages.map((m, index) => <Fragment key={m.id}>
          {(index === 0 || new Date(messages[index - 1].sentAt).toDateString() !== new Date(m.sentAt).toDateString()) &&
            <div className="chat-day"><span>{dayLabel(m.sentAt)}</span></div>}
          <div className={`chat-message-row ${m.senderId === user?.id ? 'mine' : ''}`}>
            {m.senderId !== user?.id && <ChatAvatar name={conversation.otherUserName} url={conversation.otherUserAvatarUrl} small />}
            <div className="chat-message-content"><div className={`chat-bubble ${!m.recalled && (m.hasImage || m.hasVideo) ? 'has-image' : ''} ${m.recalled ? 'recalled' : ''}`}>
              {!m.recalled && m.hasVideo && <MessageVideo conversationId={conversation.id} messageId={m.id} onLoad={() => {
                if (nearBottom.current) bottom.current?.scrollIntoView({ block: 'nearest' });
              }} />}
              {!m.recalled && m.hasImage && <MessageImage conversationId={conversation.id} messageId={m.id} onLoad={() => {
                if (nearBottom.current) bottom.current?.scrollIntoView({ block: 'nearest' });
              }} />}
              {m.recalled ? <p>Tin nhắn đã được thu hồi</p> : !((m.hasImage && m.content === '📷 Ảnh') || (m.hasVideo && m.content === '🎬 Video')) && <p>{m.content}</p>}
            </div>
              {!m.recalled && !!m.reactions?.length && <div className="chat-reactions" aria-label="Cảm xúc trên tin nhắn">
                {[...new Set(m.reactions.map(r => r.emoji))].map(emoji => {
                  const reactions = m.reactions!.filter(r => r.emoji === emoji);
                  const mine = reactions.some(r => r.userId === user?.id);
                  return <button key={emoji} type="button" className={mine ? 'selected' : ''} aria-pressed={mine}
                    disabled={messageBusy !== null} title={reactions.map(r => r.userId === user?.id ? 'Bạn' : conversation.otherUserName).join(', ')}
                    aria-label={`${emoji}: ${reactions.length} cảm xúc${mine ? ', nhấn để bỏ cảm xúc của bạn' : ''}`} onClick={() => void updateMessage(m.id, emoji)}>{emoji}<span>{reactions.length}</span></button>;
                })}
              </div>}
              {!m.recalled && <div className="chat-message-actions">
                <button type="button" title="Thả cảm xúc" aria-label="Thả cảm xúc cho tin nhắn" aria-expanded={reactionTarget === m.id}
                  disabled={messageBusy !== null} onClick={() => setReactionTarget(reactionTarget === m.id ? null : m.id)}><SmilePlus size={14} /></button>
                {m.senderId === user?.id && <button type="button" title="Thu hồi tin nhắn" aria-label="Thu hồi tin nhắn" disabled={messageBusy !== null}
                  onClick={() => void updateMessage(m.id)}><Undo2 size={14} /></button>}
                {messageBusy === m.id && <LoaderCircle size={13} className="chat-spinner" />}
              </div>}
              {!m.recalled && reactionTarget === m.id && <div className="chat-reaction-picker" role="group" aria-label="Chọn cảm xúc">
                {['👍', '❤️', '😂', '😮', '😢', '😡'].map(emoji => <button key={emoji} type="button" aria-label={`Thả ${emoji}`}
                  disabled={messageBusy !== null} onClick={() => void updateMessage(m.id, emoji)}>{emoji}</button>)}
                <button type="button" aria-label="Đóng chọn cảm xúc" onClick={() => setReactionTarget(null)}><X size={13} /></button>
              </div>}
              <time dateTime={m.sentAt} title={new Date(m.sentAt).toLocaleString('vi-VN')}>{new Date(m.sentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</time>
            </div>
            {m.senderId === user?.id && <ChatAvatar name={user?.fullName || 'Bạn'} url={user?.avatarUrl} small />}
          </div>
        </Fragment>)}
      </>}
      <div ref={bottom} />
    </div>
    <form className="chat-compose" onSubmit={send}>
      {attachment && <div className="chat-image-preview">
        {preview && (attachment.type === 'video/mp4' ? <video src={preview} controls playsInline preload="metadata" aria-label="Video chuẩn bị gửi" /> : <img src={preview} alt="Ảnh chuẩn bị gửi" />)}
        <span>{attachment.name}<small>{(attachment.size / 1024).toFixed(0)} KB</small></span>
        <button type="button" aria-label="Bỏ tệp đã chọn" disabled={sending} onClick={() => { setAttachment(null); setPreview(''); }}><X size={18} /></button>
      </div>}
      <div className="chat-compose-box">
      <input type="file" ref={fileInput} hidden accept="image/jpeg,image/png,image/gif,video/mp4,.mp4" onChange={e => { selectMedia(e.target.files?.[0]); e.target.value = ''; }} />
      <button type="button" className="chat-attach" title="Gửi ảnh hoặc video" aria-label="Chọn ảnh hoặc video để gửi" disabled={sending || loading} onClick={() => fileInput.current?.click()}><ImagePlus size={21} /></button>
      <textarea ref={composer} aria-label="Nội dung tin nhắn" placeholder="Nhập tin nhắn..." wrap="off"
        value={draft} disabled={sending} onChange={e => setDraft(e.target.value)} rows={1} onKeyDown={event => {
          if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
            event.preventDefault();
            if (!loading) event.currentTarget.form?.requestSubmit();
          }
        }} />
      <button className="chat-send" aria-label={sending ? 'Đang gửi tin nhắn' : 'Gửi tin nhắn'} title="Gửi tin nhắn"
        disabled={sending || loading || (!draft.trim() && !attachment)}>{sending ? <LoaderCircle size={19} className="chat-spinner" /> : <Send size={19} />}</button>
      </div>
      <div className="chat-compose-hint"><span>Enter để gửi · Shift + Enter để xuống dòng</span></div>
    </form>
  </section>;
}

export default function ChatPage() {
  const [params, setParams] = useSearchParams();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;
    async function refresh() {
      try {
        const list = await chatService.list();
        if (!cancelled) { setConversations(list); setError(''); }
      } catch { if (!cancelled) setError('Không tải được danh sách trò chuyện. Đang thử lại...'); }
      finally { if (!cancelled) { setLoading(false); timer = setTimeout(refresh, 5000); } }
    }
    void refresh();
    return () => { cancelled = true; clearTimeout(timer); };
  }, []);
  const selectedId = params.get('conversation');
  const selected = selectedId ? conversations.find(c => c.id === selectedId) : conversations[0];
  return <main className="chat-page"><div className="container">
    {error && <p role="alert" className="chat-error">{error}</p>}
    <div className={`chat-layout ${selectedId && selected ? 'has-selection' : ''}`}><aside className="chat-conversations" aria-label="Danh sách trò chuyện">
      <div className="chat-list-scroll">
      {loading && <div className="chat-loading"><LoaderCircle size={20} className="chat-spinner" /> Đang tải hộp thư...</div>}
      {!loading && !error && conversations.length === 0 && <div className="chat-sidebar-empty"><MessageCircle size={30} /><h3>Hộp thư đang chờ bạn</h3><p>Mở tin phòng và chọn “Nhắn tin cho chủ nhà” để bắt đầu.</p><Link to="/rooms">Tìm phòng phù hợp <ChevronRight size={15} /></Link></div>}
      {conversations.map(c => <button key={c.id} className={`chat-conversation ${selected?.id === c.id ? 'selected' : ''}`}
        onClick={() => setParams({ conversation: c.id })} aria-pressed={selected?.id === c.id}>
        <ChatAvatar name={c.otherUserName} url={c.otherUserAvatarUrl} />
        <span className="chat-conversation-copy"><span className="chat-conversation-top"><strong>{c.otherUserName}</strong>
          {c.updatedAt && <time dateTime={c.updatedAt}>{new Date(c.updatedAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })}</time>}</span>
          <span className="chat-conversation-room">{c.roomId && <Building2 size={13} />}{c.roomTitle || 'Trò chuyện trực tiếp'}</span>
          <span className="chat-conversation-caption">Mở cuộc trò chuyện <ChevronRight size={12} /></span>
        </span>
      </button>)}
      </div>
    </aside>
      {selected ? <Thread key={selected.id} conversation={selected} onBack={() => setParams({})} /> : <div className="chat-placeholder">
        <div className="chat-placeholder-art"><span><MessageSquare size={44} strokeWidth={1.5} /></span><span className="chat-art-house"><Building2 size={23} /></span></div>
        <span className="chat-eyebrow">MỘT LỜI CHÀO, MỘT KẾT NỐI MỚI</span><h2>Chuyện tìm nhà bắt đầu<br />từ một cuộc trò chuyện.</h2>
        <p>{selectedId && !loading && !error ? 'Không tìm thấy cuộc trò chuyện hoặc bạn không có quyền truy cập.' : 'Chọn một cuộc trò chuyện trong hộp thư hoặc khám phá tin phòng để kết nối với chủ nhà.'}</p>
      </div>}
    </div>
  </div></main>;
}
