package com.amazon.pages;

import com.amazon.utils.DriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AmazonPage — ONE class with all locators and the complete flow:
 *
 * Step 1 → Navigate to amazon.com
 * Step 2 → Type keyword in search box, click Search
 * Step 3 → Click first matching product (opens in NEW WINDOW)
 * Step 4 → Switch to child window
 * Step 5 → Click Add to Cart
 * Step 6 → Fetch price from cart subtotal
 * Step 7 → Close child window → back to parent window
 *
 * The search keyword is passed as a method parameter — nothing is hardcoded.
 */
public class AmazonPage {

    private static final Logger log = LoggerFactory.getLogger(AmazonPage.class);

    // ──────────────────────────────────────────────
    // Locators (exactly as you provided)
    // ──────────────────────────────────────────────

    // Step 2 — Search bar
    private static final By SEARCH_BOX = By.xpath("//input[@id='twotabsearchtextbox']");

    // Step 2 — Search submit button
    private static final By SEARCH_BUTTON = By.xpath("//input[@id='nav-search-submit-button']");

    // Step 3 — First product whose title contains the keyword AND "Mobile"
    // Built dynamically so "iPhone" or "Galaxy" can be passed in
    private static By firstProduct(String keyword) {
        return By.xpath("(//a[contains(., '" + keyword + "') and contains(., 'Mobile')])[1]");
    }

    // Step 5 — Add to Cart button inside the buy box (child window)
    private static final By ADD_TO_CART = By
            .xpath("//div[@id='desktop_qualifiedBuyBox']//input[@id='add-to-cart-button']");

    // Step 6 — Cart subtotal price element (child window, after adding to cart)
    private static final By CART_PRICE = By.xpath("//*[@id='sw-subtotal']");

    // ──────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────

    private final DriverUtils d;

    public AmazonPage(DriverUtils driverUtils) {
        this.d = driverUtils;
    }

    // ──────────────────────────────────────────────
    // Main Flow
    // ──────────────────────────────────────────────

    /**
     * Complete end-to-end flow for one keyword.
     *
     * @param keyword e.g. "iPhone" or "Galaxy"
     * @return the price text shown in the cart subtotal
     */
    public String searchAndAddToCart(String keyword) {

        log.info("=== Flow START  |  keyword: '{}' ===", keyword);

        // ── Step 1: Navigate to amazon.in ────────────────────────────
        d.navigateTo("https://www.amazon.in");

        // ── Step 2: Wait for search box to confirm page is ready ─────
        // Extra explicit wait for slow CI network before interacting
        d.getWait().until(ExpectedConditions.visibilityOfElementLocated(SEARCH_BOX));
        log.info("amazon.in loaded successfully");

        // ── Step 3: Search ───────────────────────────────────────────
        log.info("Typing '{}' into search box", keyword);
        d.safeType(SEARCH_BOX, keyword);
        d.safeClick(SEARCH_BUTTON);

        // ── Step 4: Click first matching product (opens new window) ──
        log.info("Clicking first product for keyword '{}'", keyword);
        d.safeClick(firstProduct(keyword));

        // ── Step 5: Switch to child window ───────────────────────────
        log.info("Switching to child window");
        d.switchToChildWindow();

        // ── Step 6: Add to Cart ──────────────────────────────────────
        log.info("Clicking Add to Cart");
        d.safeClick(ADD_TO_CART);

        // ── Step 7: Fetch price ──────────────────────────────────────
        log.info("Fetching cart subtotal price");
        String price = d.safeGetText(CART_PRICE);
        log.info(">>> Price for '{}' = {}", keyword, price);
        System.out.println("[PRICE] " + keyword + " → " + price);

        // ── Step 8: Close child, back to parent ──────────────────────
        log.info("Closing child window and returning to parent");
        d.closeChildAndSwitchToParent();

        log.info("=== Flow END  |  keyword: '{}' ===", keyword);
        return price;
    }
}