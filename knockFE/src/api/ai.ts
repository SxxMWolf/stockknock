import { fastApiClient } from './fastApiClient';

export interface AIChatRequest {
  question: string;
  conversationType?: string;
  user_id: number;
  conversation_history?: string;
}

export interface AIChatResponse {
  response: string;
  conversation_type?: string;
}

export const aiAPI = {
  chat: async (request: AIChatRequest): Promise<AIChatResponse> => {
    const response = await fastApiClient.post<AIChatResponse>('/api/ai/chat', request);
    return response.data;
  },
};

