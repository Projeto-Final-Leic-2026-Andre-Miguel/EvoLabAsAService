import { expect, test } from '@playwright/test';
import { mockAuthenticatedApi, mockWorkspaceApi } from './helpers/apiMocks';

test.beforeEach(async ({ page }) => {
  await mockAuthenticatedApi(page);
  await mockWorkspaceApi(page);
});

test('shows the authenticated projects list', async ({ page }) => {
  await page.goto('/projects');

  await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Sorting Experiment' })).toBeVisible();
  await expect(page.getByText('Improve sorting fitness.')).toBeVisible();
  await expect(page.getByText('gpt-4.1-mini')).toBeVisible();
});

test('keeps fitness bars and tooltips clear of the title on a narrow viewport', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await page.route('**/api/projects/31/jobs', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([{
      id: 41,
      projectId: 31,
      status: 'COMPLETED',
      createdAt: '2026-01-06T00:00:00Z',
      completedAt: '2026-01-06T00:01:00Z',
      bestFitness: 100,
      bestSolution: 'return 1',
      executionLogs: 'complete',
    }]),
  }));
  await page.route('**/api/jobs/41/metrics', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([
      { id: 51, jobId: 41, iteration: 1, fitnessScore: -1, executionTime: 1, createdAt: '2026-01-06T00:00:10Z' },
      { id: 52, jobId: 41, iteration: 2, fitnessScore: 0, executionTime: 1, createdAt: '2026-01-06T00:00:20Z' },
      { id: 53, jobId: 41, iteration: 3, fitnessScore: 0.5, executionTime: 1, createdAt: '2026-01-06T00:00:30Z' },
      { id: 54, jobId: 41, iteration: 4, fitnessScore: 100, executionTime: 1, createdAt: '2026-01-06T00:00:40Z' },
    ]),
  }));
  await page.route('**/api/jobs/41/checkpoints', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: '[]',
  }));

  await page.goto('/projects/31');

  const title = page.getByText('Fitness Score per Iteration', { exact: true });
  const maximumBarWrapper = title.locator('..').getByText('#4', { exact: true }).locator('..');
  await expect(title).toBeVisible();
  await expect(maximumBarWrapper).toBeVisible();

  const titleBox = await title.boundingBox();
  const maximumBarBox = await maximumBarWrapper.locator('div').last().boundingBox();
  expect(titleBox).not.toBeNull();
  expect(maximumBarBox).not.toBeNull();
  expect(maximumBarBox!.y).toBeGreaterThan(titleBox!.y + titleBox!.height);

  await maximumBarWrapper.hover();
  const tooltip = maximumBarWrapper.getByText('10000.0%', { exact: true }).locator('..');
  await expect(tooltip).toBeVisible();
  const tooltipBox = await tooltip.boundingBox();
  expect(tooltipBox).not.toBeNull();
  expect(tooltipBox!.x).toBeGreaterThanOrEqual(0);
  expect(tooltipBox!.x + tooltipBox!.width).toBeLessThanOrEqual(360);
});

test('creates a project through the modal and posts the expected payload', async ({ page }) => {
  let createdPayload: unknown;
  await page.route('**/api/projects', async route => {
    createdPayload = route.request().postDataJSON();
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 32,
        userId: 1,
        configId: 21,
        name: 'New Experiment',
        description: 'Created from Playwright',
        initialProgram: 'def main():\n    return 1',
        evaluatorCode: "def evaluate(code):\n    return {'combined_score': 1}",
        status: 'CREATED',
        createdAt: '2026-01-05T00:00:00Z',
      }),
    });
  });

  await page.goto('/projects');
  await page.getByRole('button', { name: 'New Project' }).click();
  await page.getByPlaceholder('My Experiment').fill('New Experiment');
  await page.getByPlaceholder('Describe the purpose of this project...').fill('Created from Playwright');
  await page.getByRole('combobox').selectOption('21');
  await page.getByPlaceholder('def main():\n    pass').fill('def main():\n    return 1');
  await page
    .getByPlaceholder('def evaluate(code):\n    return fitness_score')
    .fill("def evaluate(code):\n    return {'combined_score': 1}");
  await page.getByRole('button', { name: 'Save Project' }).click();

  await expect(page.getByText('Project created successfully.')).toBeVisible();
  expect(createdPayload).toEqual({
    name: 'New Experiment',
    description: 'Created from Playwright',
    configId: 21,
    initialProgram: 'def main():\n    return 1',
    evaluatorCode: "def evaluate(code):\n    return {'combined_score': 1}",
  });
});

test('disables start for projects missing required run inputs', async ({ page }) => {
  await page.route('**/api/projects/me', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 40,
          userId: 1,
          configId: null,
          name: 'Incomplete Experiment',
          description: null,
          initialProgram: null,
          evaluatorCode: null,
          status: 'CREATED',
          createdAt: '2026-01-04T00:00:00Z',
        },
      ]),
    }),
  );

  await page.goto('/projects');

  await expect(page.getByRole('heading', { name: 'Incomplete Experiment' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Start' })).toBeDisabled();
});
