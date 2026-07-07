/**
 * Context path of the backend SCS, derived from the <base href> which the backend rewrites to
 * its servlet context path when serving index.html (see IndexHtmlController in jme-jwe-scs-web).
 * This keeps the packaged frontend reusable for downstream instances of this example that run
 * under a different context path (e.g. jme-nivel-jwe-scs or jme-rhos-jwe-scs).
 *
 * Under `ng serve` the base href is "/": fall back to the context path of the locally running
 * backend SCS, matching the dev proxy configuration (see proxy.conf.js).
 */
const baseHrefPath = new URL(document.baseURI).pathname.replace(/\/$/, '');
export const CONTEXT_PATH = baseHrefPath === '' ? '/jme-jwe-scs' : baseHrefPath;
