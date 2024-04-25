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
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.grid.Main;
import org.openqa.selenium.manager.SeleniumManager;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
        if (System.getProperty("selenium.manager") != null) {
            downloadManager();
        }
        if (System.getProperty("selenium.grid") != null) {
            startGrid();
        }
        var startTime1 = Instant.now();
        ChromeDriverService chromeDriverService =
                switch (System.getProperty("selenium.browser", "chrome").toLowerCase()) {
                    case "chrome" -> new ChromeDriverService.Builder()
//                .withVerbose(true)
                            .withLogOutput(System.out)
                            .build();
                    default -> null;
                };

        ChromeOptions chromeOptions = new ChromeOptions();
//        chromeOptions.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        chromeOptions.addArguments("--headless=new");

        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("--headless");

        SafariOptions safariOptions = new SafariOptions();

        EdgeOptions edgeOptions = new EdgeOptions().addArguments("headless");
        var endTime1 = Instant.now();
        var duration1 = Duration.between(startTime1, endTime1);
        System.out.format("setupSession() took %02d.%04d seconds \n", duration1.toSeconds(), duration1.toMillis());

        var startTime2 = Instant.now();

        record Driver(Supplier<WebDriver> driver, Capabilities options) {
            Driver(Supplier<WebDriver> driver, Capabilities options) {
                this.driver = driver;
                this.options = options;
            }
        }

        Driver initDriver = switch (System.getProperty("selenium.browser", "chrome")) {
            case "chrome" -> new Driver(() -> new ChromeDriver(chromeDriverService, chromeOptions), chromeOptions);
            case "firefox" -> new Driver(() -> new FirefoxDriver(firefoxOptions), firefoxOptions);
            case "edge" -> new Driver(() -> new EdgeDriver(edgeOptions), edgeOptions);
            case "safari" -> new Driver(() -> new SafariDriver(safariOptions), safariOptions);
            default -> throw new IllegalStateException();
        };

        String browserVersion = System.getProperty("selenium.version");
        if (browserVersion != null) {
            var caps = (AbstractDriverOptions<?>) initDriver.options();
            caps.setBrowserVersion(browserVersion);
        }

        if (System.getProperty("selenium.grid") != null) {
            driver = new RemoteWebDriver(initDriver.options());
        } else {
            driver = initDriver.driver.get();
        }
        driver = new Augmenter().augment(driver);

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

    static Path downloadManager() {
        Path managerPath = getManagerPath();
        managerPath.getParent().toFile().mkdirs();
        var paths = SeleniumManager.getInstance().getBinaryPaths(new ArrayList<>(List.of("--browser", "chrome")));
        System.out.printf("ChromeDriver path: %s, Chrome Path: %s, Selenium Manager Path: %s\n",
                paths.getDriverPath(), paths.getBrowserPath(), managerPath);
        return managerPath;
    }

    private static Path getManagerPath() {
        String cachePath = "~/.cache/selenium".replace("~", System.getProperty("user.home"));
        String cachePathEnv = System.getenv("SE_CACHE_PATH");
        if (cachePathEnv != null) {
            cachePath = cachePathEnv;
        }
        Path cacheParent = Paths.get(cachePath);
        String binaryName = "selenium-manager" + (Platform.getCurrent().is(Platform.WINDOWS) ? ".exe" : "");
        var releaseLabel = new BuildInfo().getReleaseLabel();
        int lastDot = releaseLabel.lastIndexOf(".");
        String minorVersion = releaseLabel.substring(0, lastDot);
        String seleniumManagerVersion = "0." + minorVersion;
        return Paths.get(cacheParent.toString(), String.format("/manager/%s/%s", seleniumManagerVersion, binaryName));
    }

    static void startGrid() {
        Main.main(new String[]{
                "standalone",
                "--selenium-manager", "true",
                "--enable-managed-downloads", "true"});
    }
}
