import {TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {TranslateModule} from '@ngx-translate/core';
import {OAuthService} from 'angular-oauth2-oidc';
import {of} from 'rxjs';
import {Home} from './home';
import {PersonService} from '../person/person.service';
import {Person} from '../person/person.model';

describe('Home', () => {
	const persons: Person[] = [{id: '1', firstName: 'Henriette', lastName: 'Muster', ahvNumber: '756.1234.5678.97'}];
	let personService: {getPersons: jest.Mock; createPerson: jest.Mock; getPublicInfo: jest.Mock};

	beforeEach(async () => {
		personService = {
			getPersons: jest.fn(() => of(persons)),
			createPerson: jest.fn((person: Person) => of({...person, id: '42'})),
			getPublicInfo: jest.fn(() => of({application: 'jme-jwe-scs', serverTime: 'now', transport: 'plain'}))
		};
		await TestBed.configureTestingModule({
			declarations: [Home],
			imports: [ReactiveFormsModule, MatCardModule, MatButtonModule, MatFormFieldModule, MatInputModule, TranslateModule.forRoot()],
			providers: [
				{provide: PersonService, useValue: personService},
				{provide: OAuthService, useValue: {getIdentityClaims: () => ({given_name: 'Henriette', family_name: 'Muster'})}}
			]
		}).compileComponents();
	});

	it('should create and show the logged in user', () => {
		const fixture = TestBed.createComponent(Home);
		expect(fixture.componentInstance.username).toBe('Henriette Muster');
	});

	it('should load persons via the encrypted GET endpoint', () => {
		const component = TestBed.createComponent(Home).componentInstance;
		component.loadPersons();
		expect(personService.getPersons).toHaveBeenCalled();
		expect(component.persons()).toEqual(persons);
	});

	it('should create a person via the encrypted POST endpoint and reload the list', () => {
		const component = TestBed.createComponent(Home).componentInstance;
		component.personForm.setValue({firstName: 'Max', lastName: 'Test', ahvNumber: '756.0000.0000.00'});
		component.createPerson();
		expect(personService.createPerson).toHaveBeenCalledWith({firstName: 'Max', lastName: 'Test', ahvNumber: '756.0000.0000.00'});
		expect(component.createdPerson()?.id).toBe('42');
		expect(personService.getPersons).toHaveBeenCalled();
	});

	it('should load the public info via the allowlisted unencrypted endpoint', () => {
		const component = TestBed.createComponent(Home).componentInstance;
		component.loadPublicInfo();
		expect(personService.getPublicInfo).toHaveBeenCalled();
		expect(component.publicInfo()?.application).toBe('jme-jwe-scs');
	});
});
