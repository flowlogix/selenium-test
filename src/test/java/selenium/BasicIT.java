package selenium;

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
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

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
        var startTime = Instant.now();
        if (!initialized) {
            PageFactory.initElements(driver, this);
            initialized = true;
        }
        driver.get(baseUrl + "");
        var endTime = Instant.now();
        var duration = Duration.between(startTime, endTime);
        System.out.format("setup() took %02d.%04d seconds \n", duration.toSeconds(), duration.toMillis());
    }

    @BeforeAll
    static void setupAll() {
        var startTime1 = Instant.now();
        ChromeDriverService chromeDriverService = new ChromeDriverService.Builder()
//                .withVerbose(true)
                .withLogOutput(System.out)
                .build();
        ChromeOptions chromeOptions = new ChromeOptions();
//        chromeOptions.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        chromeOptions.addArguments("--headless=new");

        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("-headless");

        SafariOptions safariOptions = new SafariOptions();

        EdgeOptions edgeOptions = new EdgeOptions();
        //        chromeOptions.setBrowserVersion("");
        var endTime1 = Instant.now();
        var duration1 = Duration.between(startTime1, endTime1);
        System.out.format("setupSession() took %02d.%04d seconds \n", duration1.toSeconds(), duration1.toMillis());

        var startTime2 = Instant.now();
//        driver = new FirefoxDriver(firefoxOptions);
        driver = new ChromeDriver(chromeDriverService, chromeOptions);
        driver = new Augmenter().augment(driver);
//        chromeDriverService.close();
//        driver = new RemoteWebDriver(safariOptions);

        var endTime2 = Instant.now();
        var duration2 = Duration.between(startTime2, endTime2);
        System.out.format("createSession() took %02d.%04d seconds \n", duration2.toSeconds(), duration2.toMillis());
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
