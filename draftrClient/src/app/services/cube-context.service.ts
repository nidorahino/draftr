import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CubeDetail } from '../models/cube-detail';

@Injectable({ providedIn: 'root' })
export class CubeContextService {
  private readonly cube$ = new BehaviorSubject<CubeDetail | null>(null);

  setCube(cube: CubeDetail) {
    this.cube$.next(cube);
  }

  getCube() {
    return this.cube$.asObservable();
  }

  snapshot() {
    return this.cube$.value;
  }

  clear() {
    this.cube$.next(null);
  }
}
