import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface QuestionnaireItem {
  code: string;
  domain: string;
  domainLabel: string;
  text: string;
  order: number;
  conditional?: boolean;
}

export interface QuestionnaireDomain {
  code: string;
  label: string;
  items: QuestionnaireItem[];
}

export interface ItemResponse {
  itemCode: string;
  score: number; // 1-5
}

export interface AssessmentResponse {
  patientId: number;
  version: string;
  responses: ItemResponse[];
  domainScores?: DomainScore[];
  overallSimpleScore?: number;
  overallComplexScore?: number;
  overallPercentDisability?: number;
}

export interface DomainScore {
  domain: string;
  domainLabel: string;
  itemCount: number;
  rawScore: number;
  averageScore: number;
  simpleScore: number;
  complexScore: number;
  percentDisability: number;
}

export interface QuestionnaireState {
  currentItemIndex: number;
  responses: Map<string, number>; // itemCode -> score
  patientId: number;
  startedAt: Date;
}

@Injectable({ providedIn: 'root' })
export class QuestionnaireRunnerService {
  private readonly domains: QuestionnaireDomain[] = [
    {
      code: 'D1',
      label: 'Comprendere e comunicare',
      items: [
        { code: 'D1.1', domain: 'D1', domainLabel: 'Comprendere e comunicare', text: 'Concentrarsi nel fare qualcosa per 10 minuti?', order: 1 },
        { code: 'D1.2', domain: 'D1', domainLabel: 'Comprendere e comunicare', text: 'Ricordarsi di fare cose importanti?', order: 2 },
        { code: 'D1.3', domain: 'D1', domainLabel: 'Comprendere e comunicare', text: 'Analizzare e trovare soluzioni ai problemi della vita quotidiana?', order: 3 },
        { code: 'D1.4', domain: 'D1', domainLabel: 'Comprendere e comunicare', text: 'Imparare a fare cose nuove, come, per esempio, raggiungere un posto nuovo?', order: 4 },
        { code: 'D1.5', domain: 'D1', domainLabel: 'Comprendere e comunicare', text: 'Capire quello che dice la gente?', order: 5 },
        { code: 'D1.6', domain: 'D1', domainLabel: 'Comprendere e comunicare', text: 'Iniziare e portare avanti una conversazione?', order: 6 },
      ]
    },
    {
      code: 'D2',
      label: 'Mobilità',
      items: [
        { code: 'D2.1', domain: 'D2', domainLabel: 'Mobilità', text: 'Stare in piedi a lungo, come per 30 minuti?', order: 7 },
        { code: 'D2.2', domain: 'D2', domainLabel: 'Mobilità', text: 'Alzarsi da una posizione seduta?', order: 8 },
        { code: 'D2.3', domain: 'D2', domainLabel: 'Mobilità', text: 'Muoversi dentro casa?', order: 9 },
        { code: 'D2.4', domain: 'D2', domainLabel: 'Mobilità', text: 'Uscire di casa?', order: 10 },
        { code: 'D2.5', domain: 'D2', domainLabel: 'Mobilità', text: 'Camminare per una lunga distanza, come per un chilometro?', order: 11 },
      ]
    },
    {
      code: 'D3',
      label: 'Cura di Sé',
      items: [
        { code: 'D3.1', domain: 'D3', domainLabel: 'Cura di Sé', text: 'Lavarsi tutto il corpo?', order: 12 },
        { code: 'D3.2', domain: 'D3', domainLabel: 'Cura di Sé', text: 'Vestirsi?', order: 13 },
        { code: 'D3.3', domain: 'D3', domainLabel: 'Cura di Sé', text: 'Mangiare?', order: 14 },
        { code: 'D3.4', domain: 'D3', domainLabel: 'Cura di Sé', text: 'Stare da solo per qualche giorno?', order: 15 },
      ]
    },
    {
      code: 'D4',
      label: 'Relazionarsi con le persone',
      items: [
        { code: 'D4.1', domain: 'D4', domainLabel: 'Relazionarsi con le persone', text: 'Interagire con le persone che non conosce?', order: 16 },
        { code: 'D4.2', domain: 'D4', domainLabel: 'Relazionarsi con le persone', text: "Mantenere un'amicizia?", order: 17 },
        { code: 'D4.3', domain: 'D4', domainLabel: 'Relazionarsi con le persone', text: 'Andare d\'accordo con le persone con cui è legato?', order: 18 },
        { code: 'D4.4', domain: 'D4', domainLabel: 'Relazionarsi con le persone', text: 'Fare nuove amicizie?', order: 19 },
        { code: 'D4.5', domain: 'D4', domainLabel: 'Relazionarsi con le persone', text: 'Attività sessuale?', order: 20 },
      ]
    },
    {
      code: 'D5',
      label: 'Attività della vita quotidiana',
      items: [
        { code: 'D5.1', domain: 'D5', domainLabel: 'Attività della vita quotidiana', text: 'Occuparsi della casa e della famiglia per quanto è di sua responsabilità?', order: 21 },
        { code: 'D5.2', domain: 'D5', domainLabel: 'Attività della vita quotidiana', text: 'Svolgere bene le attività domestiche più importanti, tra quelle che spettano a lei?', order: 22 },
        { code: 'D5.3', domain: 'D5', domainLabel: 'Attività della vita quotidiana', text: 'Portare a termine tutte le attività domestiche che lei doveva svolgere?', order: 23 },
        { code: 'D5.4', domain: 'D5', domainLabel: 'Attività della vita quotidiana', text: 'Portare a termine tutte le attività domestiche che le spettano nei tempi per lei usuali?', order: 24 },
        { code: 'D5.5', domain: 'D5', domainLabel: 'Attività della vita quotidiana (lavoro/scuola)', text: 'Svolgere le sue attività lavorative/scolastiche quotidiane?', order: 25, conditional: true },
        { code: 'D5.6', domain: 'D5', domainLabel: 'Attività della vita quotidiana (lavoro/scuola)', text: 'Svolgere bene i compiti lavorativi/scolastici più importanti?', order: 26, conditional: true },
        { code: 'D5.7', domain: 'D5', domainLabel: 'Attività della vita quotidiana (lavoro/scuola)', text: 'Portare a termine tutto quello che doveva fare?', order: 27, conditional: true },
        { code: 'D5.8', domain: 'D5', domainLabel: 'Attività della vita quotidiana (lavoro/scuola)', text: 'Portare a termine tutto quello che doveva fare nei tempi per lei usuali?', order: 28, conditional: true },
      ]
    },
    {
      code: 'D6',
      label: 'Partecipazione alla vita sociale',
      items: [
        { code: 'D6.1', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: 'Quanta difficoltà ha avuto nel prendere parte ad attività comunitarie (per esempio, feste, attività religiose, o di altro tipo) come chiunque altro?', order: 29 },
        { code: 'D6.2', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: 'Quanta difficoltà ha avuto vivere con dignità a causa di barriere o ostacoli nel mondo che la circonda?', order: 30 },
        { code: 'D6.3', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: 'Quanta difficoltà ha avuto a vivere con dignità a causa degli atteggiamenti o delle azioni di altre persone?', order: 31 },
        { code: 'D6.4', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: 'Quanto tempo ha dedicato alle sue condizioni di salute o alle loro conseguenze?', order: 32 },
        { code: 'D6.5', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: 'Quanto è stato coinvolto emotivamente dalle sue condizioni di salute?', order: 33 },
        { code: 'D6.6', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: "Quanto le sue condizioni di salute hanno consumato risorse finanziarie sue o della sua famiglia?", order: 34 },
        { code: 'D6.7', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: 'Quanta difficoltà ha avuto la sua famiglia a causa dei suoi problemi di salute?', order: 35 },
        { code: 'D6.8', domain: 'D6', domainLabel: 'Partecipazione alla vita sociale', text: 'Quanta difficoltà ha avuto nel fare da solo qualcosa per relax o per piacere?', order: 36 },
      ]
    }
  ];

