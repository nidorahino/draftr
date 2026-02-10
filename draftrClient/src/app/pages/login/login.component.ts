import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
  error: string | null = null;

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}
  
  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/my-cubes']);
    }
  }

  submit() {
    this.error = null;
    if (this.form.invalid) return;

    this.auth.login(this.form.getRawValue() as any).subscribe({
      next: () => this.router.navigate(['/my-cubes']),
      error: () => (this.error = 'Invalid credentials.'),
    });
  }
}
