import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardDetailsModalComponent } from './card-details-modal.component';

describe('CardDetailsModalComponent', () => {
  let component: CardDetailsModalComponent;
  let fixture: ComponentFixture<CardDetailsModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardDetailsModalComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CardDetailsModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
