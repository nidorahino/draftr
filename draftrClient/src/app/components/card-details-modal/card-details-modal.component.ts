import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CubeCardDetails } from '../../models/cube-card-details';

@Component({
  selector: 'app-card-details-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-details-modal.component.html',
})
export class CardDetailsModalComponent {
  @Input() open = false;
  @Input() card: CubeCardDetails | null = null;

  // optional context for actions
  @Input() isAdmin = false;
  @Input() maxQty: number | null = null;   // if you want to show/edit
  @Input() qty: number | null = null;      // for collection

  @Output() closed = new EventEmitter<void>();

  // optional actions
  @Output() banToggle = new EventEmitter<void>();
  @Output() remove = new EventEmitter<void>();
}
