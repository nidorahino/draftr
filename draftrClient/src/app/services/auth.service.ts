import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

type LoginRequest = { username: string; password: string };
type RegisterRequest = { username: string; email: string; password: string };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';
  private readonly TOKEN_KEY = 'draftr_token';

  constructor(private http: HttpClient) {}

  login(req: LoginRequest) {
    return this.http.post<{ token: string }>(`${this.API}/login`, req).pipe(
      tap((res) => this.setToken(res.token))
    );
  }

  register(req: RegisterRequest) {
    return this.http.post<void>(`${this.API}/register`, req);
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private setToken(token: string) {
    localStorage.setItem(this.TOKEN_KEY, token);
  }
}
