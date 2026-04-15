import { request, type RequestResult } from "../../api/api";

export interface Config {
  configId: number;
  projectId: number | null;
  userId: number;
  llmCredentialsId: number;
  modelName: string;
  maxIter: number;
  checkPointInterval: number;
  additionalParams: Record<string, string>;
  createdAt: string;
}

export interface CreateConfigInput {
  projectId: number | null;
  llmCredentialsId: number;
  modelName: string;
  maxIter: number;
  checkPointInterval: number;
  additionalParams: Record<string, string>;
}

export interface UpdateConfigInput {
  modelName: string;
  maxIter: number;
  checkPointInterval: number;
  additionalParams: Record<string, string>;
}

export const apiConfigs = {
  getAllMyConfigs: (): Promise<RequestResult<Config[]>> => {
    return request('/api/configs/me', { method: 'GET' });
  },
  create: (data: CreateConfigInput): Promise<RequestResult<Config>> => {
    return request('/api/configs', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data),
    });
  },
  update: (id: number, data: UpdateConfigInput): Promise<RequestResult<Config>> => {
    return request(`/api/configs/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data),
    });
  },
  delete: (id: number): Promise<RequestResult<void>> => {
    return request(`/api/configs/${id}`, { method: 'DELETE' });
  }
};
