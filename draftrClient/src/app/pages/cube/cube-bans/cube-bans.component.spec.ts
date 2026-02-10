import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeBansComponent } from './cube-bans.component';

describe('CubeBansComponent', () => {
  let component: CubeBansComponent;
  let fixture: ComponentFixture<CubeBansComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeBansComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeBansComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
