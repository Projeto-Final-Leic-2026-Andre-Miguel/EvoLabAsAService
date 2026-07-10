import { expect, test } from '@playwright/test';
import { mockAuthenticatedApi, mockWorkspaceApi } from './helpers/apiMocks';

test.beforeEach(async ({ page }) => {
  await mockAuthenticatedApi(page);
  await mockWorkspaceApi(page);
});

test('shows existing credentials for an authenticated user', async ({ page }) => {
  await page.goto('/credentials');

  await expect(page.getByRole('heading', { name: 'API Credentials' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'OpenAI' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Validate' })).toBeVisible();
});

test('creates an OpenAI credential with the entered secret', async ({ page }) => {
  let createdPayload: unknown;
  await page.route('**/api/llm-credentials', async route => {
    if (route.request().method() === 'GET') {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    }

    createdPayload = route.request().postDataJSON();
    return route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 12,
        userId: 1,
        llm: 'OPENAI',
        createdAt: '2026-01-05T00:00:00Z',
      }),
    });
  });

  await page.goto('/credentials');
  await page.getByRole('button', { name: 'New Credential' }).click();
  await page.getByRole('combobox').selectOption('OPENAI');
  await page.getByPlaceholder('Enter your secret key').fill('sk-test-key');
  await page.getByRole('button', { name: 'Create' }).click();

  await expect(page.getByText('Credential created successfully.')).toBeVisible();
  expect(createdPayload).toEqual({
    llm: 'OPENAI',
    apiKey: 'sk-test-key',
  });
});

test('rejects local model credentials in the frontend without calling the create endpoint', async ({ page }) => {
  let postCount = 0;
  await page.route('**/api/llm-credentials', async route => {
    if (route.request().method() === 'POST') {
      postCount += 1;
    }
    await route.fallback();
  });

  await page.goto('/credentials');
  await page.getByRole('button', { name: 'New Credential' }).click();
  await page.getByRole('combobox').selectOption('LOCAL_MODEL');
  await page.getByPlaceholder('e.g. llama3, qwen2.5-coder:14b').fill('llama3');
  await page.getByPlaceholder('Leave Empty if not needed').fill('local-key');
  await page.getByRole('button', { name: 'Create' }).click();

  await expect(
    page.getByText('Local Llama models are not available in this deployment. Use OpenAI, Gemini or Anthropic instead.'),
  ).toBeVisible();
  expect(postCount).toBe(0);
});
