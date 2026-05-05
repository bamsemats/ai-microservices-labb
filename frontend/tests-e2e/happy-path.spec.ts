import { test, expect } from '@playwright/test';

test.describe('AdaptaChat Happy Path', () => {
  test('should register, login, and send a message to AI', async ({ page }) => {
    const timestamp = Date.now();
    const username = `testuser_${timestamp}`;
    const email = `test_${timestamp}@example.com`;
    const password = 'Password123!';

    // 1. Registration
    await page.goto('/register');
    await page.fill('input[placeholder="Username"]', username);
    await page.fill('input[placeholder="Email Address"]', email);
    await page.fill('input[placeholder="Password"]', password);
    await page.click('button:has-text("Initialize Identity")');

    // Should redirect to login
    await expect(page).toHaveURL('/login');

    // 2. Login
    await page.fill('input[placeholder="Email Address"]', email);
    await page.fill('input[placeholder="Password"]', password);
    await page.click('button:has-text("Authenticate")');

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

    await page.click('text=@ai-bot'); // Select AI bot frequency
    await page.fill('input[type="text"]', 'Hello @ai! Are you deterministic?');
    await page.press('input[type="text"]', 'Enter');

    // 4. Verify AI Response (Deterministic Mock)
    const aiBubble = page.locator('.message-bubble.bot').last();
    await expect(aiBubble).toContainText('deterministic mock response', { timeout: 10000 });
  });
});
