package com.amazon.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * DriverUtils — thread-safe via ThreadLocal.
 * Each parallel test thread gets its own WebDriver + WebDriverWait +
 * parentWindow.
 */
public class DriverUtils {

    private static final Logger log = LoggerFactory.getLogger(DriverUtils.class);
    private static final int TIMEOUT = 20;
    private static final int RETRIES = 3;
    private static final String SS_DIR = "screenshots/";

    // ── ThreadLocal storage — one instance per thread ──────────────
    private static final ThreadLocal<WebDriver> tlDriver = new ThreadLocal<>();
    private static final ThreadLocal<WebDriverWait> tlWait = new ThreadLocal<>();
    private static final ThreadLocal<String> tlParentWindow = new ThreadLocal<>();

    // ── Convenience getters ─────────────────────────────────────────
    public WebDriver getDriver() {
        return tlDriver.get();
    }

    public WebDriverWait getWait() {
        return tlWait.get();
    }

    // ──────────────────────────────────────────────
    // Setup / Teardown
    // ──────────────────────────────────────────────

    public void initDriver() {
        log.info("[Thread-{}] Initialising ChromeDriver...", Thread.currentThread().getId());
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // ── Headless + server flags ──────────────────────────────────
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--remote-debugging-port=0");

        // ── Don't wait for full page load, fire as soon as DOM is ready ──
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        WebDriver driver = new ChromeDriver(options);

        // ── Give amazon.in enough time to load on CI ─────────────────
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(90));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        tlDriver.set(driver);
        tlWait.set(wait);

        try {
            Files.createDirectories(Paths.get(SS_DIR));
        } catch (IOException e) {
            log.warn("Could not create screenshots dir: {}", e.getMessage());
        }

        log.info("[Thread-{}] ChromeDriver ready.", Thread.currentThread().getId());
    }

    public void quitDriver() {
        if (getDriver() != null) {
            log.info("[Thread-{}] Quitting browser.", Thread.currentThread().getId());
            getDriver().quit();
            tlDriver.remove();
            tlWait.remove();
            tlParentWindow.remove();
        }
    }

    // ──────────────────────────────────────────────
    // Safe Click (retry-aware)
    // ──────────────────────────────────────────────

    public void safeClick(By locator) {
        for (int attempt = 1; attempt <= RETRIES; attempt++) {
            try {
                log.info("[Thread-{}][Click] attempt {} → {}", Thread.currentThread().getId(), attempt, locator);
                WebElement el = getWait().until(ExpectedConditions.elementToBeClickable(locator));
                el.click();
                return;
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                log.warn("[Thread-{}][Click] attempt {} failed: {}", Thread.currentThread().getId(), attempt,
                        e.getMessage());
                sleep(500);
            }
        }
        throw new RuntimeException("safeClick failed after " + RETRIES + " attempts for: " + locator);
    }

    // ──────────────────────────────────────────────
    // Safe Type (retry-aware)
    // ──────────────────────────────────────────────

    public void safeType(By locator, String text) {
        for (int attempt = 1; attempt <= RETRIES; attempt++) {
            try {
                log.info("[Thread-{}][Type] attempt {} → '{}'", Thread.currentThread().getId(), attempt, text);
                WebElement el = getWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
                el.clear();
                el.sendKeys(text);
                return;
            } catch (StaleElementReferenceException e) {
                log.warn("[Thread-{}][Type] attempt {} failed: {}", Thread.currentThread().getId(), attempt,
                        e.getMessage());
                sleep(500);
            }
        }
        throw new RuntimeException("safeType failed after " + RETRIES + " attempts for: " + locator);
    }

    // ──────────────────────────────────────────────
    // Safe GetText (retry-aware)
    // ──────────────────────────────────────────────

    public String safeGetText(By locator) {
        for (int attempt = 1; attempt <= RETRIES; attempt++) {
            try {
                log.info("[Thread-{}][GetText] attempt {} → {}", Thread.currentThread().getId(), attempt, locator);
                WebElement el = getWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
                return el.getText().trim();
            } catch (StaleElementReferenceException e) {
                log.warn("[Thread-{}][GetText] attempt {} failed: {}", Thread.currentThread().getId(), attempt,
                        e.getMessage());
                sleep(500);
            }
        }
        throw new RuntimeException("safeGetText failed after " + RETRIES + " attempts for: " + locator);
    }

    // ──────────────────────────────────────────────
    // Window Handling
    // ──────────────────────────────────────────────

    /**
     * Saves the current (parent) window handle, waits for a new window,
     * then switches focus to it. Each thread stores its own parent handle.
     */
    public void switchToChildWindow() {
        tlParentWindow.set(getDriver().getWindowHandle());
        log.info("[Thread-{}][Window] Parent saved: {}", Thread.currentThread().getId(), tlParentWindow.get());

        getWait().until(d -> d.getWindowHandles().size() > 1);

        ArrayList<String> allHandles = new ArrayList<>(getDriver().getWindowHandles());
        for (String handle : allHandles) {
            if (!handle.equals(tlParentWindow.get())) {
                getDriver().switchTo().window(handle);
                log.info("[Thread-{}][Window] Switched to child: {}", Thread.currentThread().getId(), handle);
                break;
            }
        }
    }

    /**
     * Closes the child window and switches back to this thread's parent window.
     */
    public void closeChildAndSwitchToParent() {
        String child = getDriver().getWindowHandle();
        getDriver().close();
        log.info("[Thread-{}][Window] Closed child: {}", Thread.currentThread().getId(), child);
        getDriver().switchTo().window(tlParentWindow.get());
        log.info("[Thread-{}][Window] Back to parent: {}", Thread.currentThread().getId(), tlParentWindow.get());
    }

    // ──────────────────────────────────────────────
    // Screenshot
    // ──────────────────────────────────────────────

    public String takeScreenshot(String testName) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String path = SS_DIR + testName + "_T" + Thread.currentThread().getId() + "_" + ts + ".png";
        try {
            File src = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), Paths.get(path));
            log.info("[Thread-{}][Screenshot] Saved: {}", Thread.currentThread().getId(), path);
        } catch (IOException e) {
            log.error("[Thread-{}][Screenshot] Failed: {}", Thread.currentThread().getId(), e.getMessage());
        }
        return path;
    }

    // ──────────────────────────────────────────────
    // Navigate
    // ──────────────────────────────────────────────

    public void navigateTo(String url) {
        log.info("[Thread-{}][Navigate] → {}", Thread.currentThread().getId(), url);
        getDriver().get(url);
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}