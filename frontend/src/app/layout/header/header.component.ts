import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SearchService } from '../../shared/services/search.service';
import { DEMO_IDENTITIES, IdentityService } from '../../core/identity.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  private readonly searchService = inject(SearchService);
  private readonly router = inject(Router);
  protected readonly identity = inject(IdentityService);

  readonly identities = DEMO_IDENTITIES;
  // Ô tìm kiếm phục vụ trang Hồ sơ bệnh nhân (code MPI cũ), chỉ vai trò dùng trang đó mới thấy
  readonly showSearch = computed(() => ['RECEPTIONIST', 'DOCTOR'].includes(this.identity.role()));
  searchQuery = '';

  onSearch(): void {
    this.searchService.setQuery(this.searchQuery);
  }

  onIdentityChange(key: string): void {
    this.identity.select(key);
    this.router.navigateByUrl('/');
  }
}
