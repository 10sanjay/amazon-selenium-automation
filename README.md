# Amazon Automation Framework

Simple, stable Selenium + TestNG framework for Amazon cart tests.

## Project Structure

```
amazon-automation/
├── pom.xml
├── src/test/
│   ├── java/com/amazon/
│   │   ├── utils/
│   │   │   └── DriverUtils.java      ← Driver setup, safe actions, screenshots, tab handling
│   │   ├── pages/
│   │   │   └── AmazonPage.java       ← All locators + full search→cart flow (keyword as param)
│   │   ├── reports/
│   │   │   └── ReportManager.java    ← Extent Reports singleton
│   │   └── tests/
│   │       ├── BaseTest.java         ← @Before/@After (setup/teardown + screenshot on fail)
│   │       └── AmazonCartTest.java   ← TC1: iPhone | TC2: Galaxy
│   └── resources/
│       ├── testng.xml
│       └── logback-test.xml
├── screenshots/                       ← Auto-created at runtime
├── logs/                              ← automation.log
└── reports/                           ← ExtentReport.html
```

## Prerequisites

- Java 11+
- Maven 3.6+
- Google Chrome (latest)

> WebDriverManager automatically downloads the matching ChromeDriver — no manual setup needed.

## Run Tests

```bash
# Run all tests
mvn clean test

# Run a specific test
mvn clean test -Dtest=AmazonCartTest#testAddIphoneToCart
```

## Reports & Logs

| Output | Location |
|--------|----------|
| HTML Report | `reports/ExtentReport.html` |
| Log file | `logs/automation.log` |
| Screenshots | `screenshots/` |

## Key Design Decisions

| Feature | How it works |
|---------|-------------|
| **Safe Actions** | `safeClick`, `safeType`, `safeGetText` — all retry up to 3× with waits |
| **New Tab Handling** | `switchToNewTab()` waits for handle count > 1, then switches; `closeTabAndSwitch()` closes & returns |
| **Single Page Class** | `AmazonPage` holds all locators and the full flow in one file |
| **Keyword Parameter** | `searchAndAddToCart("iPhone")` — pass any keyword, no hardcoding |
| **Screenshot** | Taken on PASS and FAIL, embedded in Extent Report |
| **Logging** | SLF4J + Logback → console + rolling file |
