/**
 * The parsed JWE protected header: the well-known members plus whatever else it declares.
 */
export interface JweHeader {
	alg?: string;
	enc?: string;
	kid?: string;
	cty?: string;
	[parameter: string]: unknown;
}

/**
 * Result of the demo decrypt endpoint: the parsed JWE protected header and the recovered
 * plaintext — as text when it is valid UTF-8 (the usual JSON case), as base64 otherwise
 * (e.g. the raw AES key inside a JWE-Response-Key envelope).
 */
export interface DecryptedJwe {
	header: JweHeader;
	payloadText: string | null;
	payloadBase64: string | null;
}

/**
 * Error body of the demo decrypt endpoint. The header is present when the JWE could at least be
 * parsed, e.g. when a response JWE (alg "dir") is pasted, which the backend cannot decrypt.
 */
export interface DecryptError {
	reason: string;
	message: string;
	header: JweHeader | null;
}
