import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Patient } from './patient.service';

export type ExtractionMethod = 'ACROFORM' | 'PDF_TEXT' | 'OCR' | 'DOCX' | 'DOC' | 'XLSX' | 'XLS' | 'NONE';

export type DocumentImportStatus = 'IMPORTED' | 'ANALYZED_ONLY' | 'FAILED';

export type DocumentDisposition = 'attachment' | 'inline';

/** Etichette italiane dei metodi di estrazione, per la UI. */
export const EXTRACTION_METHOD_LABELS: Record<ExtractionMethod, string> = {
  ACROFORM: 'Modulo PDF compilato (AcroForm)',
  PDF_TEXT: 'Testo del PDF',
  OCR: 'Riconoscimento ottico (OCR)',
  DOCX: 'Documento Word (docx)',
  DOC: 'Documento Word (doc)',
  XLSX: 'Foglio Excel (xlsx)',
  XLS: 'Foglio Excel (xls)',
  NONE: 'Nessuna estrazione'
};

/** Etichette italiane degli stati di importazione, per la UI. */
export const IMPORT_STATUS_LABELS: Record<DocumentImportStatus, string> = {
  IMPORTED: 'Importato',
  ANALYZED_ONLY: 'Solo analizzato',
  FAILED: 'Importazione fallita'
};

export interface DocumentTemplate {
  code: string;
  label: string;
  description?: string;
  itemCount?: number;
  language?: string;
}

export interface DocumentTemplateParams {
  patientId?: number;
  compileTypeId?: number;
  interviewer?: string;
  assessmentDate?: string;
  fillAnswers?: boolean;
  disposition?: DocumentDisposition;
}

export interface DocumentImportOptions {
  patientId?: number;
  compileTypeId?: number;
  interviewer?: string;
  createPatientIfMissing?: boolean;
  saveAssessment?: boolean;
  submitAndScore?: boolean;
}

export interface ImportedPatientRef {
  id?: number;
  firstName?: string;
  lastName?: string;
  fiscalCode?: string;
  created?: boolean;
}

export interface ImportedAssessmentRef {
  id?: number;
  status?: string;
  overallPercentDisability?: number | null;
  interpretation?: string;
  answersImported?: number;
  created?: boolean;
}

export interface ExtractedDocumentField {
  key?: string;
  label?: string;
  rawValue?: string;
  normalizedValue?: string;
  confidence?: number;
  source?: string;
}

export interface ExtractedDocumentItem {
  itemCode?: string;
  domain?: string;
  domainLabelIt?: string;
  questionTextIt?: string;
  rawValue?: string;
  score?: number | null;
  confidence?: number;
  source?: string;
}

export interface DocumentImportResult {
  importId?: number;
  fileName?: string;
  fileType?: string;
  fileSizeBytes?: number;
  extractionMethod?: ExtractionMethod;
  status?: DocumentImportStatus;
  compileTypeId?: number;
  patient?: ImportedPatientRef;
  assessment?: ImportedAssessmentRef;
  fields?: ExtractedDocumentField[];
  items?: ExtractedDocumentItem[];
  warnings?: string[];
  extractedTextLength?: number;
  createdAt?: string;
  downloadUrl?: string;
}

export interface DocumentImportSummary {
  id?: number;
  fileName?: string;
  fileType?: string;
  extractionMethod?: ExtractionMethod;
  status?: DocumentImportStatus;
  patientId?: number;
  patientName?: string;
  assessmentId?: number;
  answersImported?: number;
  fileSizeBytes?: number;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/documents`;

  constructor(private http: HttpClient) {}

  /** Elenco dei modelli di documento WHODAS 2.0 disponibili. */
  getTemplates(): Observable<DocumentTemplate[]> {
    return this.http.get<DocumentTemplate[]>(`${this.baseUrl}/templates`);
  }

  /** Genera il PDF del modello come allegato (disposition=attachment). */
  downloadTemplate(params: DocumentTemplateParams): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/template.pdf`, {
      params: this.buildTemplateParams({ ...params, disposition: 'attachment' }),
      responseType: 'blob'
    });
  }

  /** Genera il PDF del modello per anteprima/stampa (disposition=inline). */
  viewTemplate(params: DocumentTemplateParams): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/template.pdf`, {
      params: this.buildTemplateParams({ ...params, disposition: 'inline' }),
      responseType: 'blob'
    });
  }

  /** Importa un documento compilato (pdf/doc/docx/xls/xlsx) e ne estrae i dati. */
  importDocument(file: File, options: DocumentImportOptions): Observable<DocumentImportResult> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    if (options.patientId !== undefined && options.patientId !== null) {
      formData.append('patientId', String(options.patientId));
    }
    if (options.compileTypeId !== undefined && options.compileTypeId !== null) {
      formData.append('compileTypeId', String(options.compileTypeId));
    }
    if (options.interviewer) {
      formData.append('interviewer', options.interviewer);
    }
    formData.append('createPatientIfMissing', String(options.createPatientIfMissing !== false));
    formData.append('saveAssessment', String(options.saveAssessment !== false));
    formData.append('submitAndScore', String(options.submitAndScore !== false));
    return this.http.post<DocumentImportResult>(`${this.baseUrl}/import`, formData);
  }

  /** Storico delle importazioni, con paginazione lato server. */
  getImports(limit = 20, offset = 0): Observable<DocumentImportSummary[]> {
    const params = new HttpParams()
      .set('limit', String(limit))
      .set('offset', String(offset));
    return this.http.get<DocumentImportSummary[]>(`${this.baseUrl}/imports`, { params });
  }

  /** Dettaglio completo di una singola importazione. */
  getImport(id: number): Observable<DocumentImportResult> {
    return this.http.get<DocumentImportResult>(`${this.baseUrl}/imports/${id}`);
  }

  /** File originale di una singola importazione. */
  getImportFile(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/imports/${id}/file`, { responseType: 'blob' });
  }

  /** Elimina una singola importazione. */
  deleteImport(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/imports/${id}`);
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

  /** Nome file del modello generato: whodas2_modello_<CF o NOCF>_<yyyy-MM-dd>.pdf */
  buildTemplateFilename(template: DocumentTemplate, patient?: Patient, date?: string): string {
    const fiscalCode = patient?.fiscalCode || 'NOCF';
    const dateStr = date || new Date().toISOString().slice(0, 10);
    const templateCode = (template?.code || 'whodas2').replace(/[^A-Za-z0-9]+/g, '-');
    return `whodas2_modello_${templateCode}_${fiscalCode}_${dateStr}.pdf`;
  }

  private buildTemplateParams(params: DocumentTemplateParams): HttpParams {
    let httpParams = new HttpParams();
    if (params.patientId !== undefined && params.patientId !== null) {
      httpParams = httpParams.set('patientId', String(params.patientId));
    }
    if (params.compileTypeId !== undefined && params.compileTypeId !== null) {
      httpParams = httpParams.set('compileTypeId', String(params.compileTypeId));
    }
    if (params.interviewer) {
      httpParams = httpParams.set('interviewer', params.interviewer);
    }
    if (params.assessmentDate) {
      httpParams = httpParams.set('assessmentDate', params.assessmentDate);
    }
    if (params.fillAnswers !== undefined) {
      httpParams = httpParams.set('fillAnswers', String(params.fillAnswers));
    }
    if (params.disposition) {
      httpParams = httpParams.set('disposition', params.disposition);
    }
    return httpParams;
  }
}
