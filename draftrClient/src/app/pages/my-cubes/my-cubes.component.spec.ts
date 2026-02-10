import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyCubesComponent } from './my-cubes.component';

describe('MyCubesComponent', () => {
  let component: MyCubesComponent;
  let fixture: ComponentFixture<MyCubesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyCubesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MyCubesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
