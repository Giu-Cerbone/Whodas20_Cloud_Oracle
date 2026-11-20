import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AssessmentPdfService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/assessments`;

  constructor(private http: HttpClient) {}

  downloadPdf(assessmentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${assessmentId}/pdf`, {
      responseType: 'blob'
    });
  }

  viewPdf(assessmentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${assessmentId}/pdf/view`, {
      responseType: 'blob'
    });
  }

  downloadPdfBlob(assessmentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${assessmentId}/pdf`, {
      responseType: 'blob'
    });
  }

  viewPdfBlob(assessmentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${assessmentId}/pdf/view`, {
      responseType: 'blob'
    });
  }

  triggerDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  triggerPrint(blob: Blob): void {
    const url = window.URL.createObjectURL(blob);
    const printWindow = window.open(url, '_blank');
    if (printWindow) {
      printWindow.onload = () => {
        setTimeout(() => {
          printWindow.print();
        }, 500);
      };
    }
  }
}