package com.amazon.tests;

import com.amazon.pages.AmazonPage;
import com.amazon.reports.ReportManager;
import com.aventstack.extentreports.Status;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * AmazonCartTest — Two test cases.
 *
 * TC1 → iPhone
 * TC2 → Galaxy
 *
 * Each test:
 *  1. Navigates to Amazon
 *  2. Searches for the keyword
 *  3. Clicks the first matching product  (new window opens)
 *  4. Switches to child window
 *  5. Clicks Add to Cart
 *  6. Fetches and prints price from cart subtotal
 *  7. Closes child window, back to parent
 */
public class AmazonCartTest extends BaseTest {

    // ──────────────────────────────────────────────
    //  Test Case 1 — iPhone
    // ──────────────────────────────────────────────

    @Test(description = "Search iPhone, add to cart, verify price")
    public void testAddIphoneToCart() {
        String keyword = "iPhone";

        ReportManager.getTest().log(Status.INFO, "TC1 → Searching for: " + keyword);

        AmazonPage amazon = new AmazonPage(driverUtils);
        String price = amazon.searchAndAddToCart(keyword);

        ReportManager.getTest().log(Status.INFO, "TC1 → Price fetched: " + price);
        System.out.println("[TC1] iPhone price from cart subtotal: " + price);

        Assert.assertNotNull(price, "Price should not be null");
        Assert.assertFalse(price.isEmpty(), "Price should not be empty");
    }

    // ──────────────────────────────────────────────
    //  Test Case 2 — Galaxy
    // ──────────────────────────────────────────────

    @Test(description = "Search Galaxy, add to cart, verify price")
    public void testAddGalaxyToCart() {
        String keyword = "Galaxy";

        ReportManager.getTest().log(Status.INFO, "TC2 → Searching for: " + keyword);

        AmazonPage amazon = new AmazonPage(driverUtils);
        String price = amazon.searchAndAddToCart(keyword);

        ReportManager.getTest().log(Status.INFO, "TC2 → Price fetched: " + price);
        System.out.println("[TC2] Galaxy price from cart subtotal: " + price);

        Assert.assertNotNull(price, "Price should not be null");
        Assert.assertFalse(price.isEmpty(), "Price should not be empty");
    }
}