import { apiClient } from './client';

export interface StockDto {
  id: number;
  symbol: string;
  name: string;
  exchange: string;
  country: string;
  currentPrice: number;
  previousClose: number;
  dayHigh: number;
  dayLow: number;
  volume: number;
  marketCap: number;
  peRatio: number;
  dividendYield: number;
}

export const stockAPI = {
  getBySymbol: async (symbol: string): Promise<StockDto> => {
    const response = await apiClient.get<StockDto>(`/stocks/symbol/${symbol}`);
    return response.data;
  },

  search: async (keyword: string): Promise<StockDto[]> => {
    const response = await apiClient.get<StockDto[]>(`/stocks/search?keyword=${keyword}`);
    return response.data;
  },

  getByCountry: async (country: string): Promise<StockDto[]> => {
    const response = await apiClient.get<StockDto[]>(`/stocks/country/${country}`);
    return response.data;
  },
};

