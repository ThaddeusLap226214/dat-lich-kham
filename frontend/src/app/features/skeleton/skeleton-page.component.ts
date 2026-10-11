import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AppPage, PARTS } from '../../core/app-pages';
import { ApiProbeService, ProbeResult } from '../../core/api-probe.service';
import { ProbeResultComponent } from './probe-result.component';

// Trang chờ chung: mô tả trang, phần việc phụ trách, việc cần làm, và nút gọi thử API thật qua Gateway
@Component({
  selector: 'app-skeleton-page',
  standalone: true,
  imports: [ProbeResultComponent, RouterLink],
  template: `
    @if (page(); as p) {
      <section class="ui-page skeleton">
        <span class="ui-badge ui-tone-info ui-badge-plain">{{ p.part }} · {{ partName() }}</span>
        <header class="ui-page-header">
          <div>
            <h1 class="ui-page-title">{{ p.title }}</h1>
            <p class="ui-page-subtitle">{{ p.summary }}</p>
          </div>
        </header>

        <div class="ui-alert ui-tone-warning">
          <span class="material-symbols-outlined">construction</span>
          <div>
            Trang này là khung — giao diện và phần xử lý do người nhận phần {{ p.part }} viết tiếp.
            Thành phần giao diện dùng chung: <a routerLink="/ui-kit"><strong>Mẫu giao diện (nhóm)</strong></a>.
          </div>
        </div>

        <h2 class="ui-section-title">Việc cần làm</h2>
        <ul>
          @for (t of p.tasks; track t) {
            <li>{{ t }}</li>
          }
        </ul>

        <h2 class="ui-section-title">API chính của trang</h2>
        <div class="api-row">
          <code>{{ p.api.method }} {{ probe.resolveUrl(p.api) }}</code>
          <button class="ui-btn ui-btn-primary" (click)="call(p)" [disabled]="loading()">
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
    .skeleton { max-width: 56rem; gap: 1rem; }
    .skeleton > .ui-badge { align-self: flex-start; }
    .ui-alert a { text-decoration: underline; }
    ul { padding-left: 1.25rem; display: flex; flex-direction: column; gap: .25rem; font: var(--font-body-md); }
    .api-row { display: flex; gap: 1rem; align-items: center; flex-wrap: wrap; }
    .api-row code { background: var(--surface-container-low); padding: .5rem .75rem; border-radius: .5rem; font-size: .8rem; word-break: break-all; }
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
