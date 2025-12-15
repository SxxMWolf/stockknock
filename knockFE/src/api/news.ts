import { fastApiClient } from './fastApiClient';

export interface NewsAnalysisDto {
  summary: string;
  sentiment: string;
  impact_score: number;
  ai_comment: string;
}

export interface NewsDto {
  id: number;
  title: string;
  content: string;
  source: string;
  url: string;
  published_at: string;
  related_stock_symbols?: string[];
  analysis?: NewsAnalysisDto;
}

export const newsAPI = {
  getRecent: async (days: number = 7): Promise<NewsDto[]> => {
    const response = await fastApiClient.get<NewsDto[]>(`/api/news/recent?days=${days}`);
    return response.data;
  },

  getById: async (newsId: number): Promise<NewsDto> => {
    const response = await fastApiClient.get<NewsDto>(`/api/news/${newsId}`);
    return response.data;
  },

  analyze: async (newsId: number): Promise<NewsAnalysisDto> => {
    const response = await fastApiClient.post<NewsAnalysisDto>(`/api/news/analyze/${newsId}`);
    return response.data;
  },
};

