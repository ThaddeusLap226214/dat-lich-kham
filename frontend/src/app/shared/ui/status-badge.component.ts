import { Component, computed, input } from '@angular/core';
import { StatusKind, statusStyle } from './status';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span [class]="'ui-badge ui-tone-' + style().tone">{{ style().label }}</span>`
})
export class StatusBadgeComponent {
  readonly kind = input.required<StatusKind>();
  readonly value = input.required<string | boolean | null | undefined>();

  readonly style = computed(() => statusStyle(this.kind(), this.value()));
}
