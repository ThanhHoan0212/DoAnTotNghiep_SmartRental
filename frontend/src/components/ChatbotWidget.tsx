import { useState, useEffect, useRef } from 'react';
import { Bot, Send, Sparkles, RotateCcw, ChevronDown, ExternalLink } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { aiService } from '../services/aiService';
import type { ChatMessage, SuggestedRoom } from '../types/ai';

const QUICK_PROMPTS = [
  '⚡ Giá điện nước tính như thế nào?',
  '🐾 Có được nuôi thú cưng không?',
  '📍 Tìm phòng trọ Quận 7 dưới 5 triệu',
  '📑 Thủ tục đặt cọc & hợp đồng ra sao?',
];

export default function ChatbotWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const navigate = useNavigate();
  const location = useLocation();

  // Khởi tạo hoặc lấy sessionId từ sessionStorage
  const [sessionId] = useState<string>(() => {
    const existing = sessionStorage.getItem('smartrental_ai_session');
    if (existing) return existing;
    const newId = 'sess_' + Math.random().toString(36).substring(2, 11) + Date.now().toString(36);
    sessionStorage.setItem('smartrental_ai_session', newId);
    return newId;
  });

  // Scroll xuống cuối tin nhắn
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen]);

  // Tin nhắn chào mừng ban đầu nếu chưa có lịch sử
  useEffect(() => {
    if (messages.length === 0) {
      setMessages([
        {
          id: 'welcome-msg',
          sender: 'bot',
          text: 'Xin chào! Mình là trợ lý ảo SmartRental AI. Bạn cần mình tư vấn về giá thuê, tiện ích phòng, chính sách nuôi thú cưng hay thủ tục ký hợp đồng?',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    }
  }, [messages.length]);

  // Trích xuất roomId từ URL nếu đang ở trang chi tiết phòng
  const extractRoomIdFromPath = (): number | undefined => {
    const match = location.pathname.match(/\/rooms\/([0-9a-fA-F-]+)/);
    if (match && match[1]) {
      return Math.abs(match[1].split('').reduce((acc, char) => acc + char.charCodeAt(0), 0));
    }
    return undefined;
  };

  const handleSendMessage = async (textToSend?: string) => {
    const text = (textToSend || inputMessage).trim();
    if (!text || isLoading) return;

    const userMessage: ChatMessage = {
      id: 'msg-' + Date.now(),
      sender: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMessage]);
    if (!textToSend) setInputMessage('');
    setIsLoading(true);

    try {
      const roomId = extractRoomIdFromPath();
      const response = await aiService.sendChatMessage({
        session_id: sessionId,
        message: text,
        context: {
          room_id: roomId,
          current_page: location.pathname,
        },
      });

      const botMessage: ChatMessage = {
        id: 'bot-' + Date.now(),
        sender: 'bot',
        text: response.reply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        intent: response.intent,
        suggestedRooms: response.suggested_rooms,
      };

      setMessages((prev) => [...prev, botMessage]);
    } catch (error) {
      console.warn('Lỗi kết nối tới AI Chatbot:', error);
      setMessages((prev) => [
        ...prev,
        {
          id: 'bot-err-' + Date.now(),
          sender: 'bot',
          text: 'Rất tiếc, đường truyền AI đang tạm thời gián đoạn. Bạn thử lại sau ít giây nhé!',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetChat = () => {
    const newSession = 'sess_' + Math.random().toString(36).substring(2, 11) + Date.now().toString(36);
    sessionStorage.setItem('smartrental_ai_session', newSession);
    setMessages([
      {
        id: 'welcome-reset',
        sender: 'bot',
        text: 'Cuộc trò chuyện đã được làm mới. Bạn cần SmartRental AI hỗ trợ thêm thông tin gì?',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
  };

  const handleNavigateRoom = (room: number | SuggestedRoom) => {
    if (typeof room === 'object' && room.room_id) {
      navigate(`/rooms/${room.room_id}`);
    } else if (typeof room === 'object' && room.district) {
      navigate(`/rooms?district=${encodeURIComponent(room.district)}`);
    } else {
      navigate('/rooms');
    }
    setIsOpen(false);
  };

  return (
    <div style={{ position: 'fixed', bottom: '24px', right: '24px', zIndex: 9999 }}>
      {/* Nút bấm tròn mở/đóng Chatbot */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            background: 'linear-gradient(135deg, #2563eb 0%, #4f46e5 100%)',
            color: '#fff',
            border: 'none',
            borderRadius: '9999px',
            padding: '14px 22px',
            fontSize: '15px',
            fontWeight: 600,
            cursor: 'pointer',
            boxShadow: '0 10px 25px -5px rgba(37, 99, 235, 0.4), 0 8px 10px -6px rgba(37, 99, 235, 0.3)',
            transition: 'all 0.25s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.transform = 'translateY(-3px)')}
          onMouseLeave={(e) => (e.currentTarget.style.transform = 'translateY(0)')}
        >
          <Sparkles size={20} color="#fbbf24" />
          <span>Hỏi SmartRental AI</span>
        </button>
      )}

      {/* Cửa sổ Chatbot dạng Modal */}
      {isOpen && (
        <div
          style={{
            width: '380px',
            maxWidth: 'calc(100vw - 32px)',
            height: '560px',
            maxHeight: 'calc(100vh - 100px)',
            background: '#ffffff',
            borderRadius: '20px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            border: '1px solid #e2e8f0',
            animation: 'fadeInUp 0.2s ease',
          }}
        >
          {/* Header */}
          <div
            style={{
              background: 'linear-gradient(135deg, #1e40af 0%, #3b82f6 100%)',
              color: '#ffffff',
              padding: '16px 20px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div
                style={{
                  width: '38px',
                  height: '38px',
                  borderRadius: '50%',
                  background: 'rgba(255, 255, 255, 0.2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Bot size={22} color="#ffffff" />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '15px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  SmartRental AI
                  <span
                    style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '50%',
                      background: '#4ade80',
                      display: 'inline-block',
                    }}
                  />
                </div>
                <div style={{ fontSize: '12px', opacity: 0.85 }}>Tư vấn phòng trọ thông minh 24/7</div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                onClick={handleResetChat}
                title="Làm mới đoạn chat"
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#ffffff',
                  cursor: 'pointer',
                  padding: '6px',
                  borderRadius: '6px',
                  opacity: 0.85,
                }}
              >
                <RotateCcw size={17} />
              </button>
              <button
                onClick={() => setIsOpen(false)}
                title="Thu nhỏ"
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#ffffff',
                  cursor: 'pointer',
                  padding: '6px',
                  borderRadius: '6px',
                  opacity: 0.85,
                }}
              >
                <ChevronDown size={20} />
              </button>
            </div>
          </div>

          {/* Body tin nhắn */}
          <div
            style={{
              flex: 1,
              overflowY: 'auto',
              padding: '16px',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
              background: '#f8fafc',
            }}
          >
            {messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                }}
              >
                <div
                  style={{
                    maxWidth: '82%',
                    padding: '12px 16px',
                    borderRadius:
                      msg.sender === 'user' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                    background: msg.sender === 'user' ? '#2563eb' : '#ffffff',
                    color: msg.sender === 'user' ? '#ffffff' : '#1e293b',
                    fontSize: '14px',
                    lineHeight: '1.5',
                    boxShadow: '0 2px 6px rgba(0,0,0,0.06)',
                    border: msg.sender === 'user' ? 'none' : '1px solid #e2e8f0',
                    whiteSpace: 'pre-wrap',
                  }}
                >
                  {msg.text}

                  {/* Card phòng gợi ý đính kèm nếu có */}
                  {msg.suggestedRooms && msg.suggestedRooms.length > 0 && (
                    <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      <div style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>
                        Phòng trọ phù hợp gợi ý:
                      </div>
                      {msg.suggestedRooms.map((room, idx) => {
                        const isNum = typeof room === 'number';
                        const roomId = isNum ? room : room.room_id;
                        const roomTitle = isNum ? `Phòng trọ đề xuất #${room}` : room.title;
                        const subInfo = isNum ? 'Bấm để tìm kiếm trên hệ thống' : `${room.price.toLocaleString('vi-VN')} đ/tháng • ${room.district}`;
                        return (
                          <div
                            key={roomId || idx}
                            onClick={() => handleNavigateRoom(room)}
                            style={{
                              background: '#f1f5f9',
                              borderRadius: '10px',
                              padding: '10px 12px',
                              cursor: 'pointer',
                              border: '1px solid #cbd5e1',
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              transition: 'all 0.15s ease',
                            }}
                            onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#2563eb')}
                            onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#cbd5e1')}
                          >
                            <div>
                              <div style={{ fontWeight: 600, fontSize: '13px', color: '#1e293b' }}>
                                {roomTitle}
                              </div>
                              <div style={{ fontSize: '12px', color: '#dc2626', fontWeight: 700, marginTop: '2px' }}>
                                {subInfo}
                              </div>
                            </div>
                            <ExternalLink size={15} color="#2563eb" />
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
                <span style={{ fontSize: '11px', color: '#94a3b8', marginTop: '4px', padding: '0 4px' }}>
                  {msg.timestamp}
                </span>
              </div>
            ))}

            {isLoading && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px' }}>
                <span
                  style={{
                    display: 'inline-block',
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    background: '#3b82f6',
                    animation: 'pulse 1s infinite',
                  }}
                />
                <span style={{ fontSize: '13px', color: '#64748b' }}>AI đang soạn phản hồi...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick chips gợi ý câu hỏi */}
          <div
            style={{
              padding: '8px 12px',
              background: '#ffffff',
              borderTop: '1px solid #f1f5f9',
              display: 'flex',
              gap: '6px',
              overflowX: 'auto',
              whiteSpace: 'nowrap',
            }}
          >
            {QUICK_PROMPTS.map((prompt, idx) => (
              <button
                key={idx}
                onClick={() => handleSendMessage(prompt)}
                disabled={isLoading}
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '9999px',
                  padding: '5px 11px',
                  fontSize: '12px',
                  color: '#475569',
                  cursor: 'pointer',
                  flexShrink: 0,
                  transition: 'background 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#eff6ff';
                  e.currentTarget.style.color = '#1d4ed8';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = '#f8fafc';
                  e.currentTarget.style.color = '#475569';
                }}
              >
                {prompt}
              </button>
            ))}
          </div>

          {/* Input gửi tin nhắn */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            style={{
              padding: '12px 16px',
              background: '#ffffff',
              borderTop: '1px solid #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <input
              type="text"
              placeholder="Nhập câu hỏi của bạn..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              disabled={isLoading}
              style={{
                flex: 1,
                padding: '10px 14px',
                borderRadius: '9999px',
                border: '1px solid #cbd5e1',
                fontSize: '14px',
                outline: 'none',
              }}
              onFocus={(e) => (e.target.style.borderColor = '#2563eb')}
              onBlur={(e) => (e.target.style.borderColor = '#cbd5e1')}
            />
            <button
              type="submit"
              disabled={!inputMessage.trim() || isLoading}
              style={{
                background: inputMessage.trim() && !isLoading ? '#2563eb' : '#94a3b8',
                color: '#ffffff',
                border: 'none',
                borderRadius: '50%',
                width: '40px',
                height: '40px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: inputMessage.trim() && !isLoading ? 'pointer' : 'default',
                transition: 'background 0.2s ease',
              }}
            >
              <Send size={18} />
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
