import { api, requestBlob } from './api';

export interface Conversation {
  id: string;
  roomId?: string | null;
  roomTitle?: string | null;
  otherUserId: string;
  otherUserName: string;
  otherUserEmail?: string | null;
  otherUserAvatarUrl?: string | null;
  otherUserRole?: string;
  otherUserIdentityVerified?: boolean;
  otherUserCreatedAt?: string | null;
  updatedAt: string;
}
export interface ChatMessage {
  recalled?: boolean;
  revision?: number;
  reactions?: { userId: string; emoji: string }[];
  hasImage?: boolean;
  hasVideo?: boolean;
  id: string;
  senderId: string;
  content: string;
  sentAt: string;
}
export const chatService = {
  async recall(id: string, messageId: string) {
    return (await api.post<ChatMessage>(`/chat/conversations/${id}/messages/${messageId}/recall`)).data;
  },
  async react(id: string, messageId: string, emoji: string) {
    return (await api.post<ChatMessage>(`/chat/conversations/${id}/messages/${messageId}/reaction`, { emoji })).data;
  },
  async sharedMedia(id: string, page = 0) {
    return (await api.get<{items: ChatMessage[]; hasMore: boolean}>(`/chat/conversations/${id}/media?page=${page}`)).data;
  },
  async sendVideo(id: string, video: File, caption: string) {
    const body = new FormData(); body.append('video', video); body.append('caption', caption);
    return (await api.post<ChatMessage>(`/chat/conversations/${id}/videos`, body)).data;
  },
  video(id: string, messageId: string) { return requestBlob(`/chat/conversations/${id}/messages/${messageId}/video`); },
  async sharedImages(id: string, page = 0) {
    return (await api.get<{items: ChatMessage[]; hasMore: boolean}>(`/chat/conversations/${id}/images?page=${page}`)).data;
  },
  async sendImage(id: string, image: File, caption: string) {
    const body = new FormData(); body.append('image', image); body.append('caption', caption);
    return (await api.post<ChatMessage>(`/chat/conversations/${id}/images`, body)).data;
  },
  async image(id: string, messageId: string) {
    return (await api.get<{contentType: string; data: string}>(`/chat/conversations/${id}/messages/${messageId}/image`)).data;
  },
  async list() { return (await api.get<Conversation[]>('/chat/conversations')).data; },
  async start(roomId: string) { return (await api.post<Conversation>(`/chat/rooms/${roomId}`)).data; },
  async history(id: string, page = 0) {
    return (await api.get<ChatMessage[]>(`/chat/conversations/${id}/messages?page=${page}`)).data;
  },
  async send(id: string, content: string) {
    return (await api.post<ChatMessage>(`/chat/conversations/${id}/messages`, { content })).data;
  },
};
