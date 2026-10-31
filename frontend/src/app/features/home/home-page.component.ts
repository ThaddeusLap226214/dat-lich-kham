import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiCall, PARTS, Part, pagesFor } from '../../core/app-pages';
import { ApiProbeService, ProbeResult } from '../../core/api-probe.service';
import { IdentityService } from '../../core/identity.service';

interface ServiceCheck {
  part: Part | 'GW';
  name: string;
  call: ApiCall;
}

// Mỗi thành phần một API GET không cần header danh tính, để biết thành phần đó có đang chạy và nhận request qua Gateway không
const CHECKS: ServiceCheck[] = [
  { part: 'P4', name: 'Auth Service', call: { method: 'GET', path: '/accounts' } },
  { part: 'P3', name: 'Provider Service', call: { method: 'GET', path: '/specialties' } },
  { part: 'P1', name: 'Patient Service', call: { method: 'GET', path: '/patients?size=1' } },
  { part: 'P2', name: 'Scheduling Service', call: { method: 'GET', path: '/slots?doctorId=1&date={today}' } },
  { part: 'P5', name: 'Reminder Service', call: { method: 'GET', path: '/reminders/logs' } },
  { part: 'GW', name: 'API Gateway (API Composition)', call: { method: 'GET', path: '/composite/doctor-agenda?doctorId=1&date={today}' } }
];

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="home">
      <h1>Nền tảng đặt lịch khám đa khoa</h1>
      <p class="lead">
        Bản khung của hệ thống microservices: Gateway, 5 service, database, RabbitMQ và web đã chạy và nối với nhau.
        Mỗi trang và mỗi API đã có chỗ; phần xử lý bên trong do nhóm viết tiếp theo <code>docs/04-viec-cua-tung-nguoi.md</code>.
      </p>

      <div class="section-head">
        <h2>Tình trạng các thành phần</h2>
        <button class="btn" (click)="check()" [disabled]="loading()">
          <span class="material-symbols-outlined">refresh</span> Kiểm tra lại
        </button>
      </div>
      <table>
        <thead><tr><th>Phần</th><th>Thành phần</th><th>API thử</th><th>Kết quả</th></tr></thead>
        <tbody>
          @for (row of rows(); track row.check.name) {
            <tr>
              <td><span class="badge">{{ row.check.part }}</span></td>
              <td>{{ row.check.name }}</td>
              <td><code>{{ row.check.call.path }}</code></td>
              <td [class]="tone(row.result)">{{ label(row.result) }}</td>
            </tr>
          }
        </tbody>
      </table>
      <p class="legend">
        <span class="ok">200</span> đã làm xong ·
        <span class="todo">501</span> service chạy, đường đi đúng, phần xử lý chưa viết ·
        <span class="down">503 / lỗi</span> service chưa bật hoặc đang khởi động
      </p>

      <h2>Trang dành cho vai trò đang chọn: {{ identity.identity().label }}</h2>
      <p class="hint">Đổi vai trò ở ô "Chế độ thử" trên thanh đầu trang.</p>
      <div class="cards">
        @for (p of pages(); track p.path) {
          <a class="card" [routerLink]="'/' + p.path">
            <span class="material-symbols-outlined">{{ p.icon }}</span>
            <div>
              <div class="card-title">{{ p.title }}</div>
              <div class="card-part">{{ p.part }} · {{ partName(p.part) }}</div>
            </div>
          </a>
        }
      </div>
    </section>
  `,
  styles: [`
    .home { max-width: 64rem; display: flex; flex-direction: column; gap: 1rem; }
    h1 { font: var(--font-headline-lg); }
    h2 { font: var(--font-title-lg); }
    .lead, .hint, .legend { color: var(--on-surface-variant); }
    .section-head { display: flex; justify-content: space-between; align-items: center; margin-top: 1rem; }
    table { border-collapse: collapse; background: var(--surface-container-lowest); border-radius: .75rem; overflow: hidden; }
    th, td { text-align: left; padding: .6rem .9rem; border-bottom: 1px solid var(--outline-variant); font-size: .875rem; }
    th { background: var(--surface-container-low); }
    code { font-size: .8rem; }
    .badge { background: var(--primary-fixed); color: var(--on-primary-fixed-variant); padding: .1rem .5rem; border-radius: 999px; font-size: .75rem; font-weight: 600; }
    .ok { color: #2e7d32; font-weight: 600; }
    .todo { color: #b26500; font-weight: 600; }
    .down { color: var(--error); font-weight: 600; }
    .btn { display: inline-flex; align-items: center; gap: .25rem; border: none; background: var(--primary); color: var(--on-primary); padding: .5rem 1rem; border-radius: .5rem; font-weight: 600; }
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: .75rem; }
    .card { display: flex; gap: .75rem; align-items: center; padding: 1rem; background: var(--surface-container-lowest); border: 1px solid var(--outline-variant); border-radius: .75rem; }
    .card:hover { border-color: var(--primary); }
    .card .material-symbols-outlined { color: var(--primary); }
    .card-title { font-weight: 600; }
    .card-part { font-size: .75rem; color: var(--on-surface-variant); }
  `]
})
export class HomePageComponent implements OnInit {
  private readonly probe = inject(ApiProbeService);
  protected readonly identity = inject(IdentityService);

  readonly pages = computed(() => pagesFor(this.identity.role()));
  readonly rows = signal<{ check: ServiceCheck; result: ProbeResult | null }[]>(CHECKS.map(check => ({ check, result: null })));
  readonly loading = signal(false);

  ngOnInit(): void {
    this.check();
  }

  check(): void {
    this.loading.set(true);
    forkJoin(CHECKS.map(c => this.probe.probe(c.call))).subscribe(results => {
      this.rows.set(CHECKS.map((check, i) => ({ check, result: results[i] })));
      this.loading.set(false);
    });
  }

  partName(part: Part): string {
    return PARTS[part];
  }

  tone(r: ProbeResult | null): string {
    if (!r) return '';
    if (r.status >= 200 && r.status < 300) return 'ok';
    if (r.status === 501) return 'todo';
    return 'down';
  }

  label(r: ProbeResult | null): string {
    if (!r) return 'đang kiểm tra…';
    if (r.status >= 200 && r.status < 300) return r.status + ' — đã chạy';
    if (r.status === 501) return '501 — chưa làm';
    return (r.status || 'không kết nối') + ' — không phản hồi';
  }
}
