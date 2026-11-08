import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SearchService } from '../../shared/services/search.service';
import { IdentityService } from '../../core/identity.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  private readonly searchService = inject(SearchService);
  protected readonly identity = inject(IdentityService);

  readonly roleLabels: Record<string, string> = {
    PATIENT: 'Bệnh nhân', RECEPTIONIST: 'Lễ tân', DOCTOR: 'Bác sĩ', ADMIN: 'Quản trị viên', GUEST: 'Chưa đăng nhập'
  };
  // Ô tìm kiếm phục vụ trang Hồ sơ bệnh nhân (code MPI cũ), chỉ vai trò dùng trang đó mới thấy
  readonly showSearch = computed(() => ['RECEPTIONIST', 'DOCTOR'].includes(this.identity.role()));
  searchQuery = '';

  onSearch(): void {
    this.searchService.setQuery(this.searchQuery);
  }

  login(): void {
    this.identity.login();
  }

  logout(): void {
    this.identity.logout();
  }
}
