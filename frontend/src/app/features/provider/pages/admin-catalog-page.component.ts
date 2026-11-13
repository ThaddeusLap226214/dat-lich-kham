import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProviderCatalogService } from '../services/provider-catalog.service';
import {
  Doctor,
  DoctorRequest,
  Room,
  RoomRequest,
  Specialty,
  SpecialtyRequest
} from '../models/provider.model';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

type CatalogTab = 'specialties' | 'rooms' | 'doctors';

@Component({
  selector: 'app-admin-catalog-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="ui-page">
      <header class="ui-page-header">
        <div>
          <h1 class="ui-page-title">Quản trị danh mục khám</h1>
          <p class="ui-page-subtitle">Thêm mới và cập nhật chuyên khoa, phòng khám, bác sĩ</p>
        </div>
      </header>

      <div style="display: flex; gap: 8px; margin-bottom: 16px;">
        <button type="button" class="ui-btn" [class.ui-btn-primary]="activeTab === 'specialties'" [class.ui-btn-outline]="activeTab !== 'specialties'" (click)="activeTab = 'specialties'">
          Chuyên khoa ({{ specialties.length }})
        </button>
        <button type="button" class="ui-btn" [class.ui-btn-primary]="activeTab === 'rooms'" [class.ui-btn-outline]="activeTab !== 'rooms'" (click)="activeTab = 'rooms'">
          Phòng khám ({{ rooms.length }})
        </button>
        <button type="button" class="ui-btn" [class.ui-btn-primary]="activeTab === 'doctors'" [class.ui-btn-outline]="activeTab !== 'doctors'" (click)="activeTab = 'doctors'">
          Bác sĩ ({{ doctors.length }})
        </button>
      </div>

      @if (errorMessage) {
        <div class="ui-alert ui-alert-danger" style="margin-bottom: 16px;">
          <span class="material-symbols-outlined">error</span>
          <div class="ui-alert-content">{{ errorMessage }}</div>
        </div>
      }

      @if (activeTab === 'specialties') {
        <div class="ui-card" style="margin-bottom: 16px;">
          <h2 style="margin: 0 0 12px; font-size: 1rem;">
            {{ editingSpecialtyId ? 'Cập nhật chuyên khoa #' + editingSpecialtyId : 'Thêm chuyên khoa mới' }}
          </h2>
          <form (ngSubmit)="saveSpecialty()" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; align-items: end;">
            <div class="ui-field">
              <label class="ui-label">Mã chuyên khoa *</label>
              <input class="ui-input" [(ngModel)]="specialtyForm.code" name="specCode" required maxlength="20" placeholder="VD: TIM_MACH" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Tên chuyên khoa *</label>
              <input class="ui-input" [(ngModel)]="specialtyForm.name" name="specName" required maxlength="100" placeholder="VD: Tim mạch" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Mô tả</label>
              <input class="ui-input" [(ngModel)]="specialtyForm.description" name="specDesc" maxlength="500" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Trạng thái</label>
              <select class="ui-select" [(ngModel)]="specialtyForm.active" name="specActive">
                <option [ngValue]="true">Hoạt động</option>
                <option [ngValue]="false">Ngừng hoạt động</option>
              </select>
            </div>
            <div style="display: flex; gap: 8px;">
              <button type="submit" class="ui-btn ui-btn-primary" [disabled]="saving">
                {{ editingSpecialtyId ? 'Lưu cập nhật' : 'Thêm chuyên khoa' }}
              </button>
              @if (editingSpecialtyId) {
                <button type="button" class="ui-btn ui-btn-outline" (click)="resetSpecialtyForm()">Hủy</button>
              }
            </div>
          </form>
        </div>

        <div class="ui-card" style="padding: 0; overflow-x: auto;">
          <table class="ui-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Mã</th>
                <th>Tên chuyên khoa</th>
                <th>Mô tả</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (s of specialties; track s.id) {
                <tr>
                  <td>{{ s.id }}</td>
                  <td><code>{{ s.code }}</code></td>
                  <td><strong>{{ s.name }}</strong></td>
                  <td>{{ s.description || '—' }}</td>
                  <td>{{ s.active ? 'Hoạt động' : 'Ngừng' }}</td>
                  <td>
                    <button type="button" class="ui-btn ui-btn-outline" (click)="editSpecialty(s)">Sửa</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (activeTab === 'rooms') {
        <div class="ui-card" style="margin-bottom: 16px;">
          <h2 style="margin: 0 0 12px; font-size: 1rem;">
            {{ editingRoomId ? 'Cập nhật phòng #' + editingRoomId : 'Thêm phòng khám mới' }}
          </h2>
          <form (ngSubmit)="saveRoom()" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; align-items: end;">
            <div class="ui-field">
              <label class="ui-label">Mã phòng *</label>
              <input class="ui-input" [(ngModel)]="roomForm.code" name="roomCode" required maxlength="20" placeholder="VD: P205" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Tên phòng *</label>
              <input class="ui-input" [(ngModel)]="roomForm.name" name="roomName" required maxlength="100" placeholder="VD: Phòng 205" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Vị trí</label>
              <input class="ui-input" [(ngModel)]="roomForm.location" name="roomLoc" maxlength="100" placeholder="Tầng 2, khu A" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Trạng thái</label>
              <select class="ui-select" [(ngModel)]="roomForm.active" name="roomActive">
                <option [ngValue]="true">Hoạt động</option>
                <option [ngValue]="false">Ngừng hoạt động</option>
              </select>
            </div>
            <div style="display: flex; gap: 8px;">
              <button type="submit" class="ui-btn ui-btn-primary" [disabled]="saving">
                {{ editingRoomId ? 'Lưu cập nhật' : 'Thêm phòng' }}
              </button>
              @if (editingRoomId) {
                <button type="button" class="ui-btn ui-btn-outline" (click)="resetRoomForm()">Hủy</button>
              }
            </div>
          </form>
        </div>

        <div class="ui-card" style="padding: 0; overflow-x: auto;">
          <table class="ui-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Mã</th>
                <th>Tên phòng</th>
                <th>Vị trí</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (r of rooms; track r.id) {
                <tr>
                  <td>{{ r.id }}</td>
                  <td><code>{{ r.code }}</code></td>
                  <td><strong>{{ r.name }}</strong></td>
                  <td>{{ r.location || '—' }}</td>
                  <td>{{ r.active ? 'Hoạt động' : 'Ngừng' }}</td>
                  <td>
                    <button type="button" class="ui-btn ui-btn-outline" (click)="editRoom(r)">Sửa</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (activeTab === 'doctors') {
        <div class="ui-card" style="margin-bottom: 16px;">
          <h2 style="margin: 0 0 12px; font-size: 1rem;">
            {{ editingDoctorId ? 'Cập nhật bác sĩ #' + editingDoctorId : 'Thêm bác sĩ mới' }}
          </h2>
          <form (ngSubmit)="saveDoctor()" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; align-items: end;">
            <div class="ui-field">
              <label class="ui-label">Họ và tên *</label>
              <input class="ui-input" [(ngModel)]="doctorForm.fullName" name="docName" required maxlength="255" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Chức danh</label>
              <input class="ui-input" [(ngModel)]="doctorForm.title" name="docTitle" maxlength="50" placeholder="BS.CKI / ThS.BS" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Chuyên khoa *</label>
              <select class="ui-select" [(ngModel)]="doctorForm.specialtyId" name="docSpec" required>
                @for (s of specialties; track s.id) {
                  <option [ngValue]="s.id">{{ s.name }}</option>
                }
              </select>
            </div>
            <div class="ui-field">
              <label class="ui-label">Số điện thoại (10 số)</label>
              <input class="ui-input" [(ngModel)]="doctorForm.phone" name="docPhone" placeholder="0901234567" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Email</label>
              <input class="ui-input" type="email" [(ngModel)]="doctorForm.email" name="docEmail" />
            </div>
            <div class="ui-field">
              <label class="ui-label">Trạng thái</label>
              <select class="ui-select" [(ngModel)]="doctorForm.active" name="docActive">
                <option [ngValue]="true">Hoạt động</option>
                <option [ngValue]="false">Ngừng hoạt động</option>
              </select>
            </div>
            <div style="display: flex; gap: 8px;">
              <button type="submit" class="ui-btn ui-btn-primary" [disabled]="saving">
                {{ editingDoctorId ? 'Lưu cập nhật' : 'Thêm bác sĩ' }}
              </button>
              @if (editingDoctorId) {
                <button type="button" class="ui-btn ui-btn-outline" (click)="resetDoctorForm()">Hủy</button>
              }
            </div>
          </form>
        </div>

        <div class="ui-card" style="padding: 0; overflow-x: auto;">
          <table class="ui-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Họ tên</th>
                <th>Chức danh</th>
                <th>Chuyên khoa</th>
                <th>SĐT / Email</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (d of doctors; track d.id) {
                <tr>
                  <td>{{ d.id }}</td>
                  <td><strong>{{ d.fullName }}</strong></td>
                  <td>{{ d.title || '—' }}</td>
                  <td>{{ d.specialtyName }}</td>
                  <td>{{ d.phone || '—' }} / {{ d.email || '—' }}</td>
                  <td>{{ d.active ? 'Hoạt động' : 'Ngừng' }}</td>
                  <td>
                    <button type="button" class="ui-btn ui-btn-outline" (click)="editDoctor(d)">Sửa</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `
})
export class AdminCatalogPageComponent implements OnInit {
  private readonly catalogService = inject(ProviderCatalogService);
  private readonly apiError = inject(ApiErrorService);
  private readonly toast = inject(ToastService);

  activeTab: CatalogTab = 'specialties';
  specialties: Specialty[] = [];
  rooms: Room[] = [];
  doctors: Doctor[] = [];

  saving = false;
  errorMessage = '';

  editingSpecialtyId: number | null = null;
  specialtyForm: SpecialtyRequest = { code: '', name: '', description: '', active: true };

  editingRoomId: number | null = null;
  roomForm: RoomRequest = { code: '', name: '', location: '', active: true };

  editingDoctorId: number | null = null;
  doctorForm: DoctorRequest = { fullName: '', title: '', specialtyId: 0, phone: '', email: '', active: true };

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.catalogService.listSpecialties().subscribe({
      next: (data) => {
        this.specialties = data;
        if (this.doctorForm.specialtyId === 0 && data.length > 0) {
          this.doctorForm.specialtyId = data[0].id;
        }
      },
      error: (err) => (this.errorMessage = this.apiError.show(err).message)
    });
    this.catalogService.listRooms().subscribe({
      next: (data) => (this.rooms = data),
      error: (err) => (this.errorMessage = this.apiError.show(err).message)
    });
    this.catalogService.listDoctors().subscribe({
      next: (data) => (this.doctors = data),
      error: (err) => (this.errorMessage = this.apiError.show(err).message)
    });
  }

  editSpecialty(s: Specialty): void {
    this.editingSpecialtyId = s.id;
    this.specialtyForm = { code: s.code, name: s.name, description: s.description ?? '', active: s.active };
  }

  resetSpecialtyForm(): void {
    this.editingSpecialtyId = null;
    this.specialtyForm = { code: '', name: '', description: '', active: true };
  }

  saveSpecialty(): void {
    this.saving = true;
    this.errorMessage = '';
    const req: SpecialtyRequest = {
      ...this.specialtyForm,
      description: this.specialtyForm.description?.trim() || null
    };
    const obs = this.editingSpecialtyId
      ? this.catalogService.updateSpecialty(this.editingSpecialtyId, req)
      : this.catalogService.createSpecialty(req);
    obs.subscribe({
      next: () => {
        this.toast.success('Thành công', this.editingSpecialtyId ? 'Đã cập nhật chuyên khoa' : 'Đã thêm chuyên khoa mới');
        this.saving = false;
        this.resetSpecialtyForm();
        this.loadAll();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.saving = false;
      }
    });
  }

  editRoom(r: Room): void {
    this.editingRoomId = r.id;
    this.roomForm = { code: r.code, name: r.name, location: r.location ?? '', active: r.active };
  }

  resetRoomForm(): void {
    this.editingRoomId = null;
    this.roomForm = { code: '', name: '', location: '', active: true };
  }

  saveRoom(): void {
    this.saving = true;
    this.errorMessage = '';
    const req: RoomRequest = {
      ...this.roomForm,
      location: this.roomForm.location?.trim() || null
    };
    const obs = this.editingRoomId
      ? this.catalogService.updateRoom(this.editingRoomId, req)
      : this.catalogService.createRoom(req);
    obs.subscribe({
      next: () => {
        this.toast.success('Thành công', this.editingRoomId ? 'Đã cập nhật phòng khám' : 'Đã thêm phòng khám mới');
        this.saving = false;
        this.resetRoomForm();
        this.loadAll();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.saving = false;
      }
    });
  }

  editDoctor(d: Doctor): void {
    this.editingDoctorId = d.id;
    this.doctorForm = {
      fullName: d.fullName,
      title: d.title ?? '',
      specialtyId: d.specialtyId,
      phone: d.phone ?? '',
      email: d.email ?? '',
      active: d.active
    };
  }

  resetDoctorForm(): void {
    this.editingDoctorId = null;
    this.doctorForm = {
      fullName: '',
      title: '',
      specialtyId: this.specialties[0]?.id ?? 0,
      phone: '',
      email: '',
      active: true
    };
  }

  saveDoctor(): void {
    this.saving = true;
    this.errorMessage = '';
    const req: DoctorRequest = {
      ...this.doctorForm,
      title: this.doctorForm.title?.trim() || null,
      phone: this.doctorForm.phone?.trim() || null,
      email: this.doctorForm.email?.trim() || null
    };
    const obs = this.editingDoctorId
      ? this.catalogService.updateDoctor(this.editingDoctorId, req)
      : this.catalogService.createDoctor(req);
    obs.subscribe({
      next: () => {
        this.toast.success('Thành công', this.editingDoctorId ? 'Đã cập nhật bác sĩ' : 'Đã thêm bác sĩ mới');
        this.saving = false;
        this.resetDoctorForm();
        this.loadAll();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.saving = false;
      }
    });
  }
}

