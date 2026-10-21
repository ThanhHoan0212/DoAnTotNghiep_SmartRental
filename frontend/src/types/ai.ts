export interface FraudCheckRequest {
  title: string;
  description: string;
  price: number;
  area: number;
  district: string;
  address: string;
  landlord_id?: number;
  room_type?: string;
}

export interface FraudCheckResponse {
  fraud_score: number;
  risk_level: 'LOW' | 'MEDIUM' | 'HIGH' | string;
  is_fraud: boolean;
  recommendation?: string;
  reasons: string[];
  details?: Record<string, unknown>;
}

export interface RecommendationItem {
  room_id: number;
  score: number;
  reason?: string;
  match_factors?: Record<string, unknown>;
}

export interface RecommendationResponse {
  user_id: number;
  recommendations: RecommendationItem[];
  strategy: string;
}

export interface PersonalizedRecommendRequest {
  user_id?: number;
  viewed_room_ids?: number[];
  favorite_room_ids?: number[];
  preferred_districts?: string[];
  target_room_id?: number;
  limit?: number;
}

export interface ChatbotContext {
  room_id?: number;
  district?: string;
  current_page?: string;
}

export interface ChatbotRequest {
  session_id: string;
  message: string;
  user_id?: number;
  context?: ChatbotContext;
}

export interface SuggestedRoom {
  room_id: number;
  title: string;
  price: number;
  district: string;
}

export interface ChatbotResponse {
  reply: string;
  intent: string;
  confidence: number;
  suggested_rooms?: Array<number | SuggestedRoom>;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: string;
  intent?: string;
  suggestedRooms?: Array<number | SuggestedRoom>;
}

export interface PricePredictionResponse {
  district: string;
  room_type: string;
  current_avg_price: number;
  predicted_next_month: number;
  trend: 'increasing' | 'decreasing' | 'stable';
  confidence_interval: [number, number];
  change_percentage: number;
  factors: string[];
}

export interface DistrictOverview {
  district: string;
  avg_price: number;
  min_price: number;
  max_price: number;
  avg_price_per_m2: number;
  trend: string;
  sample_count: number;
}

export interface MarketOverviewResponse {
  districts: DistrictOverview[];
  generated_at: string;
}
