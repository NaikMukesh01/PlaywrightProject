package com.open.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

public class LoginPage {
    private Page page;

    private String emailId="input#input-email";
    private String paswrd="input#input-password";
    private String click="input[type='submit']";
    private String frgtPass= "form[action*='route=account/login'] a[href*='route=account/forgotten']";

    public LoginPage(Page page) {
        this.page = page;
    }
    public String getLoginPagetitle()
    {
       String lgTitle= page.title();
       return lgTitle;
    }
    public boolean isForgetPassword() {
        try {
            page.locator(frgtPass).first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000));
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }
    public void doLogin(String email,String password)
    {
        System.out.println("Email: "+email+" Password: "+password);
        page.fill(emailId,email);
        page.fill(paswrd,password);
        page.click(click);
    }

}
