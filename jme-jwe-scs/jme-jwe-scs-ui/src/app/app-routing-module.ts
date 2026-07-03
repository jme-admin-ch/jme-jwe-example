import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {ObUnknownRouteModule} from '@oblique/oblique';
import {Home} from './home/home';

const routes: Routes = [
	{path: '', redirectTo: 'home', pathMatch: 'full'},
	{path: 'home', component: Home},
	{path: '**', redirectTo: 'unknown-route'}
];

@NgModule({
	imports: [RouterModule.forRoot(routes, {anchorScrolling: 'enabled'}), ObUnknownRouteModule],
	exports: [RouterModule]
})
export class AppRoutingModule {}
