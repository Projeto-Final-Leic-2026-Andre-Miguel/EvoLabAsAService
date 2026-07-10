import { expect, test } from '@playwright/test';
import {
  mockLoginFailure,
  mockLoginSuccess,
  mockUnauthenticatedApi,
  testUser,
} from './helpers/apiMocks';

test('shows an authentication required state for protected routes without a session', async ({ page }) => {
  await mockUnauthenticatedApi(page);

  await page.goto('/projects');

  await expect(page.getByRole('heading', { name: 'Authentication required' })).toBeVisible();
  await page.getByRole('button', { name: 'Go to login' }).click();
  await expect(page).toHaveURL(/\/login$/);
});

test('validates malformed login email before calling the API', async ({ page }) => {
  await mockUnauthenticatedApi(page);

  await page.goto('/login');
  await page.getByLabel('Email:').fill('a@b.c');
  await page.getByLabel('Password:').fill('password123');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();

  await expect(page.getByText('Please enter a valid email address.')).toBeVisible();
});

test('shows the login failure message returned by a 401 response', async ({ page }) => {
  await mockUnauthenticatedApi(page);
  await mockLoginFailure(page);

  await page.goto('/login');
  await page.getByLabel('Email:').fill('test@example.com');
  await page.getByLabel('Password:').fill('wrong-password');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();

  await expect(page.getByText('Invalid email or password. Please try again.')).toBeVisible();
});

test('logs in and opens the home page when the session reload succeeds', async ({ page }) => {
  let sessionChecks = 0;
  await page.route('**/api/me', route => {
    sessionChecks += 1;
    if (sessionChecks === 1) {
      return route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ title: 'Unauthorized' }),
      });
    }

    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(testUser),
    });
  });
  await mockLoginSuccess(page);

  await page.goto('/login');
  await page.getByLabel('Email:').fill('test@example.com');
  await page.getByLabel('Password:').fill('correct-password');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();

  await expect(page).toHaveURL('/');
});
