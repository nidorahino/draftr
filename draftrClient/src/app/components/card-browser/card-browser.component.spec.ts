import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CardBrowserComponent } from './card-browser.component';

type TestItem = { id: number; name?: string };

describe('CardBrowserComponent', () => {
  let component: CardBrowserComponent<TestItem>;
  let fixture: ComponentFixture<CardBrowserComponent<TestItem>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardBrowserComponent]
    }).compileComponents();

    fixture = TestBed.createComponent<CardBrowserComponent<TestItem>>(CardBrowserComponent);

    component = fixture.componentInstance;

    // required input
    component.getId = (it: TestItem) => it.id;

    // optional: prevent "empty" template paths if you want
    component.items = [{ id: 1, name: 'Blue-Eyes' }];

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});