package ch.admin.bit.jme.jwe.web.ui;

import jakarta.servlet.ServletContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serves the SPA's {@code index.html} with the {@code <base href>} rewritten to the servlet
 * context path. The Angular production build packages a neutral {@code <base href="/">};
 * rewriting it at serve time keeps the packaged frontend reusable for downstream instances of
 * this example that run under a different context path (e.g. jme-nivel-jwe-scs or
 * jme-rhos-jwe-scs). All SPA routes are forwarded here by the
 * {@link ch.admin.bit.jme.jwe.web.filter.SpaWebFilter}.
 */
@RestController
public class IndexHtmlController {

    private static final Pattern BASE_HREF_PATTERN = Pattern.compile("<base href=\"[^\"]*\"");

    private final String indexHtml;

    IndexHtmlController(ServletContext servletContext) throws IOException {
        String packagedIndexHtml = new ClassPathResource("static/index.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String baseHref = "<base href=\"" + servletContext.getContextPath() + "/\"";
        this.indexHtml = BASE_HREF_PATTERN.matcher(packagedIndexHtml)
                .replaceFirst(Matcher.quoteReplacement(baseHref));
    }

    @GetMapping(path = "/index.html", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> getIndexHtml() {
        // index.html must not be cached: it references the fingerprinted bundles of the current build
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(indexHtml);
    }
}
