import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeAuditComponent } from './cube-audit.component';

describe('CubeAuditComponent', () => {
  let component: CubeAuditComponent;
  let fixture: ComponentFixture<CubeAuditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeAuditComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeAuditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
