import {TestBed} from '@angular/core/testing';
import {RouterModule} from '@angular/router';
import {TranslatePipe, provideTranslateService} from '@ngx-translate/core';
import {App} from './app';

describe('App', () => {
	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [RouterModule.forRoot([]), TranslatePipe],
			declarations: [App],
			providers: [provideTranslateService()]
		}).compileComponents();
	});

	it('should create the app', () => {
		const fixture = TestBed.createComponent(App);
		const app = fixture.componentInstance;
		expect(app).toBeTruthy();
	});
});
