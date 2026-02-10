import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeShellComponent } from './cube-shell.component';

describe('CubeShellComponent', () => {
  let component: CubeShellComponent;
  let fixture: ComponentFixture<CubeShellComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeShellComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
