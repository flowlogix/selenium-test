package drone;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class BasicIT {
    private static final String testName = "selenium-test";
    @ArquillianResource
    URL baseUrl;
    static WebDriver driver;
    boolean initialized;

    /*
        findBy() are the Graphene parts
     */
    @FindBy(id = "oneButton")
    private WebElement oneButton;
    @FindBy(id = "twoButton")
    private WebElement twoButton;

    @BeforeEach
    void setup() {
        if (!initialized) {
            PageFactory.initElements(driver, this);
            initialized = true;
        }
        driver.get(baseUrl + "");
    }

    @BeforeAll
    static void setupAll() {
        ChromeDriverService chromeDriverService = new ChromeDriverService.Builder()
                .withLogOutput(System.out).build();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        chromeOptions.addArguments("--headless=new");

        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("-headless");

        SafariOptions safariOptions = new SafariOptions();

        EdgeOptions edgeOptions = new EdgeOptions();
        //        chromeOptions.setBrowserVersion("");
        driver = new ChromeDriver(chromeDriverService, chromeOptions);
//        driver = new RemoteWebDriver(safariOptions);
    }

    @AfterAll
    static void teardown() {
        driver.quit();
    }

    @Test
    void contextPath() {
        assertEquals(String.format("/%s/", testName), baseUrl.getPath());
    }

    @Test
    void title() {
        assertEquals("My Index", driver.getTitle());
    }

    @Test
    void droneButtonPress() {
        driver.findElement(By.id("oneButton")).click();
    }

    @Test
    @Tag("graphene")
    void grapheneButtonPress() {
        oneButton.click();
    }

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(MavenImporter.class, testName + ".war")
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
    }
}
