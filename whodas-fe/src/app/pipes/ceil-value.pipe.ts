import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'ceilValue', standalone: true })
export class CeilValuePipe implements PipeTransform {
  transform(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const n = Number(value);
    return Number.isFinite(n) ? Math.round(n * 100) / 100 : null;
  }
}
