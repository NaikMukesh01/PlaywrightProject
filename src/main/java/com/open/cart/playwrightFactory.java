package com.open.cart;

import com.microsoft.playwright.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;


public class playwrightFactory {

    Playwright playwright;
    Browser browser;
    BrowserContext browserContext;
    Page page;
    Properties prop;

    private static ThreadLocal<Browser> tlBrowser = new ThreadLocal<>();
    private static ThreadLocal<Playwright> tlPlaywright = new ThreadLocal<>();
    private static ThreadLocal<BrowserContext> tlBrowserContext = new ThreadLocal<>();
    private static ThreadLocal<Page> tlPage = new ThreadLocal<>();

    public static Browser getBrowser() {
        return tlBrowser.get();
    }
    public static Playwright getPlaywright() {
        return tlPlaywright.get();
    }
    public static BrowserContext getBrowserContext() {
        return tlBrowserContext.get();
    }
    public static Page getPage() {
        return tlPage.get();
    }

    public Page initializeBrowser(Properties prop) {
        String browserName=prop.getProperty("browser").trim();

        System.out.println("Initializing Browser: " + browserName);

       // playwright= Playwright.create();

        tlPlaywright.set(Playwright.create());

        switch (browserName.trim().toLowerCase()) {
            case "chromium":
               // browser= playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
                tlBrowser.set(getPlaywright().chromium().launch(new BrowserType.LaunchOptions().setHeadless(false)));
                break;
            case "firefox":
               // browser= playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
                tlBrowser.set(getPlaywright().firefox().launch(new BrowserType.LaunchOptions().setHeadless(false)));
                break;
            case "safari":
                //browser= playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(false));
                tlBrowser.set(getPlaywright().webkit().launch(new BrowserType.LaunchOptions().setHeadless(false)));
                break;
            case "chrome":
               // browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(false));
                tlBrowser.set(getPlaywright().chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true)));
                break;
            default:
                System.out.println("Invalid Browser Name: " + browserName);
                break;
        }


        tlBrowserContext.set(getBrowser().newContext());
        tlPage.set(getBrowserContext().newPage());
        getPage().navigate(prop.getProperty("url").trim());

        /*browserContext= browser.newContext();
        page= browserContext.newPage();
        page.navigate(prop.getProperty("url").trim());
*/
        return getPage();

    }


    public Properties init_prop() throws FileNotFoundException {
        try {

            FileInputStream fis = new FileInputStream("C:\\Users\\C-Mukesh.Naik1\\IdeaProjects\\PlaywrightProject\\src\\resources\\confg");
            prop = new Properties();
            prop.load(fis);

        }catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop;
    }
}
