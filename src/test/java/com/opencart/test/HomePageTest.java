package com.opencart.test;

import com.microsoft.playwright.Page;
import com.open.cart.playwrightFactory;
import com.open.pages.HomePage;
import com.opencart.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.*;

public class HomePageTest extends BaseTest {


    @Test
    public void HomePageTitle() {
        String titleName = homePage.getHomePageTitle();
        Assert.assertEquals(titleName, "Your Store");
    }

    @DataProvider(name = "getDataProduct")
    public Object[][] getDataProduct() {
        return new Object[][]{
                {"Macbook"},
                {"Macbook Pro"},
                {"Samsung"}
        };
    }

    @Test(dataProvider = "getDataProduct")
    public void HomePageHeader(String productName) {
        String headerName = homePage.Dosearch(productName);
        Assert.assertEquals(headerName, "Search - " + productName.trim());
    }

    @Test
    public void HomePageURl() {
        String pageUrl = homePage.getHomePageUrl();
        Assert.assertEquals(pageUrl, "https://ecommerce-playground.lambdatest.io/");
    }


}