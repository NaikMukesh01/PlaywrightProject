package com.open.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class HomePage {

    Page page;
    private String search= "input[name='search']";
    private String searchIcon= "div.search-button button[class='type-text']";
    private String searchPageHeader="div.entry-content h1";
    private String loginLink="li.dropdown-hoverable a.dropdown-item[href*='route=account/login']";
    private String myAccount="li.dropdown-hoverable > a.dropdown-toggle:has-text('My account'):visible";;
    public HomePage(Page page) {
        this.page = page;

    }

    public String getHomePageTitle()
    {
       return page.title();
    }
    public String getHomePageUrl()
    {
        String pageUrl= page.url();
        System.out.println("Page URl"+pageUrl);
        return pageUrl;
    }

    public String Dosearch(String productName)
    {
     page.fill(search, productName);
     page.click(searchIcon);
    return page.textContent(searchPageHeader);
    }

    public LoginPage navigateToLoginPage() {
        page.locator(myAccount).first().hover();          // opens the dropdown
        page.locator(loginLink).first().waitFor(          // wait until Login is visible
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        page.locator(loginLink).first().click();
        return new LoginPage(page);
    }

}
