import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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

  getNextCertificateNumber(): Observable<string> {
    return this.getAll().pipe(
      map(certs => {
        const currentYear = new Date().getFullYear();
        if (!certs.length) {
          return `WHODAS-${currentYear}-0001`;
        }
        const sorted = [...certs].sort((a, b) => {
          const numA = this.extractSequenceNumber(a.certificateNumber);
          const numB = this.extractSequenceNumber(b.certificateNumber);
          return numB - numA;
        });
        const lastNumber = sorted[0].certificateNumber;
        const lastYear = this.extractYear(lastNumber);
        // If year changed, reset sequence to 1
        if (lastYear !== currentYear) {
          return `WHODAS-${currentYear}-0001`;
        }
        const nextSeq = this.extractSequenceNumber(lastNumber) + 1;
        // Determine padding length from the longest sequence in the data
        const maxSeqLen = Math.max(4, ...certs.map(c => this.extractSequenceNumber(c.certificateNumber).toString().length));
        return `WHODAS-${currentYear}-${nextSeq.toString().padStart(maxSeqLen, '0')}`;
      })
    );
  }

  private extractSequenceNumber(certNum?: string): number {
    if (!certNum) return 0;
    const match = certNum.match(/-(\d+)$/);
    return match ? parseInt(match[1], 10) : 0;
  }

  private extractYear(certNum?: string): number | null {
    if (!certNum) return null;
    const match = certNum.match(/WHODAS-(\d{4})/);
    return match ? parseInt(match[1], 10) : null;
  }
}