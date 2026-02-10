import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeCollectionComponent } from './cube-collection.component';

describe('CubeCollectionComponent', () => {
  let component: CubeCollectionComponent;
  let fixture: ComponentFixture<CubeCollectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeCollectionComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeCollectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
