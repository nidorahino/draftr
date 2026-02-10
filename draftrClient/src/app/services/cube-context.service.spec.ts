import { TestBed } from '@angular/core/testing';

import { CubeContextService } from './cube-context.service';

describe('CubeContextServiceService', () => {
  let service: CubeContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CubeContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
