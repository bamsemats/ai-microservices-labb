import { test, expect } from '@playwright/test';

test.describe('AdaptaChat Happy Path', () => {
  test('should register, login, and send a message to AI', async ({ page }) => {
    const timestamp = Date.now();
    const username = `testuser_${timestamp}`;
    const email = `test_${timestamp}@example.com`;
    const password = 'Password123!';

    // 1. Registration
    await page.goto('/register');
    await page.getByLabel('Username').fill(username);
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.click('button:has-text("Register")');

    // Should redirect to login
    await expect(page).toHaveURL('/login', { timeout: 10000 });

    // 2. Login
    await page.getByLabel('Username').fill(username);
    await page.getByLabel('Password').fill(password);
    await page.click('button:has-text("Login")');

    // Should redirect to chat
    await expect(page).toHaveURL('/');
    await expect(page.locator('.user-badge')).toContainText(username);

    // 3. Send message to AI in Test Mode
    // We inject the header via a route override
    await page.route('**/messages', async (route) => {
      const headers = route.request().headers();
      await route.continue({
        headers: {
          ...headers,
          'X-Adapta-Test-Mode': 'true',
        },
      });
    });

    await page.click('text=AdaptaAI'); // Select AdaptaAI bot
    await page.fill('.input-wrapper input', `Hello AdaptaAI! Are you deterministic?`);
    await page.press('.input-wrapper input', 'Enter');

    // 4. Verify AI Response (Deterministic Mock)
    const aiBubble = page.locator('.message-bubble.bot').last();
    await expect(aiBubble).toContainText('Deterministic mock response', { timeout: 15000 });
  });
});
