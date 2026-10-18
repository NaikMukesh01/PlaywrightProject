package com.opencart.base;

import com.microsoft.playwright.Page;
import com.open.cart.playwrightFactory;
import com.open.pages.HomePage;
import com.open.pages.LoginPage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import java.io.FileNotFoundException;
import java.util.Properties;

public class BaseTest {

    playwrightFactory pf;
    Page page;
    protected Properties prop;
    protected HomePage homePage;
    protected LoginPage loginPage;
    @BeforeMethod
    public void setUp() throws FileNotFoundException {
        pf = new playwrightFactory();
        prop=pf.init_prop();
        page = pf.initializeBrowser(prop);
        homePage = new HomePage(page);
        loginPage = new LoginPage(page);

    }

    @AfterMethod
    public void tearDown() {
        page.context().browser().close();
    }
}
