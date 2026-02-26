import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardFiltersPanelComponent } from './card-filters-panel.component';

describe('CardFiltersPanelComponent', () => {
  let component: CardFiltersPanelComponent;
  let fixture: ComponentFixture<CardFiltersPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardFiltersPanelComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CardFiltersPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
