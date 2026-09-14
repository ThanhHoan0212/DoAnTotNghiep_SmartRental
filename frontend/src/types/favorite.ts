export interface ToggleFavoriteResponse {
  roomId: string;
  isFavorited?: boolean;
  favorited?: boolean;
  message: string;
  totalFavorites: number;
}

export interface FavoriteStatusResponse {
  roomId: string;
  isFavorited?: boolean;
  favorited?: boolean;
  totalFavorites: number;
}

export interface SearchHistoryItem {
  id: string;
  queryText?: string;
  district?: string;
  minPrice?: number;
  maxPrice?: number;
  searchedAt: string;
}

export interface SaveSearchHistoryRequest {
  queryText?: string;
  district?: string;
  minPrice?: number;
  maxPrice?: number;
}
