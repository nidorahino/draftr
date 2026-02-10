import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeDraftComponent } from './cube-draft.component';

describe('CubeDraftComponent', () => {
  let component: CubeDraftComponent;
  let fixture: ComponentFixture<CubeDraftComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeDraftComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeDraftComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
