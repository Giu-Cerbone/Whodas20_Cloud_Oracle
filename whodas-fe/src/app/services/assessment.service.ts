import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ItemResponse {
  itemCode?: string;
  score?: number;
  days?: number;
  note?: string;
}

export interface Assessment {
  id?: number;
  patientId: string;
  version?: string;
  status?: string;
  mode?: string;
  language?: string;
  interviewer?: string;
  notes?: string;
  responses?: ItemResponse[];
  createdAt?: string;
  updatedAt?: string;
  completedAt?: string;
  totalScore?: number;
  overallPercentDisability?: number;
  overallSimpleScore?: number;
  overallComplexScore?: number;
  overallAverageScore?: number;
  totalItems?: number;
  answeredItems?: number;
  missingItems?: number;
  interpretation?: string;
  compileTypeId?: number;
}

export interface DomainScore {
  domain?: string;
  domainLabel?: string;
  domainLabelIt?: string;
  itemCount?: number;
  rawScore?: number;
  averageScore?: number;
  simpleScore?: number;
  complexScore?: number;
  percentDisability?: number;
}

export interface AssessmentResult {
  assessmentId?: number;
  patientId?: string;
  patientName?: string;
  version?: string;
  status?: string;
  totalItems?: number;
  answeredItems?: number;
  missingItems?: number;
  overallSimpleScore?: number;
  overallComplexScore?: number;
  overallAverageScore?: number;
  overallPercentDisability?: number;
  domainScores?: DomainScore[];
  interpretation?: string;
}

export interface TypeCompileQuest {
  id?: number;
  descType?: string;
  specificDesc?: string;
  additionalNotes?: string;
}

@Injectable({providedIn: 'root'})
export class AssessmentService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/assessments`;
  private typeCompileQuestUrl = `${environment.baseUrl}/${environment.apiVersion}/type-compile-quest`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Assessment[]> {
    return this.http.get<Assessment[]>(this.baseUrl);
  }

  findById(id: number): Observable<Assessment> {
    return this.http.get<Assessment>(`${this.baseUrl}/${id}`);
  }

  findByPatientId(patientId: string): Observable<Assessment[]> {
    return this.http.get<Assessment[]>(`${this.baseUrl}/patient/${patientId}`);
  }

  create(assessment: Assessment): Observable<Assessment> {
    return this.http.post<Assessment>(this.baseUrl, assessment);
  }

  update(id: number, assessment: Assessment): Observable<Assessment> {
    return this.http.put<Assessment>(`${this.baseUrl}/${id}`, assessment);
  }

  submitAndScore(id: number): Observable<AssessmentResult> {
    return this.http.put<AssessmentResult>(`${this.baseUrl}/${id}/submit`, {});
  }

  score(id: number): Observable<AssessmentResult> {
    return this.http.put<AssessmentResult>(`${this.baseUrl}/${id}/score`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Recupera la lista dei tipi di compilazione del questionario. */
  getTypeCompileQuests(): Observable<TypeCompileQuest[]> {
    return this.http.get<TypeCompileQuest[]>(this.typeCompileQuestUrl);
  }

  /** Recupera un singolo tipo di compilazione per ID. */
  getTypeCompileQuest(id: number): Observable<TypeCompileQuest> {
    return this.http.get<TypeCompileQuest>(`${this.typeCompileQuestUrl}/${id}`);
  }
}