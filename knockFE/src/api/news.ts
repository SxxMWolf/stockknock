import { apiClient } from './client';

export interface NewsAnalysisDto {
  summary: string;
  impactAnalysis: string;
  sentiment: string;
  impactScore: number;
}

export interface NewsDto {
  id: number;
  title: string;
  content: string;
  source: string;
  url: string;
  publishedAt: string;
  relatedStockSymbols?: string[];
  analysis?: NewsAnalysisDto;
}

export const newsAPI = {
  getRecent: async (days: number = 7): Promise<NewsDto[]> => {
    const response = await apiClient.get<NewsDto[]>(`/news/recent?days=${days}`);
    return response.data;
  },

  getById: async (newsId: number): Promise<NewsDto> => {
    const response = await apiClient.get<NewsDto>(`/news/${newsId}`);
    return response.data;
  },

  analyze: async (newsId: number): Promise<NewsAnalysisDto> => {
    const response = await apiClient.post<NewsAnalysisDto>(`/news/${newsId}/analyze`);
    return response.data;
  },

  getTodaySummary: async (): Promise<string> => {
    const response = await apiClient.get<{ summary: string }>('/news/today-summary');
    return response.data.summary;
  },

  getMarketBriefing: async (): Promise<string> => {
    const response = await apiClient.get<{ summary: string }>('/news/market-briefing');
    return response.data.summary;
  },
};

