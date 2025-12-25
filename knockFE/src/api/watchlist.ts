import { apiClient } from './client';
import type { StockDto } from './stock';

export interface WatchlistItem {
  userId: number;
  stockSymbol: string;
  stock: StockDto;
  createdAt: string;
}

export const watchlistAPI = {
  getAll: async (): Promise<StockDto[]> => {
    const response = await apiClient.get<StockDto[]>('/watchlist');
    return response.data;
  },

  add: async (stockSymbol: string): Promise<void> => {
    await apiClient.post(`/watchlist/${stockSymbol}`);
  },

  remove: async (stockSymbol: string): Promise<void> => {
    await apiClient.delete(`/watchlist/${stockSymbol}`);
  },
};

