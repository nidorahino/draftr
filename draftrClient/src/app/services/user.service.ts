import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface User {
  userId: number;    
  username: string;
  email?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly API = `${environment.apiBaseUrl}/api/users`;

  constructor(private http: HttpClient) {}

  me() {
    return this.http.get<User>(`${this.API}/me`);
  }
}
