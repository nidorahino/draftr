import { Injectable } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly key = 'theme';

  init(): void {
    const saved = localStorage.getItem(this.key) as Theme | null;
    if (saved) {
      this.set(saved);
      return;
    }

    const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches;
    this.set(prefersDark ? 'dark' : 'light');
  }

  get(): Theme {
    return (document.documentElement.getAttribute('data-theme') as Theme) ?? 'light';
  }

  toggle(): void {
    this.set(this.get() === 'dark' ? 'light' : 'dark');
  }

  set(theme: Theme): void {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(this.key, theme);
  }
}
