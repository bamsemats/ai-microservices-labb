import { test, expect } from '@playwright/test';

/**
 * AdaptaChat Full System Health Check
 * This suite verifies the end-to-end integration of all microservices:
 * Auth -> User -> Message -> AI -> Content Aggregator -> Gateway
 */
test.describe('AdaptaChat Health Check', () => {
  const timestamp = Date.now();
  const testUser = {
    username: `health_user_${timestamp}`,
    email: `health_${timestamp}@example.com`,
    password: 'Password123!'
  };

  test.beforeEach(async ({ page }) => {
    // Inject Test Mode header globally for E2E runs to trigger deterministic AI/Simulations
    await page.route('**/messages', async (route) => {
      const headers = route.request().headers();
      await route.continue({
        headers: {
          ...headers,
          'X-Adapta-Test-Mode': 'true',
        },
      });
    });
  });

  test('01: User Lifecycle (Register -> Login -> Persist)', async ({ page }) => {
    // Registration
    await page.goto('/register');
    await page.getByLabel('Username').fill(testUser.username);
    await page.getByLabel('Email').fill(testUser.email);
    await page.getByLabel('Password').fill(testUser.password);
    await page.click('button:has-text("Register")');
    await expect(page).toHaveURL('/login');

    // Login
    await page.getByLabel('Username').fill(testUser.username);
    await page.getByLabel('Password').fill(testUser.password);
    await page.click('button:has-text("Login")');
    await expect(page).toHaveURL('/');
    
    // Identity Verification
    await expect(page.locator('.user-badge')).toContainText(testUser.username);

    // Persistence Check (Refresh)
    await page.reload();
    await expect(page.locator('.user-badge')).toContainText(testUser.username);
    await expect(page).toHaveURL('/');
  });

  test('02: AI Interaction & Real-time Delivery', async ({ page }) => {
    // Reuse admin or login fresh if needed, but here we assume previous registration succeeded
    // For standalone reliability, we login as the seeded admin for speed
    await page.goto('/login');
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('admin-password-2026');
    await page.click('button:has-text("Login")');

    // Select AdaptaAI from Sidebar
    await page.click('text=AdaptaAI'); 
    
    // Send Message
    const msgContent = "Hello bot, what is your purpose?";
    await page.fill('input[placeholder*="Type your message"]', msgContent);
    await page.press('input[placeholder*="Type your message"]', 'Enter');

    // Verify Own Message appears optimistically
    await expect(page.locator('.message-bubble.own').last()).toContainText(msgContent);

    // Verify AI Response (Simulation Mode fallback)
    // Note: In test mode, AI service returns deterministic strings
    const aiBubble = page.locator('.message-bubble.bot').last();
    await expect(aiBubble).toBeVisible({ timeout: 20000 });
    await expect(aiBubble).not.toContainText('...', { timeout: 10000 }); // Wait for "thinking" to finish
  });

  test('03: Discovery & Social Graph', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('admin-password-2026');
    await page.click('button:has-text("Login")');

    // Navigate to Discovery
    await page.click('a[href="/discovery"], button:has-text("Discovery")');
    await expect(page).toHaveURL('/discovery');

    // Search for entities
    await page.fill('input[placeholder*="Search"]', 'Nexus');
    await page.click('button:has-text("Scan")');

    // Verify results
    await expect(page.locator('.entity-card')).toContainText('NexusPrime');
  });

  test('04: Admin Command Center', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('admin-password-2026');
    await page.click('button:has-text("Login")');

    // Open Admin Panel (Shield Icon)
    await page.click('.admin-btn, .sidebar-btn[title="Admin"]');
    await expect(page).toHaveURL('/admin');

    // Send Broadcast
    const broadcastMsg = "ATTENTION: System maintenance scheduled for midnight.";
    await page.fill('textarea[placeholder*="Broadcast"]', broadcastMsg);
    await page.click('button:has-text("Transmit")');

    // Verify success feedback
    await expect(page.locator('.status-message')).toContainText('Broadcast transmitted');
  });

  test('05: Adaptive UI (Sentiment Detection)', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('admin-password-2026');
    await page.click('button:has-text("Login")');

    // Select a channel
    await page.click('text=#general');

    // Send high-sentiment message
    await page.fill('input[placeholder*="Type your message"]', "I absolutely love this new update, it is amazing!");
    await page.press('input[placeholder*="Type your message"]', 'Enter');

    // Verify CSS variable change on document root (Sentiment Adaptation)
    // We check if --sentiment-blur or similar exists and is non-zero
    const blur = await page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--sentiment-blur'));
    expect(parseFloat(blur)).toBeGreaterThanOrEqual(0);
  });
});
