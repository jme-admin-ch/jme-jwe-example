// @ts-check
import eslint from "@eslint/js";
import angular from "angular-eslint";
import prettierRecommended from "eslint-plugin-prettier/recommended";
import tseslint from "typescript-eslint";

export default tseslint.config(
	{
		files: ["**/*.ts"],

		extends: [
			eslint.configs.recommended,
			...tseslint.configs.recommended,
			...angular.configs.tsRecommended,
			prettierRecommended,
		],

		processor: angular.processInlineTemplates,

		languageOptions: {
			parserOptions: {
				project: ["tsconfig.lint.json"],
				tsconfigRootDir: import.meta.dirname,
			},
		},

		rules: {
			"@typescript-eslint/member-ordering": [
				"error",
				{
					default: [
						"public-static-field",
						"public-instance-field",
						"protected-static-field",
						"protected-instance-field",
						"private-static-field",
						"private-instance-field",
						"public-constructor",
						"protected-constructor",
						"private-constructor",
						"public-static-method",
						"public-instance-method",
						"protected-static-method",
						"protected-instance-method",
						"private-static-method",
						"private-instance-method",
					],
				},
			],
			"@angular-eslint/component-class-suffix": "off", // not compatible with new Angular naming scheme
			"@angular-eslint/directive-class-suffix": "off", // not compatible with new Angular naming scheme
			"@angular-eslint/no-pipe-impure": "error",
			"@angular-eslint/prefer-output-readonly": "error",
			"@angular-eslint/prefer-signals": "off",
			"@angular-eslint/prefer-standalone": "off",
			"@angular-eslint/relative-url-prefix": "error",
			"@angular-eslint/use-component-selector": "error",
			"@typescript-eslint/array-type": "error",
			"@typescript-eslint/ban-tslint-comment": "error",
			"@typescript-eslint/consistent-type-assertions": "error",
			"@typescript-eslint/default-param-last": "error",
			"@typescript-eslint/method-signature-style": "error",
			"@typescript-eslint/no-floating-promises": "error",
			"@typescript-eslint/no-unnecessary-boolean-literal-compare": "error",
			"@typescript-eslint/no-unnecessary-type-assertion": "error",
			"@typescript-eslint/prefer-includes": "error",
			"@typescript-eslint/prefer-optional-chain": "error",
			"@typescript-eslint/prefer-readonly": "error",
			"@typescript-eslint/prefer-regexp-exec": "error",
			"@typescript-eslint/prefer-string-starts-ends-with": "error",
			"@typescript-eslint/unified-signatures": "error",
			"dot-notation": "error",
			eqeqeq: ["error", "smart"],
			"guard-for-in": "error",

			"max-lines": [
				"error",
				{
					max: 300,
				},
			],

			"max-lines-per-function": [
				"error",
				{
					max: 35,
				},
			],

			"no-bitwise": "error",
			"no-caller": "error",

			"no-console": [
				"error",
				{
					allow: ["info", "warn", "error"],
				},
			],

			"no-else-return": "error",
			"no-eval": "error",
			"no-new-wrappers": "error",
			"no-sequences": "error",
			"no-throw-literal": "error",
			"no-undef-init": "error",
			"no-unused-expressions": "error",
			"no-useless-concat": "error",
			"no-useless-escape": "error",
			"object-shorthand": "error",
			"prefer-exponentiation-operator": "error",
			"prefer-object-spread": "error",
			"prefer-regex-literals": "error",
			"prefer-template": "error",
			radix: "error",

			"sort-imports": [
				"error",
				{
					ignoreDeclarationSort: true,
				},
			],

			"spaced-comment": "error",
		},
	},
	{
		files: ["**/*.spec.ts"],

		rules: {
			"@typescript-eslint/ban-ts-comment": "off",
			"@typescript-eslint/no-floating-promises": "off",
			"@angular-eslint/use-component-selector": "off",
			"max-lines-per-function": "off",
			"max-lines": "off",
		},
	},
	{
		files: ["**/*.decorator.ts"],

		rules: {
			"max-lines-per-function": "off",
		},
	},
	{
		files: ["**/*.html"],

		extends: [...angular.configs.templateRecommended],
	},
	{
		files: ["**/*.html"],
		ignores: ["**/*inline-template-*.component.html"],

		extends: [prettierRecommended],

		rules: {
			"prettier/prettier": [
				"error",
				{
					parser: "angular",
				},
			],
		},
	},
);
