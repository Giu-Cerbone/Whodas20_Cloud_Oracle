import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface QuestionnaireItem {
  code?: string;
  text?: string;
  domain?: string;
  domainLabel?: string;
  domainLabelIt?: string;
  icfCode?: string;
  itemGroup?: string;
  orderNum?: number;
}

@Injectable({providedIn: 'root'})
export class QuestionnaireService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/questionnaire`;

  constructor(private http: HttpClient) {}

  getAllItems(): Observable<QuestionnaireItem[]> {
    return this.http.get<QuestionnaireItem[]>(`${this.baseUrl}/items`);
  }

  getItem(itemCode: string): Observable<QuestionnaireItem> {
    return this.http.get<QuestionnaireItem>(`${this.baseUrl}/items/${itemCode}`);
  }

  getItemsByDomain(domain: string): Observable<QuestionnaireItem[]> {
    return this.http.get<QuestionnaireItem[]>(`${this.baseUrl}/items/domain/${domain}`);
  }

  getItemsByDomains(domains: string[]): Observable<QuestionnaireItem[]> {
    return this.http.get<QuestionnaireItem[]>(`${this.baseUrl}/items/domains`, { params: { domains } });
  }

  createItem(item: QuestionnaireItem): Observable<QuestionnaireItem> {
    return this.http.post<QuestionnaireItem>(`${this.baseUrl}/items`, item);
  }

  updateItem(itemCode: string, item: QuestionnaireItem): Observable<QuestionnaireItem> {
    return this.http.put<QuestionnaireItem>(`${this.baseUrl}/items/${itemCode}`, item);
  }

  deleteItem(itemCode: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/items/${itemCode}`);
  }
}