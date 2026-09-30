import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { pagesFor } from '../../core/app-pages';
import { IdentityService } from '../../core/identity.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  private readonly identity = inject(IdentityService);

  readonly pages = computed(() => pagesFor(this.identity.role()));
}
