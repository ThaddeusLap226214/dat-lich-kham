import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AppPage, PARTS } from '../../core/app-pages';
import { ApiProbeService, ProbeResult } from '../../core/api-probe.service';
import { ProbeResultComponent } from './probe-result.component';

// Trang chờ chung: mô tả trang, phần việc phụ trách, việc cần làm, và nút gọi thử API thật qua Gateway
@Component({
  selector: 'app-skeleton-page',
  standalone: true,
  imports: [ProbeResultComponent],
  template: `
    @if (page(); as p) {
      <section class="skeleton">
        <div class="badge">{{ p.part }} · {{ partName() }}</div>
        <h1>{{ p.title }}</h1>
        <p class="summary">{{ p.summary }}</p>

        <div class="notice">
          <span class="material-symbols-outlined">construction</span>
          Trang này là khung — giao diện và phần xử lý do người nhận phần {{ p.part }} viết tiếp.
        </div>

        <h2>Việc cần làm</h2>
        <ul>
          @for (t of p.tasks; track t) {
            <li>{{ t }}</li>
          }
        </ul>

        <h2>API chính của trang</h2>
        <div class="api-row">
          <code>{{ p.api.method }} {{ probe.resolveUrl(p.api) }}</code>
          <button class="btn" (click)="call(p)" [disabled]="loading()">
            <span class="material-symbols-outlined">play_arrow</span> Gọi thử
          </button>
        </div>
        @if (result(); as r) {
          <app-probe-result [result]="r" />
        }
      </section>
    }
  `,
  styles: [`
    .skeleton { max-width: 56rem; display: flex; flex-direction: column; gap: 1rem; }
    .badge { align-self: flex-start; background: var(--primary-fixed); color: var(--on-primary-fixed-variant); padding: .25rem .75rem; border-radius: 999px; font-size: .75rem; font-weight: 600; }
    h1 { font: var(--font-headline-md); }
    h2 { font: var(--font-title-lg); font-size: 1rem; margin-top: .5rem; }
    .summary { color: var(--on-surface-variant); }
    .notice { display: flex; gap: .5rem; align-items: center; background: #fff4e0; color: #7a4a00; padding: .75rem 1rem; border-radius: .75rem; font-size: .875rem; }
    ul { padding-left: 1.25rem; display: flex; flex-direction: column; gap: .25rem; }
    .api-row { display: flex; gap: 1rem; align-items: center; flex-wrap: wrap; }
    .api-row code { background: var(--surface-container-low); padding: .5rem .75rem; border-radius: .5rem; font-size: .8rem; word-break: break-all; }
    .btn { display: inline-flex; align-items: center; gap: .25rem; border: none; background: var(--primary); color: var(--on-primary); padding: .5rem 1rem; border-radius: .5rem; font-weight: 600; }
    .btn:disabled { opacity: .6; }
  `]
})
export class SkeletonPageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly probe = inject(ApiProbeService);

  readonly page = toSignal(this.route.data.pipe(map(d => d['page'] as AppPage)));
  readonly partName = computed(() => {
    const p = this.page();
    return p ? PARTS[p.part] : '';
  });
  readonly result = signal<ProbeResult | null>(null);
  readonly loading = signal(false);

  call(page: AppPage): void {
    this.loading.set(true);
    this.probe.probe(page.api).subscribe(r => {
      this.result.set(r);
      this.loading.set(false);
    });
  }
}
