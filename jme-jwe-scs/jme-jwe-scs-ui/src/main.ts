import {platformBrowser} from '@angular/platform-browser';
import {AppModule} from './app/app-module';

// Intentionally a promise chain instead of top-level await (typescript:S7785): the webpack-based
// browser builder used by the Oblique toolchain emits a target-environment warning for
// top-level await.
platformBrowser()
	.bootstrapModule(AppModule, {})
	.catch(err => console.error(err));
