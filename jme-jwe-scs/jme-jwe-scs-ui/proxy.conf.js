/**
 * Proxy configuration for `ng serve`: forwards all backend calls (API, UI configuration and the
 * JWE discovery endpoints) to the locally running SCS so the app can use relative URLs in
 * development just like when it is served from the backend jar.
 */
module.exports = {
	'/jme-jwe-scs': {
		target: 'http://localhost:8080',
		changeOrigin: true,
		secure: false,
		logLevel: 'debug'
	}
};
