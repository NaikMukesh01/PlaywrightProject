package com.opencart.test;

import com.opencart.base.BaseTest;

import org.testng.annotations.Test;
import org.testng.Assert;

public class LoginPageTest extends BaseTest {

    @Test(priority = 1)
    public void loginPageNavigate() {
        loginPage = homePage.navigateToLoginPage();
        String actLoginPageTitle = loginPage.getLoginPagetitle();
        System.out.println("Act Login Page Title: " + actLoginPageTitle);
        Assert.assertEquals(actLoginPageTitle, "Account Login");
    }

    @Test(priority = 2)
    public void isforlinkexist() {
        loginPage = homePage.navigateToLoginPage();   // navigate first
        Assert.assertTrue(loginPage.isForgetPassword());
    }
}