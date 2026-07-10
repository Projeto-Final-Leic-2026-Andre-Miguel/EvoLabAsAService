import type { Page, Route } from '@playwright/test';

type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

export const testUser = {
  id: 1,
  name: 'Test User',
  email: 'test@example.com',
  passwordHash: null,
  authProvider: 'LOCAL',
  providerId: null,
  createdAt: '2026-01-01T00:00:00Z',
};

export const sampleCredentials = [
  {
    id: 11,
    userId: 1,
    llm: 'OPENAI',
    createdAt: '2026-01-02T00:00:00Z',
  },
];

export const sampleConfigs = [
  {
    configId: 21,
    projectId: null,
    userId: 1,
    llmCredentialsId: 11,
    modelName: 'gpt-4.1-mini',
    maxIter: 10,
    checkPointInterval: 5,
    additionalParams: {},
    createdAt: '2026-01-03T00:00:00Z',
  },
];

export const sampleProjects = [
  {
    id: 31,
    userId: 1,
    configId: 21,
    name: 'Sorting Experiment',
    description: 'Improve sorting fitness.',
    initialProgram: 'def main():\n    return []',
    evaluatorCode: "def evaluate(code):\n    return {'combined_score': 1}",
    status: 'CREATED',
    createdAt: '2026-01-04T00:00:00Z',
  },
];

async function fulfillJson(route: Route, status: number, body: JsonValue): Promise<void> {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

export async function mockAuthenticatedApi(page: Page): Promise<void> {
  await page.route('**/api/me', route => fulfillJson(route, 200, testUser));
}

export async function mockUnauthenticatedApi(page: Page): Promise<void> {
  await page.route('**/api/me', route =>
    fulfillJson(route, 401, { title: 'Unauthorized', detail: 'Authentication required' }),
  );
}

export async function mockWorkspaceApi(page: Page): Promise<void> {
  await page.route('**/api/projects', async route => {
    if (route.request().method() !== 'POST') {
      return fulfillJson(route, 405, { title: 'Method Not Allowed' });
    }

    const input = route.request().postDataJSON() as { name: string; description?: string | null };
    return fulfillJson(route, 201, {
      ...sampleProjects[0],
      id: 32,
      name: input.name,
      description: input.description ?? null,
    });
  });
  await page.route('**/api/projects/*', route => {
    if (route.request().url().endsWith('/api/projects/me')) {
      return fulfillJson(route, 200, sampleProjects);
    }

    if (route.request().method() === 'DELETE') {
      return fulfillJson(route, 204, null);
    }

    return fulfillJson(route, 200, sampleProjects[0]);
  });
  await page.route('**/api/configs/me', route => fulfillJson(route, 200, sampleConfigs));
  await page.route('**/api/llm-credentials', route => {
    if (route.request().method() === 'GET') {
      return fulfillJson(route, 200, sampleCredentials);
    }

    return fulfillJson(route, 201, {
      id: 12,
      userId: 1,
      llm: 'OPENAI',
      createdAt: '2026-01-05T00:00:00Z',
    });
  });
  await page.route('**/api/llm-credentials/*/validate', route =>
    fulfillJson(route, 200, { isValid: true }),
  );
  await page.route('**/api/llm-credentials/*', route => {
    if (route.request().url().includes('/validate')) {
      return fulfillJson(route, 200, { isValid: true });
    }

    if (route.request().method() === 'DELETE') {
      return fulfillJson(route, 204, null);
    }

    return fulfillJson(route, 200, sampleCredentials[0]);
  });
}

export async function mockLoginSuccess(page: Page): Promise<void> {
  await page.route('**/api/users/token', route => fulfillJson(route, 204, null));
}

export async function mockLoginFailure(page: Page): Promise<void> {
  await page.route('**/api/users/token', route =>
    fulfillJson(route, 401, { title: 'Unauthorized', detail: 'Invalid credentials' }),
  );
}
