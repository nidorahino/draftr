import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CubeMembersComponent } from './cube-members.component';

describe('CubeMembersComponent', () => {
  let component: CubeMembersComponent;
  let fixture: ComponentFixture<CubeMembersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CubeMembersComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CubeMembersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
