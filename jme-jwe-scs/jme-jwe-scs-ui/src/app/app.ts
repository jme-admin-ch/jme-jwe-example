import {ChangeDetectionStrategy, Component, signal} from '@angular/core';

@Component({
	selector: 'app-root',
	templateUrl: './app.html',
	standalone: false,
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './app.scss'
})
export class App {
	readonly year = signal(new Date().getFullYear());
}
