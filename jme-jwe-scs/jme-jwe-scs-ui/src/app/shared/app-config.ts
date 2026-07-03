/**
 * Context path of the backend SCS. All API calls are made with relative URLs so that the same
 * build works both when the frontend is served from the backend jar (same origin) and during
 * development with `ng serve` and the proxy configuration (see proxy.conf.js).
 */
export const CONTEXT_PATH = '/jme-jwe-scs';
