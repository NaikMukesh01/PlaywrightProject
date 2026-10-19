package StatusCodecheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.swing.plaf.synth.SynthScrollBarUI;
import java.io.IOException;
import java.util.Map;

public class Apichecker {
    Playwright playwright;
    APIRequest apiRequest;
    APIRequestContext apiRequestContext;

    @BeforeTest
    public void setup() {
        playwright = Playwright.create();
        apiRequest = playwright.request();
        apiRequestContext = apiRequest.newContext(
                new APIRequest.NewContextOptions().setIgnoreHTTPSErrors(true)
        );
    }

    @AfterTest
    public void tearDown() {
       // apiRequestContext.dispose();
        playwright.close();
    }

         @Test
         public void getSpecificDetails(){
             APIResponse responseApi=  apiRequestContext.get("https://gorest.co.in/public/v2/users", RequestOptions.create()

                         .setQueryParam("id",8671697)
                         .setQueryParam("name","Prema Shukla")



                 );

             int statusCode=responseApi.status();
             System.out.println(statusCode);
             Assert.assertEquals(statusCode,200);

             System.out.println("=====Status Text=====");
             String statusText=responseApi.statusText();
             System.out.println(statusText);

             System.out.println("===== Text =====");
             System.out.println(responseApi.text());
    }

    @Test
    public void Apichecker() throws IOException {
      APIResponse responseApi= apiRequestContext.get("https://gorest.co.in/public/v2/users");
      int statusCode=responseApi.status();
      System.out.println(statusCode);
      Assert.assertEquals(statusCode,200);
      Assert.assertEquals(responseApi.ok(),true);

      System.out.println("===== Body Reponse=====");
        ObjectMapper objectMapper = new ObjectMapper();
       JsonNode reponseBody= objectMapper.readTree(responseApi.body());
       String actualResponseBody=reponseBody.toPrettyString();
       System.out.println(actualResponseBody);

        System.out.println("===== Header Reponse=====");

       Map<String,String> headerMap= responseApi.headers();
       System.out.println(headerMap);

       System.out.println("===== Header Values Reponse=====");
      //  Assert.assertEquals(headerMap.get("access-control-expose-headers"),"Content-Length,X-Requested-With");





    }
}
