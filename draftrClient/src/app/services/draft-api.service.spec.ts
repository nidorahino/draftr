import { TestBed } from '@angular/core/testing';

import { DraftApiService } from './draft-api.service';

describe('DraftApiService', () => {
  let service: DraftApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DraftApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