  private readonly scaleLabels = [
    { value: 1, label: 'Nessuna', short: 'Nessuna', class: 'bg-success' },
    { value: 2, label: 'Poca', short: 'Poca', class: 'bg-info' },
    { value: 3, label: 'Moderata', short: 'Moderata', class: 'bg-warning' },
    { value: 4, label: 'Molta', short: 'Molta', class: 'bg-orange' },
    { value: 5, label: 'Moltissima o non ho potuto farlo', short: 'Moltissima/Impossibile', class: 'bg-danger' }
  ];

  private stateSubject = new BehaviorSubject<QuestionnaireState | null>(null);
  private readonly allItems: QuestionnaireItem[] = this.domains.flatMap(d => d.items);

  getDomains(): QuestionnaireDomain[] {
    return this.domains;
  }

  getAllItems(): QuestionnaireItem[] {
    return this.allItems;
  }

  getScaleLabels() {
    return this.scaleLabels;
  }

  getItemByCode(code: string): QuestionnaireItem | undefined {
    return this.allItems.find(i => i.code === code);
  }

  getItemsByDomain(domainCode: string): QuestionnaireItem[] {
    const domain = this.domains.find(d => d.code === domainCode);
    return domain ? domain.items : [];
  }

  initializeQuestionnaire(patientId: number): QuestionnaireState {
    const state: QuestionnaireState = {
      currentItemIndex: 0,
      responses: new Map(),
      patientId,
      startedAt: new Date()
    };
    this.stateSubject.next(state);
    return state;
  }

  getState(): Observable<QuestionnaireState | null> {
    return this.stateSubject.asObservable();
  }

  getCurrentState(): QuestionnaireState | null {
    return this.stateSubject.value;
  }

  setResponse(itemCode: string, score: number): void {
    const state = this.stateSubject.value;
    if (state) {
      state.responses.set(itemCode, score);
      this.stateSubject.next({ ...state });
    }
  }

  getResponse(itemCode: string): number | undefined {
    return this.stateSubject.value?.responses.get(itemCode);
  }

