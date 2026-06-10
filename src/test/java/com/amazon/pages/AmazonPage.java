package com.amazon.pages;

import com.amazon.utils.DriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class AmazonPage {

    private static final Logger log = LoggerFactory.getLogger(AmazonPage.class);

    // ── Locators ─────────────────────────────────────────────────
    private static final By SEARCH_BOX       = By.xpath("//input[@id='twotabsearchtextbox']");
    private static final By SEARCH_BUTTON    = By.xpath("//input[@id='nav-search-submit-button']");
    private static final By ADD_TO_CART      = By.xpath("//div[@id='desktop_qualifiedBuyBox']//input[@id='add-to-cart-button']");
    private static final By CART_PRICE       = By.xpath("//*[@id='sw-subtotal']");

    // ── "Continue shopping" interstitial button ───────────────────
    private static final By CONTINUE_SHOPPING = By.xpath("//input[@value='Continue shopping'] | //button[contains(text(),'Continue shopping')] | //a[contains(text(),'Continue shopping')]");

    private static By firstProduct(String keyword) {
        return By.xpath("(//a[contains(., '" + keyword + "') and contains(., 'Mobile')])[1]");
    }

    // ── Read base URL from env variable, default to amazon.in ────
    private static String getBaseUrl() {
        String url = System.getenv("AMAZON_URL");
        return (url != null && !url.isEmpty()) ? url : "https://www.amazon.in";
    }

    private final DriverUtils d;

    public AmazonPage(DriverUtils driverUtils) {
        this.d = driverUtils;
    }

    public String searchAndAddToCart(String keyword) {

        log.info("=== Flow START  |  keyword: '{}' ===", keyword);

        // ── Step 1: Navigate ─────────────────────────────────────
        String baseUrl = getBaseUrl();
        log.info("Using base URL: {}", baseUrl);
        d.navigateTo(baseUrl);

        // ── Step 2: Handle "Continue shopping" interstitial ──────
        // Amazon shows this blocking page on CI server IPs
        handleContinueShoppingPage();

        // ── Step 3: Wait for search box ──────────────────────────
        d.getWait().until(ExpectedConditions.visibilityOfElementLocated(SEARCH_BOX));
        log.info("Homepage loaded, search box visible");

        // ── Step 4: Search ───────────────────────────────────────
        log.info("Typing '{}' into search box", keyword);
        d.safeType(SEARCH_BOX, keyword);
        d.safeClick(SEARCH_BUTTON);

        // ── Step 5: Click first matching product ─────────────────
        log.info("Clicking first product for keyword '{}'", keyword);
        d.safeClick(firstProduct(keyword));

        // ── Step 6: Switch to child window ───────────────────────
        log.info("Switching to child window");
        d.switchToChildWindow();

        // ── Step 7: Add to Cart ──────────────────────────────────
        log.info("Clicking Add to Cart");
        d.safeClick(ADD_TO_CART);

        // ── Step 8: Fetch price ──────────────────────────────────
        log.info("Fetching cart subtotal price");
        String price = d.safeGetText(CART_PRICE);
        log.info(">>> Price for '{}' = {}", keyword, price);
        System.out.println("[PRICE] " + keyword + " → " + price);

        // ── Step 9: Close child, back to parent ──────────────────
        log.info("Closing child window and returning to parent");
        d.closeChildAndSwitchToParent();

        log.info("=== Flow END  |  keyword: '{}' ===", keyword);
        return price;
    }

    // ──────────────────────────────────────────────────────────────
    //  Handles the Amazon interstitial "Continue shopping" page
    //  that appears when Amazon detects a CI/bot-like IP.
    //  Waits up to 5 seconds — if not found, continues normally.
    // ──────────────────────────────────────────────────────────────
    private void handleContinueShoppingPage() {
        try {
            d.getWait()
             .withTimeout(Duration.ofSeconds(5))
             .until(ExpectedConditions.elementToBeClickable(CONTINUE_SHOPPING))
             .click();
            log.info("'Continue shopping' interstitial detected and clicked");

            // Wait for the real homepage to load after clicking
            d.getWait().until(ExpectedConditions.visibilityOfElementLocated(SEARCH_BOX));
            log.info("Homepage loaded after interstitial");

        } catch (Exception e) {
            log.info("No interstitial page detected, proceeding normally");
        }
    }
}