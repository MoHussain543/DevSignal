import { expect, test } from '@playwright/test'

test('landing page shows the main DevSignal experience', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: /turn github profiles into hiring-ready insights/i })).toBeVisible()
  await expect(page.getByRole('button', { name: /analyze a profile/i })).toBeVisible()
  await expect(page.getByRole('heading', { name: /what we analyze/i })).toBeVisible()
})

test('signed-out user clicking AI report CTA is sent to auth', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('link', { name: /try ai report/i }).click()

  await expect(page).toHaveURL(/\/auth$/)
  await expect(page.getByRole('heading', { name: /save your latest devsignal state and come back to it later/i })).toBeVisible()
  await expect(page.locator('form').getByRole('button', { name: /^sign in$/i })).toBeVisible()
})
