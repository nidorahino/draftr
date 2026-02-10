import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface User {
  userId: number;    
  username: string;
  email?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly API = 'http://localhost:8080/api/users';

  constructor(private http: HttpClient) {}

  me() {
    return this.http.get<User>(`${this.API}/me`);
  }
}
