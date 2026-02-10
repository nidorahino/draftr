import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeCardsComponent } from './cube-cards.component';

describe('CubeCardsComponent', () => {
  let component: CubeCardsComponent;
  let fixture: ComponentFixture<CubeCardsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeCardsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeCardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
