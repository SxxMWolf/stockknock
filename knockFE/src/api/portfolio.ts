import { apiClient } from './client';
import { StockDto } from './stock';

export interface PortfolioDto {
  id: number;
  stock: StockDto;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  totalValue: number;
  profitLoss: number;
  profitLossRate: number;
}

export const portfolioAPI = {
  getAll: async (): Promise<PortfolioDto[]> => {
    const response = await apiClient.get<PortfolioDto[]>('/portfolio');
    return response.data;
  },

  add: async (stockSymbol: string, quantity: number, averagePrice: number): Promise<PortfolioDto> => {
    const response = await apiClient.post<PortfolioDto>('/portfolio', null, {
      params: { stockSymbol, quantity, averagePrice },
    });
    return response.data;
  },

  update: async (portfolioId: number, quantity?: number, averagePrice?: number): Promise<PortfolioDto> => {
    const response = await apiClient.put<PortfolioDto>(`/portfolio/${portfolioId}`, null, {
      params: { quantity, averagePrice },
    });
    return response.data;
  },

  delete: async (portfolioId: number): Promise<void> => {
    await apiClient.delete(`/portfolio/${portfolioId}`);
  },
};

