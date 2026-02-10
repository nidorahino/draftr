import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeSettingsComponent } from './cube-settings.component';

describe('CubeSettingsComponent', () => {
  let component: CubeSettingsComponent;
  let fixture: ComponentFixture<CubeSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeSettingsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
