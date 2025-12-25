import { fastApiClient } from './fastApiClient';
import { apiClient } from './client';

// StockDto interface (Spring Boot에서 가져옴)
export interface StockDto {
  symbol: string;
  name: string;
  exchange: string;
  country: string;
  industry: string;
  currency: string;
  currentPrice: number;
  previousClose: number;
  dayHigh: number;
  dayLow: number;
  volume: number;
  lastUpdated?: string; // ISO 8601 형식의 날짜 문자열
}

// FastAPI StockPriceResponse
export interface StockPriceResponse {
  symbol: string;
  price: number;
  open?: number;
  high?: number;
  low?: number;
  volume?: number;
  timestamp: string;
}

export const stockAPI = {
  // Spring Boot에서 종목 정보 조회
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

  getByIndustry: async (industry: string): Promise<StockDto[]> => {
    const response = await apiClient.get<StockDto[]>(`/stocks/industry/${industry}`);
    return response.data;
  },

  // FastAPI에서 주가 조회
  getCurrentPrice: async (symbol: string): Promise<StockPriceResponse> => {
    const response = await fastApiClient.get<StockPriceResponse>(`/api/stock/${symbol}/price`);
    return response.data;
  },

  updatePrice: async (symbol: string): Promise<void> => {
    await fastApiClient.post(`/api/stock/${symbol}/update`);
  },
};

