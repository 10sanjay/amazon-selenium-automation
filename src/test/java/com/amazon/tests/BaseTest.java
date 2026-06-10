package com.amazon.tests;

import com.amazon.reports.ReportManager;
import com.amazon.utils.DriverUtils;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * BaseTest — screenshot is captured ONLY on test failure.
 */
public class BaseTest {

    protected DriverUtils driverUtils;

    @BeforeMethod
    public void setUp(Method method) {
        String testName = method.getName();
        ReportManager.createTest(testName);
        ReportManager.getTest().log(Status.INFO, "Test started: " + testName);

        driverUtils = new DriverUtils();
        driverUtils.initDriver();
    }

    @AfterMethod
    public void tearDown(ITestResult result) {

        if (result.getStatus() == ITestResult.FAILURE) {
            // ── Failure — take screenshot and attach to report ──
            String ssPath = driverUtils.takeScreenshot(result.getName());
            try {
                ReportManager.getTest().fail(
                    "Test FAILED: " + result.getThrowable().getMessage(),
                    MediaEntityBuilder.createScreenCaptureFromPath("../" + ssPath).build()
                );
            } catch (Exception e) {
                ReportManager.getTest().fail("Test FAILED. Screenshot error: " + e.getMessage());
            }

        } else if (result.getStatus() == ITestResult.SUCCESS) {
            // ── Pass — log message only, NO screenshot ──
            ReportManager.getTest().log(Status.PASS, "Test PASSED");

        } else {
            // ── Skipped — log only ──
            ReportManager.getTest().log(Status.SKIP, "Test SKIPPED");
        }

        driverUtils.quitDriver();
        ReportManager.flush();
    }
}