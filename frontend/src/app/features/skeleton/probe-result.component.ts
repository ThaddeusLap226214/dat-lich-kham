import { Component, computed, input } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { ProbeResult } from '../../core/api-probe.service';

@Component({
  selector: 'app-probe-result',
  standalone: true,
  imports: [JsonPipe],
  template: `
    <div class="probe" [class]="'probe ' + tone()">
      <div class="probe-head">
        <span class="probe-status">{{ result().status || 'Không kết nối được' }}</span>
        <span class="probe-meaning">{{ meaning() }}</span>
      </div>
      <code class="probe-url">{{ result().url }}</code>
      @if (result().body) {
        <pre class="probe-body">{{ result().body | json }}</pre>
      }
    </div>
  `,
  styles: [`
    .probe { border-radius: .75rem; padding: 1rem; border: 1px solid var(--outline-variant); background: var(--surface-container-lowest); }
    .probe.ok { border-color: #2e7d32; }
    .probe.todo { border-color: #ed8b00; }
    .probe.down { border-color: var(--error); }
    .probe-head { display: flex; gap: .75rem; align-items: baseline; margin-bottom: .5rem; }
    .probe-status { font-weight: 700; font-size: 1.25rem; }
    .ok .probe-status { color: #2e7d32; }
    .todo .probe-status { color: #b26500; }
    .down .probe-status { color: var(--error); }
    .probe-meaning { color: var(--on-surface-variant); font-size: .875rem; }
    .probe-url { font-size: .75rem; color: var(--outline); word-break: break-all; }
    .probe-body { margin-top: .75rem; max-height: 16rem; overflow: auto; font-size: .75rem; background: var(--surface-container-low); padding: .75rem; border-radius: .5rem; white-space: pre-wrap; }
  `]
})
export class ProbeResultComponent {
  readonly result = input.required<ProbeResult>();

  readonly tone = computed(() => {
    const s = this.result().status;
    if (s >= 200 && s < 300) return 'ok';
    if (s === 501 || (s >= 400 && s < 500)) return 'todo';
    return 'down';
  });

  readonly meaning = computed(() => {
    const s = this.result().status;
    if (s >= 200 && s < 300) return 'Đã chạy — service trả dữ liệu thật';
    if (s === 501) return 'Service chạy, đường đi qua Gateway đúng — phần xử lý chưa viết';
    if (s === 401 || s === 403) return 'Bị chặn quyền — thử vai trò khác';
    if (s === 400) return 'Dữ liệu gửi lên chưa hợp lệ';
    if (s === 404) return 'Không có đường dẫn này';
    if (s === 503) return 'Service phía sau không phản hồi (đang khởi động hoặc đã tắt)';
    return 'Không gọi được Gateway — kiểm tra docker compose ps';
  });
}
