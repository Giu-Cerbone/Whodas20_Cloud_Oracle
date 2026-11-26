import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface Patient {
  id?: number;
  firstName: string;
  lastName: string;
  fiscalCode?: string;
  birthDate?: string;
  birthPlace?: string;
  gender?: string;
  address?: string;
  phone?: string;
  email?: string;
  healthCondition?: string;
  notes?: string;
  registeredUserUid?: string;
}

@Injectable({providedIn: 'root'})
export class PatientService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/patients`;
  private patientsSubject = new BehaviorSubject<Patient[]>([]);

  constructor(private http: HttpClient) {}

  getAll(): Observable<Patient[]> {
    return this.http.get<Patient[]>(this.baseUrl).pipe(
      tap(patients => this.patientsSubject.next(patients))
    );
  }

  findById(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.baseUrl}/${id}`);
  }

  findByFiscalCode(fiscalCode: string): Observable<Patient> {
    return this.http.get<Patient>(`${this.baseUrl}/fiscal-code/${fiscalCode}`);
  }

  searchByLastName(lastName: string): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.baseUrl}/search/last-name`, { params: { lastName } });
  }

  create(patient: Patient): Observable<Patient> {
    return this.http.post<Patient>(this.baseUrl, patient);
  }

  update(id: number, patient: Patient): Observable<Patient> {
    return this.http.put<Patient>(`${this.baseUrl}/${id}`, patient);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getPatients(): Observable<Patient[]> {
    return this.patientsSubject.asObservable();
  }
}