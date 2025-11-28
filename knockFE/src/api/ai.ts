import { apiClient } from './client';

export interface AIChatRequest {
  question: string;
  conversationType?: string;
}

export interface AIChatResponse {
  response: string;
  conversationType: string;
}

export const aiAPI = {
  chat: async (request: AIChatRequest): Promise<AIChatResponse> => {
    const response = await apiClient.post<AIChatResponse>('/ai/chat', request);
    return response.data;
  },
};

