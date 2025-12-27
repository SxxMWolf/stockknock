import { apiClient } from './client';
import type { StockDto } from './stock';

// PortfolioDto interface
export interface PortfolioDto {
  id: number;
  portfolioId: number;
  portfolioName: string;
  stock: StockDto;
  quantity: number;
  avgBuyPrice: number;
  currentPrice: number;
  totalValue: number;
  profitLoss: number;
  profitLossRate: number;
}

export interface PortfolioAnalysisDto {
  totalValue: number;
  totalProfitLoss: number;
  totalProfitLossRate: number;
  analysis: string;
  investmentStyle: string;
}

export const portfolioAPI = {
  getAll: async (): Promise<PortfolioDto[]> => {
    const response = await apiClient.get<PortfolioDto[]>('/portfolio');
    return response.data;
  },

  add: async (stockSymbol: string, quantity: number, avgBuyPrice: number): Promise<PortfolioDto> => {
    console.log('portfolioAPI.add called with:', { stockSymbol, quantity, avgBuyPrice });
    try {
      const response = await apiClient.post<PortfolioDto>('/portfolio', {
        stockSymbol,
        quantity,
        avgBuyPrice
      });
      return response.data;
    } catch (error: any) {
      console.error('Portfolio add error:', error.response?.data || error.message);
      throw error;
    }
  },

  update: async (portfolioId: number, quantity?: number, avgBuyPrice?: number): Promise<PortfolioDto> => {
    const response = await apiClient.put<PortfolioDto>(`/portfolio/${portfolioId}`, null, {
      params: { quantity, avgBuyPrice },
    });
    return response.data;
  },

  delete: async (portfolioId: number): Promise<void> => {
    await apiClient.delete(`/portfolio/${portfolioId}`);
  },

  getAnalysis: async (): Promise<PortfolioAnalysisDto> => {
    const response = await apiClient.get<PortfolioAnalysisDto>('/portfolio/analysis');
    return response.data;
  },
};

