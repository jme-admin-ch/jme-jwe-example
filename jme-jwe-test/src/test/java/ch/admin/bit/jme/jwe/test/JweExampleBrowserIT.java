package ch.admin.bit.jme.jwe.test;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser-based end-to-end test driving the real Angular UI in headless Chrome (system Chrome via
 * the Playwright "chrome" channel). Reuses the same harness as {@link JweExampleIT}: Spring Boot's
 * Docker Compose support starts Vault, and the OAuth mock server and the JWE SCS are started as
 * Maven subprocesses. The tests log in through the OAuth mock server's login page (authorization
 * code flow with PKCE, as triggered automatically by the SPA) and then verify on the captured
 * network traffic what this example is all about:
 * <ul>
 *     <li>Encrypted GET: {@code /api/persons} response travels as {@code application/jose}</li>
 *     <li>Encrypted POST: the request body is encrypted in the browser, no plaintext on the wire</li>
 *     <li>Allowlisted request: {@code /api/public/info} stays plain JSON, but authenticated</li>
 *     <li>Decrypt demo view: a captured request JWE is decrypted server-side and displayed,
 *     while the decrypt round trip itself stays encrypted on the wire</li>
 * </ul>
 */
class JweExampleBrowserIT extends BootServiceSpringIntegrationTestBase {

    private static final String AUTH_BASE_URL = "http://localhost:8081/jme-jwe-auth-scs";
    private static final String SCS_BASE_URL = "http://localhost:8080/jme-jwe-scs";
    private static final String APP_URL = SCS_BASE_URL + "/";

    private static final String APPLICATION_JOSE = "application/jose";
    // Compact JWE serialization: five dot-separated base64url segments, starting with the header
    private static final String COMPACT_JWE_PATTERN = "eyJ[A-Za-z0-9_-]*(\\.[A-Za-z0-9_-]*){4}";

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private List<String> browserErrors;

