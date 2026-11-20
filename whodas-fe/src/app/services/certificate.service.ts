import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DomainScore } from './assessment.service';

export interface Certificate {
  id?: number;
  certificateNumber?: string;
  assessmentId?: number;
  patientId?: string;
  patientName?: string;
  issueDate?: string;
  validUntil?: string;
  status?: string;
  overallPercentDisability?: number;
  overallComplexScore?: number;
  certifyingBody?: string;
  doctorName?: string;
  doctorLicense?: string;
  notes?: string;
  domainScores?: DomainScore[];
}

@Injectable({providedIn: 'root'})
export class CertificateService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/certificates`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(this.baseUrl);
  }

  findById(id: number): Observable<Certificate> {
    return this.http.get<Certificate>(`${this.baseUrl}/${id}`);
  }

  findByCertificateNumber(certificateNumber: string): Observable<Certificate> {
    return this.http.get<Certificate>(`${this.baseUrl}/number/${certificateNumber}`);
  }

  findByPatientId(patientId: string): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(`${this.baseUrl}/patient/${patientId}`);
  }

  findByAssessmentId(assessmentId: number): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(`${this.baseUrl}/assessment/${assessmentId}`);
  }

  create(certificate: Certificate): Observable<Certificate> {
    return this.http.post<Certificate>(this.baseUrl, certificate);
  }

  update(id: number, certificate: Certificate): Observable<Certificate> {
    return this.http.put<Certificate>(`${this.baseUrl}/${id}`, certificate);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}