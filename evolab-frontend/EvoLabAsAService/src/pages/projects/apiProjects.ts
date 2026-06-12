import { request, type RequestResult } from "../../api/api";

export interface Project {
  id: number;
  userId: number;
  configId: number | null;
  name: string;
  description: string | null;
  initialProgram: string | null;
  evaluatorCode: string | null;
  status: 'CREATED' | 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
}

export interface CreateProjectInput {
  name: string;
  description: string | null;
  configId: number | null;
  initialProgram: string | null;
  evaluatorCode: string | null;
}

export interface UpdateProjectDetailsInput {
  name?: string | null;
  description?: string | null;
  configId?: number | null;
  initialProgram?: string | null;
  evaluatorCode?: string | null;
}

export const apiProjects = {
  getAll: (): Promise<RequestResult<Project[]>> => {
    return request('/api/projects/me', { method: 'GET' });
  },
  getById: (id: number): Promise<RequestResult<Project>> => {
    return request(`/api/projects/${id}`, { method: 'GET' });
  },
  create: (data: CreateProjectInput): Promise<RequestResult<Project>> => {
    return request('/api/projects', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data),
    });
  },
  update: (id: number, data: UpdateProjectDetailsInput): Promise<RequestResult<Project>> => {
    return request(`/api/projects/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data),
    });
  },
  delete: (id: number): Promise<RequestResult<string>> => {
    return request(`/api/projects/${id}`, { method: 'DELETE' });
  },
  start: (id: number): Promise<RequestResult<Project>> => {
    return request(`/api/projects/${id}/start`, { method: 'POST' });
  },
  restart: (id: number): Promise<RequestResult<Project>> => {
    return request(`/api/projects/${id}/restart`, { method: 'POST' });
  }
};