    @BeforeAll
    static void startServicesAndBrowser() throws Exception {
        startService("jme-jwe-auth-scs", AUTH_BASE_URL);
        startService("jme-jwe-scs/jme-jwe-scs-web", SCS_BASE_URL);
        playwright = Playwright.create();
        try {
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome"));
        } catch (PlaywrightException e) {
            throw new IllegalStateException(
                    "Failed to launch Google Chrome. The browser integration test requires a current " +
                            "Chrome installation, see the prerequisites in README.md.", e);
        }
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext();
        page = context.newPage();
        // Allow for the OIDC round trips (authorize -> login -> redirect -> code exchange)
        page.setDefaultTimeout(15_000);
        browserErrors = new CopyOnWriteArrayList<>();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                browserErrors.add(message.text());
            }
        });
        page.onPageError(browserErrors::add);
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void loginViaOAuthMockServerShowsHomeWithLoggedInUser() {
        // The SPA has no login button: an app initializer starts the code flow on load and
        // redirects the browser to the mock server's login page.
        page.navigate(APP_URL);
        page.waitForURL("**/openIdMockServerLogin**");
        assertThat(page.locator("#username")).hasValue("user");

        page.locator("#submit-button").click();

        // Back at the app, the SPA exchanges the authorization code and renders /home
        assertThat(page.locator("p", new Page.LocatorOptions().setHasText("Angemeldet als")))
                .containsText("Henriette Muster");
        assertNoBrowserErrors();
    }

    @Test
    void encryptedGetTransportsPersonsAsJoseOnTheWire() {
        loginAndAwaitHome();

        Response response = page.waitForResponse(
                r -> r.url().endsWith("/api/persons") && "GET".equals(r.request().method()),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Personen laden")).click());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headerValue("content-type")).startsWith(APPLICATION_JOSE);
        assertThat(response.request().headerValue("Accept")).contains(APPLICATION_JOSE);
        assertThat(response.request().headerValue("JWE-Response-Key")).matches(COMPACT_JWE_PATTERN);
        assertThat(response.text()).matches(COMPACT_JWE_PATTERN);

        // ...while the app decrypted the response and renders the seeded person
        assertThat(page.locator("table.ob-table")).containsText("Henriette");
        assertNoBrowserErrors();
    }

    @Test
    void encryptedPostEncryptsRequestBodyInBrowser() {
        loginAndAwaitHome();
        page.locator("input[formcontrolname=firstName]").fill("Paula");
        page.locator("input[formcontrolname=lastName]").fill("Playwright");
        // ahvNumber is pre-filled

        Response response = page.waitForResponse(
                r -> r.url().endsWith("/api/persons") && "POST".equals(r.request().method()),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Person erstellen")).click());

        assertThat(response.status()).isEqualTo(201);
        Request request = response.request();
        assertThat(request.headerValue("Content-Type")).startsWith(APPLICATION_JOSE);
        assertThat(request.postData()).matches(COMPACT_JWE_PATTERN);
        assertThat(request.postData()).doesNotContain("Paula");
        assertThat(response.headerValue("content-type")).startsWith(APPLICATION_JOSE);

        assertThat(page.locator("p.jwe-result")).containsText("Person Paula Playwright mit Id");
        // Creating a person reloads the person list into the table
        assertThat(page.locator("table.ob-table")).containsText("Playwright");
        assertNoBrowserErrors();
    }

    @Test
    void allowlistedPublicInfoStaysPlainJsonButAuthenticated() {
        loginAndAwaitHome();

        Response response = page.waitForResponse(
                r -> r.url().endsWith("/api/public/info"),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Public Info laden")).click());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headerValue("content-type")).startsWith("application/json");
        assertThat(response.request().headerValue("Authorization")).startsWith("Bearer ");
        assertThat(response.text()).contains("\"application\"").contains("jme-jwe-scs");

        assertThat(page.locator("pre.jwe-result")).containsText("jme-jwe-scs");
        assertNoBrowserErrors();
    }

    @Test
    void decryptDemoViewRevealsContentOfACapturedRequestJwe() {
        loginAndAwaitHome();
        // Capture a real encrypted request body, as a user would from the browser's network tab
        page.locator("input[formcontrolname=firstName]").fill("Paula");
        page.locator("input[formcontrolname=lastName]").fill("Playwright");
        Response createResponse = page.waitForResponse(
                r -> r.url().endsWith("/api/persons") && "POST".equals(r.request().method()),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Person erstellen")).click());
        String capturedJwe = createResponse.request().postData();
        assertThat(capturedJwe).matches(COMPACT_JWE_PATTERN);

        // Paste it into the decrypt demo view, which colors the five compact-serialization segments
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(Pattern.compile("JWE entschlüsseln"))).click();
        page.locator("textarea.jwe-input").fill(capturedJwe);
        assertThat(page.locator("pre.jwe-parts span")).hasCount(9); // 5 segments + 4 dots

        Response decryptResponse = page.waitForResponse(
                r -> r.url().endsWith("/api/demo/decrypt"),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Entschlüsseln")).click());

        // The decrypt round trip is itself encrypted: the decrypted person never crosses the wire
        // in plaintext, neither in the request nor in the response
        assertThat(decryptResponse.status()).isEqualTo(200);
        Request decryptRequest = decryptResponse.request();
        assertThat(decryptRequest.headerValue("Content-Type")).startsWith(APPLICATION_JOSE);
        assertThat(decryptRequest.postData()).matches(COMPACT_JWE_PATTERN);
        assertThat(decryptResponse.headerValue("content-type")).startsWith(APPLICATION_JOSE);
        assertThat(decryptResponse.text()).matches(COMPACT_JWE_PATTERN);
        assertThat(decryptResponse.text()).doesNotContain("Paula");

        // ...while the view shows the decrypted protected header and payload
        assertThat(page.locator("pre.jwe-header-json")).containsText("jme-jwe-scs-key");
        assertThat(page.locator("pre.jwe-header-json")).containsText("RSA-OAEP-256");
        assertThat(page.locator("pre.jwe-payload")).containsText("Paula");
        assertThat(page.locator("pre.jwe-payload")).containsText("756.1234.5678.97");
        assertNoBrowserErrors();
    }

    @Test
    void decryptDemoViewDecryptsAResponseJweWithItsResponseKeyEnvelope() {
        loginAndAwaitHome();
        // Capture an encrypted response together with the JWE-Response-Key header of its request —
        // the envelope is what lets the backend recover the response CEK
        Response personsResponse = page.waitForResponse(
                r -> r.url().endsWith("/api/persons") && "GET".equals(r.request().method()),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Personen laden")).click());
        String responseJwe = personsResponse.text();
        String responseKeyEnvelope = personsResponse.request().headerValue("JWE-Response-Key");
        assertThat(responseJwe).matches(COMPACT_JWE_PATTERN);

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(Pattern.compile("JWE entschlüsseln"))).click();

        // Without the envelope the backend cannot decrypt a response JWE and says so
        page.locator("textarea.jwe-input").fill(responseJwe);
        page.waitForResponse(r -> r.url().endsWith("/api/demo/decrypt"),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Entschlüsseln")).click());
        assertThat(page.locator("div.ob-alert-error")).containsText("RESPONSE_KEY_REQUIRED");
        assertThat(page.locator("pre.jwe-header-json")).containsText("dir");

        // With the envelope of the same request, the response is decrypted
        page.locator("textarea.jwe-response-key-input").fill(responseKeyEnvelope);
        Response decryptResponse = page.waitForResponse(
                r -> r.url().endsWith("/api/demo/decrypt"),
                () -> page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Entschlüsseln")).click());

        assertThat(decryptResponse.status()).isEqualTo(200);
        assertThat(page.locator("pre.jwe-payload")).containsText("Henriette");
        assertThat(page.locator("pre.jwe-payload")).containsText("756.1234.5678.97");
        // The deliberate 400 above is reported by Chrome as a console error for the failed resource
        browserErrors.removeIf(error -> error.contains("status of 400"));
        assertNoBrowserErrors();
    }

    // --- Helpers ----------------------------------------------------------------------------------

    /**
     * Opens the app, submits the mock server's login form (the single mock user is pre-selected,
     * the password is a fixed hidden field) and waits until /home is rendered.
     */
    private void loginAndAwaitHome() {
        page.navigate(APP_URL);
        page.waitForURL("**/openIdMockServerLogin**");
        page.locator("#submit-button").click();
        assertThat(page.getByText("Angemeldet als")).isVisible();
    }

    private void assertNoBrowserErrors() {
        assertThat(browserErrors).as("browser console/page errors").isEmpty();
    }
}
