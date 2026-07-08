import {Component, computed, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {FormBuilder, Validators} from '@angular/forms';
import {HttpErrorResponse} from '@angular/common/http';
import {DecryptService} from './decrypt.service';
import {DecryptError, DecryptedJwe} from './decrypt.model';

/**
 * The five dot-separated segments of a compact JWE, in serialization order.
 */
export const JWE_PART_TYPES = ['header', 'encryptedKey', 'iv', 'ciphertext', 'tag'] as const;
export type JwePartType = (typeof JWE_PART_TYPES)[number];

export interface JwePart {
	text: string;
	type: JwePartType;
}

/**
 * A fragment of pretty-printed JSON, categorized for syntax coloring.
 */
export interface JsonToken {
	text: string;
	type: 'key' | 'string' | 'number' | 'literal' | 'plain';
}

/**
 * Demo view: paste a compact JWE captured from the network tab and let the backend decrypt it,
 * displaying the colored JWE segments, the protected header and the recovered plaintext.
 * Strictly for demonstration — the backend endpoint is a deliberate decryption oracle.
 */
@Component({
	selector: 'app-decrypt',
	templateUrl: './decrypt.html',
	styleUrl: './decrypt.scss',
	standalone: false
})
export class Decrypt {
	readonly partTypes = JWE_PART_TYPES;
	readonly result = signal<DecryptedJwe | null>(null);
	readonly error = signal<DecryptError | null>(null);

	readonly jweControl = inject(FormBuilder).nonNullable.control('', Validators.required);

	/** JWE-Response-Key header value — only needed to decrypt a response JWE (alg "dir"). */
	readonly responseKeyControl = inject(FormBuilder).nonNullable.control('');

	/** Live colored breakdown of the pasted JWE into its five compact-serialization segments. */
	readonly jweParts = computed<JwePart[]>(() => splitJwe(this.jweValue()));

	readonly headerTokens = computed<JsonToken[]>(() => tokenizeJsonObject(this.result()?.header));
	readonly payloadTokens = computed<JsonToken[]>(() => {
		const payloadText = this.result()?.payloadText;
		return payloadText ? tokenizeJson(prettyPrint(payloadText)) : [];
	});

	/** Header of a parseable but undecryptable JWE, returned alongside the error. */
	readonly errorHeaderTokens = computed<JsonToken[]>(() => tokenizeJsonObject(this.error()?.header));

	/** A response JWE was pasted without the JWE-Response-Key envelope needed to recover its CEK. */
	readonly needsResponseKey = computed(() => this.error()?.reason === 'RESPONSE_KEY_REQUIRED');

	private readonly jweValue = toSignal(this.jweControl.valueChanges, {initialValue: ''});
	private readonly decryptService = inject(DecryptService);

	decrypt(): void {
		if (this.jweControl.invalid) {
			return;
		}
		this.result.set(null);
		this.error.set(null);
		const responseKeyEnvelope = this.responseKeyControl.value.trim();
		this.decryptService.decrypt(this.jweControl.value.trim(), responseKeyEnvelope || undefined).subscribe({
			next: result => this.result.set(result),
			error: err => this.error.set(toDecryptError(err))
		});
	}
}

function splitJwe(value: string): JwePart[] {
	const trimmed = value.trim();
	if (!trimmed) {
		return [];
	}
	return trimmed.split('.').map((text, index) => ({
		text,
		type: JWE_PART_TYPES[Math.min(index, JWE_PART_TYPES.length - 1)]
	}));
}

function toDecryptError(err: unknown): DecryptError {
	const backendError = parseBackendError(err);
	if (backendError?.reason && backendError.message) {
		return {reason: backendError.reason, message: backendError.message, header: backendError.header ?? null};
	}
	return {
		reason: 'REQUEST_FAILED',
		message: err instanceof HttpErrorResponse || err instanceof Error ? err.message : String(err),
		header: null
	};
}

/**
 * The JWE interceptor requests encrypted endpoints with responseType "text", so Angular leaves
 * error bodies unparsed — the backend's plain JSON error arrives as a string.
 */
function parseBackendError(err: unknown): Partial<DecryptError> | null {
	if (!(err instanceof HttpErrorResponse)) {
		return null;
	}
	if (typeof err.error === 'string') {
		try {
			return JSON.parse(err.error) as Partial<DecryptError>;
		} catch {
			return null;
		}
	}
	return err.error as Partial<DecryptError> | null;
}

function tokenizeJsonObject(object: object | null | undefined): JsonToken[] {
	return object ? tokenizeJson(JSON.stringify(object, null, 2)) : [];
}

function prettyPrint(text: string): string {
	try {
		return JSON.stringify(JSON.parse(text), null, 2);
	} catch {
		return text; // not JSON — show the plaintext as-is
	}
}

function tokenizeJson(json: string): JsonToken[] {
	const pattern = /"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|\btrue\b|\bfalse\b|\bnull\b/g;
	const tokens: JsonToken[] = [];
	let last = 0;
	for (let match = pattern.exec(json); match !== null; match = pattern.exec(json)) {
		if (match.index > last) {
			tokens.push({text: json.slice(last, match.index), type: 'plain'});
		}
		tokens.push({text: match[0], type: tokenType(json, match)});
		last = match.index + match[0].length;
	}
	if (last < json.length) {
		tokens.push({text: json.slice(last), type: 'plain'});
	}
	return tokens;
}

function tokenType(json: string, match: RegExpExecArray): JsonToken['type'] {
	if (!match[0].startsWith('"')) {
		return /^-?\d/.test(match[0]) ? 'number' : 'literal';
	}
	return /^\s*:/.test(json.slice(match.index + match[0].length)) ? 'key' : 'string';
}
