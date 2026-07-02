package com.uit.agentcore.tools;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import dev.langchain4j.community.browser.use.BrowserExecutionEngine;

/**
 * An implementation of a {@link BrowserExecutionEngine} that uses
 * <a href="https://playwright.dev/java/">Playwright Java API</a> for performing browser actions.
 */
public class PlaywrightAoyaExecutionEngine implements BrowserExecutionEngine {

    private final BrowserContext browser;
    private final Page page;

    public PlaywrightAoyaExecutionEngine(BrowserContext browser) {
        this.browser = browser;
        // Currently only keep one page
        this.page = this.browser.newPage();
    }

    public Page getPage() {
        return this.page;
    }

    @Override
    public void navigate(String url) {
        Page.NavigateOptions options = new Page.NavigateOptions();
        options.setWaitUntil(WaitUntilState.DOMCONTENTLOADED);
        page.navigate(url, options);
    }

    @Override
    public void click(String element) {
        page.click(element);
    }

    @Override
    public void reload() {
        page.reload();
    }

    @Override
    public void goBack() {
        page.goBack();
    }

    @Override
    public void goForward() {
        page.goForward();
    }

    @Override
    public String getTitle() {
        return page.title();
    }

    @Override
    public String getHtml() {
        return page.content();
    }

    @Override
    public String getText() {
        return page.locator("body").innerText();
    }

    @Override
    public void waitForTimeout(Integer seconds) {
        page.waitForTimeout(seconds * 1000.0);
    }

    @Override
    public void pressEnter() {
        page.keyboard().press("Enter");
    }

    @Override
    public void typeText(String text) {
        page.keyboard().type(text);
    }

    @Override
    public void inputText(String element, String text) {
        page.fill(element, text);
    }

    @Override
    public void dragAndDrop(String source, String target) {
        page.dragAndDrop(source, target);
    }

    public static PlaywrightAoyaExecutionEngineBuilder builder() {
        return new PlaywrightAoyaExecutionEngineBuilder();
    }

    public static class PlaywrightAoyaExecutionEngineBuilder {
        private BrowserContext browser;

        PlaywrightAoyaExecutionEngineBuilder() {}

        public PlaywrightAoyaExecutionEngineBuilder browser(BrowserContext browser) {
            this.browser = browser;
            return this;
        }

        public PlaywrightAoyaExecutionEngine build() {
            return new PlaywrightAoyaExecutionEngine(browser);
        }
    }
}

