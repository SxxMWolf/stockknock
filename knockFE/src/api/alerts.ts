import { apiClient } from './client';
import type { StockDto } from './stock';

export interface PriceAlert {
  id: number;
  user: {
    id: number;
    username: string;
    email: string;
  };
  stock: StockDto;
  alertType: 'TARGET' | 'STOP_LOSS' | 'PERCENT';
  targetPrice?: number;
  percentChange?: number;
  triggered: boolean;
  triggeredAt?: string;
  createdAt: string;
}

export interface CreateAlertRequest {
  stockSymbol: string;
  alertType: 'TARGET' | 'STOP_LOSS' | 'PERCENT';
  targetPrice?: number;
  percentageChange?: number;
}

export const alertsAPI = {
  getAll: async (): Promise<PriceAlert[]> => {
    const response = await apiClient.get<PriceAlert[]>('/alerts');
    return response.data;
  },

  create: async (request: CreateAlertRequest): Promise<PriceAlert> => {
    const response = await apiClient.post<PriceAlert>('/alerts', null, {
      params: {
        stockSymbol: request.stockSymbol,
        alertType: request.alertType,
        targetPrice: request.targetPrice,
        percentageChange: request.percentageChange,
      },
    });
    return response.data;
  },

  delete: async (alertId: number): Promise<void> => {
    await apiClient.delete(`/alerts/${alertId}`);
  },
};

