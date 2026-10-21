import { api } from './api';
import type {
  ChatbotRequest,
  ChatbotResponse,
  FraudCheckRequest,
  FraudCheckResponse,
  MarketOverviewResponse,
  PersonalizedRecommendRequest,
  PricePredictionResponse,
  RecommendationResponse,
} from '../types/ai';

export const aiService = {
  /**
   * Module 2: Kiểm tra gian lận tin đăng (POST /ai/fraud/check)
   */
  async checkListingFraud(data: FraudCheckRequest): Promise<FraudCheckResponse> {
    const res = await api.post<FraudCheckResponse>('/ai/fraud/check', data);
    return res.data;
  },

  /**
   * Module 3: Lấy danh sách gợi ý phòng trọ cho người dùng hiện tại (GET /ai/recommendations)
   */
  async getRecommendations(limit = 10): Promise<RecommendationResponse> {
    const res = await api.get<RecommendationResponse>(`/ai/recommendations?limit=${limit}`);
    return res.data;
  },

  /**
   * Module 3: Lấy gợi ý phòng trọ cá nhân hóa theo ngữ cảnh (POST /ai/recommendations/personalized)
   */
  async getPersonalizedRecommendations(data: PersonalizedRecommendRequest): Promise<RecommendationResponse> {
    const res = await api.post<RecommendationResponse>('/ai/recommendations/personalized', data);
    return res.data;
  },

  /**
   * Module 5: Gửi tin nhắn đến Trợ lý ảo AI Chatbot (POST /ai/chatbot/message)
   */
  async sendChatMessage(data: ChatbotRequest): Promise<ChatbotResponse> {
    const res = await api.post<ChatbotResponse>('/ai/chatbot/message', data);
    return res.data;
  },

  /**
   * Module 5: Lấy lịch sử tin nhắn của session (GET /ai/chatbot/history/{sessionId})
   */
  async getChatHistory(sessionId: string): Promise<Array<{ sender: string; message: string; timestamp: string }>> {
    const res = await api.get<Array<{ sender: string; message: string; timestamp: string }>>(
      `/ai/chatbot/history/${encodeURIComponent(sessionId)}`
    );
    return res.data;
  },

  /**
   * Module 6: Dự báo xu hướng giá thuê của quận (GET /ai/price-prediction)
   */
  async predictPrice(district: string, roomType = 'phòng trọ'): Promise<PricePredictionResponse> {
    const params = new URLSearchParams({
      district,
      roomType,
    });
    const res = await api.get<PricePredictionResponse>(`/ai/price-prediction?${params.toString()}`);
    return res.data;
  },

  /**
   * Module 6: Lấy tổng quan biến động thị trường toàn thành phố (GET /ai/price-prediction/market-overview)
   */
  async getMarketOverview(): Promise<MarketOverviewResponse> {
    const res = await api.get<MarketOverviewResponse>('/ai/price-prediction/market-overview');
    return res.data;
  },
};
