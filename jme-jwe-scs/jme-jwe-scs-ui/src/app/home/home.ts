import {Component, inject, signal} from '@angular/core';
import {FormBuilder, Validators} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';
import {OAuthService} from 'angular-oauth2-oidc';
import {PersonService} from '../person/person.service';
import {Person, PublicInfo} from '../person/person.model';

@Component({
	selector: 'app-home',
	templateUrl: './home.html',
	styleUrl: './home.scss',
	standalone: false
})
export class Home {
	readonly persons = signal<Person[] | null>(null);
	readonly createdPerson = signal<Person | null>(null);
	readonly publicInfo = signal<PublicInfo | null>(null);
	readonly error = signal<string | null>(null);

	readonly personForm = inject(FormBuilder).nonNullable.group({
		firstName: ['', Validators.required],
		lastName: ['', Validators.required],
		ahvNumber: ['756.1234.5678.97', Validators.required]
	});

	private readonly personService = inject(PersonService);
	private readonly oauthService = inject(OAuthService);
	private readonly translateService = inject(TranslateService);

	get username(): string {
		const claims = this.oauthService.getIdentityClaims() as {given_name?: string; family_name?: string} | null;
		return claims ? `${claims.given_name ?? ''} ${claims.family_name ?? ''}`.trim() : '';
	}

	loadPersons(): void {
		this.error.set(null);
		this.personService.getPersons().subscribe({
			next: persons => this.persons.set(persons),
			error: err => this.error.set(this.translateService.instant('jwe.home.error.loadPersons', {message: err.message ?? err}))
		});
	}

	createPerson(): void {
		if (this.personForm.invalid) {
			return;
		}
		this.error.set(null);
		this.personService.createPerson(this.personForm.getRawValue()).subscribe({
			next: person => {
				this.createdPerson.set(person);
				this.loadPersons();
			},
			error: err => this.error.set(this.translateService.instant('jwe.home.error.createPerson', {message: err.message ?? err}))
		});
	}

	loadPublicInfo(): void {
		this.error.set(null);
		this.personService.getPublicInfo().subscribe({
			next: info => this.publicInfo.set(info),
			error: err => this.error.set(this.translateService.instant('jwe.home.error.loadPublicInfo', {message: err.message ?? err}))
		});
	}
}