  goToItem(index: number): void {
    const state = this.stateSubject.value;
    if (state && index >= 0 && index < this.allItems.length) {
      state.currentItemIndex = index;
      this.stateSubject.next({ ...state });
    }
  }

  nextItem(): boolean {
    const state = this.stateSubject.value;
    if (state && state.currentItemIndex < this.allItems.length - 1) {
      state.currentItemIndex++;
      this.stateSubject.next({ ...state });
      return true;
    }
    return false;
  }

  previousItem(): boolean {
    const state = this.stateSubject.value;
    if (state && state.currentItemIndex > 0) {
      state.currentItemIndex--;
      this.stateSubject.next({ ...state });
      return true;
    }
    return false;
  }

  getCurrentItem(): QuestionnaireItem | null {
    const state = this.stateSubject.value;
    if (!state) return null;
    return this.allItems[state.currentItemIndex] || null;
  }

  getProgress(): { current: number; total: number; percent: number } {
    const state = this.stateSubject.value;
    if (!state) return { current: 0, total: this.allItems.length, percent: 0 };
    return {
      current: state.currentItemIndex + 1,
      total: this.allItems.length,
      percent: Math.round(((state.currentItemIndex + 1) / this.allItems.length) * 100)
    };
  }

  getAnsweredCount(): number {
    const state = this.stateSubject.value;
    if (!state) return 0;
    return state.responses.size;
  }

  isComplete(): boolean {
    const state = this.stateSubject.value;
    if (!state) return false;
    const applicableItems = this.getApplicableItems();
    return applicableItems.every(item => state.responses.has(item.code));
  }

  getApplicableItems(worksOrStudies?: boolean): QuestionnaireItem[] {
    if (worksOrStudies === undefined) {
      const state = this.stateSubject.value;
      worksOrStudies = state?.responses.has('worksOrStudies') || false;
    }
    return this.allItems.filter(item => 
      !item.conditional || worksOrStudies
    );
  }

  calculateScores(worksOrStudies?: boolean): AssessmentResponse {
    const state = this.stateSubject.value;
    if (!state) throw new Error('Questionnaire not initialized');

    const applicableItems = this.getApplicableItems(worksOrStudies);
    const responses: ItemResponse[] = applicableItems.map(item => ({
      itemCode: item.code,
      score: state.responses.get(item.code) || 0
    })).filter(r => r.score > 0);

    const domainScores: DomainScore[] = this.domains.map(domain => {
      const domainItems = domain.items.filter(item => 
        !item.conditional || worksOrStudies
      );
      const domainResponses = responses.filter(r => 
        domainItems.some(i => i.code === r.itemCode)
      );
      const rawScore = domainResponses.reduce((sum, r) => sum + r.score, 0);
      const itemCount = domainResponses.length;
      const averageScore = itemCount > 0 ? rawScore / itemCount : 0;
      
      // Simple score: sum of item scores (range: itemCount to itemCount*5)
      const simpleScore = rawScore;
      
      // Complex score: transform to 0-100 scale using IRT-like transformation
      // WHODAS uses: (rawScore - minPossible) / (maxPossible - minPossible) * 100
      const minPossible = itemCount * 1;
      const maxPossible = itemCount * 5;
      const complexScore = maxPossible > minPossible 
        ? ((rawScore - minPossible) / (maxPossible - minPossible)) * 100 
        : 0;
      
      // Percent disability
      const percentDisability = complexScore;

      return {
        domain: domain.code,
        domainLabel: domain.label,
        itemCount,
        rawScore,
        averageScore: Math.round(averageScore * 100) / 100,
        simpleScore,
        complexScore: Math.round(complexScore * 100) / 100,
        percentDisability: Math.ceil(Math.round(percentDisability * 1e6) / 1e6)
      };
    });

    const totalRawScore = domainScores.reduce((sum, d) => sum + d.rawScore, 0);
    const totalItems = domainScores.reduce((sum, d) => sum + d.itemCount, 0);
    const minTotal = totalItems * 1;
    const maxTotal = totalItems * 5;
    const overallComplexScore = maxTotal > minTotal 
      ? ((totalRawScore - minTotal) / (maxTotal - minTotal)) * 100 
      : 0;
    const overallSimpleScore = totalRawScore;
    const overallPercentDisability = overallComplexScore;

    return {
      patientId: state.patientId,
      version: 'WHODAS-2.0-36',
      responses,
      domainScores,
      overallSimpleScore,
      overallComplexScore: Math.round(overallComplexScore * 100) / 100,
      overallPercentDisability: Math.ceil(Math.round(overallPercentDisability * 1e6) / 1e6)
    };
  }

  reset(): void {
    this.stateSubject.next(null);
  }

  setWorksOrStudies(value: boolean): void {
    const state = this.stateSubject.value;
    if (state) {
      if (value) {
        state.responses.set('worksOrStudies', 1);
      } else {
        state.responses.delete('worksOrStudies');
      }
      this.stateSubject.next({ ...state });
    }
  }

  getWorksOrStudies(): boolean {
    const state = this.stateSubject.value;
    return state?.responses.has('worksOrStudies') || false;
  }
}