import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';

import { ErrorModalService } from '../../services/error-modal.service';
import { Patient, PatientService } from '../../services/patient.service';
import { AssessmentService, Assessment } from '../../services/assessment.service';
import { QuestionnaireRunnerService, QuestionnaireItem, QuestionnaireDomain, QuestionnaireState, DomainScore } from '../../services/questionnaire-runner.service';

@Component({
  selector: 'app-questionnaire-runner',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './questionnaire-runner.component.html',
  styleUrls: ['./questionnaire-runner.component.css']
})
export class QuestionnaireRunnerComponent implements OnInit, OnDestroy {
  patient: Patient | null = null;
  patientId: number | null = null;
  compileTypeId: number | null = null;
  
  domains: QuestionnaireDomain[] = [];
  currentItem: QuestionnaireItem | null = null;
  currentItemIndex = 0;
  progress = { current: 0, total: 36, percent: 0 };
  
  responses = new Map<string, number>();
  worksOrStudies = false;
  showWorksOrStudiesQuestion = true;
  
  isSubmitting = false;
  isComplete = false;
  showResults = false;
  assessmentResult: any = null;
  domainScores: DomainScore[] = [];
  
  private subscriptions = new Subscription();
  private scaleLabels = [
    { value: 1, label: 'Nessuna', short: 'Nessuna', class: 'btn-outline-success' },
    { value: 2, label: 'Poca', short: 'Poca', class: 'btn-outline-info' },
    { value: 3, label: 'Moderata', short: 'Moderata', class: 'btn-outline-warning' },
    { value: 4, label: 'Molta', short: 'Molta', class: 'btn-outline-orange' },
    { value: 5, label: 'Moltissima o non ho potuto farlo', short: 'Moltissima/Impossibile', class: 'btn-outline-danger' }
  ];

  constructor(
    private route:ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private assessmentService: AssessmentService,
    private questionnaireService: QuestionnaireRunnerService,
    private errorModalService: ErrorModalService
  ) {}

  ngOnInit(): void {
    this.domains = this.questionnaireService.getDomains();
    this.scaleLabels = this.questionnaireService.getScaleLabels();
    
    // Recupera il tipo di compilazione dallo stato di navigazione
    const navState = history.state;
    if (navState && navState.compileTypeId) {
      this.compileTypeId = navState.compileTypeId;
    }
    
this.subscriptions.add(
      this.route.paramMap.subscribe(params => {
        const id = params.get('patientId');
        if (id) {
          this.patientId = +id;
          this.loadPatient(this.patientId);
          this.questionnaireService.initializeQuestionnaire(this.patientId);
          this.subscribeToQuestionnaireState();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private loadPatient(id: number): void {
    this.patientService.findById(id).subscribe({
      next: patient => this.patient = patient,
      error: () => this.errorModalService.showError('Paziente non trovato')
    });
  }

  private subscribeToQuestionnaireState(): void {
    this.subscriptions.add(
      this.questionnaireService.getState().subscribe(state => {
        if (state) {
          this.currentItemIndex = state.currentItemIndex;
          this.currentItem = this.questionnaireService.getCurrentItem();
          this.responses = new Map(state.responses);
          this.worksOrStudies = state.responses.has('worksOrStudies');
          this.showWorksOrStudiesQuestion = this.currentItemIndex === 24 && !this.worksOrStudies; // After D5.4
          this.progress = this.questionnaireService.getProgress();
          this.isComplete = this.questionnaireService.isComplete();
        }
      })
    );
  }

  onScoreSelect(score: number): void {
    if (this.currentItem) {
      this.questionnaireService.setResponse(this.currentItem.code, score);
    }
  }

  getResponse(itemCode: string): number | undefined {
    return this.responses.get(itemCode);
  }

  next(): void {
    // Check if we're at the works/studies branching question
    if (this.showWorksOrStudiesQuestion) {
      this.questionnaireService.setWorksOrStudies(this.worksOrStudies);
      this.showWorksOrStudiesQuestion = false;
    }
    
    if (!this.questionnaireService.nextItem()) {
      this.onComplete();
    }
  }

  previous(): void {
    this.questionnaireService.previousItem();
    // Re-show works/studies question if going back
    if (this.currentItemIndex === 24) {
      this.showWorksOrStudiesQuestion = true;
    }
  }

  goToItem(index: number): void {
    this.questionnaireService.goToItem(index);
  }

  goToDomain(domain: QuestionnaireDomain): void {
    const firstItem = domain.items.find(item => !item.conditional || this.worksOrStudies);
    if (firstItem) {
      this.goToItem(firstItem.order - 1);
    }
  }

  isCurrentDomain(domain: QuestionnaireDomain): boolean {
    if (!this.currentItem) return false;
    return this.currentItem.domain === domain.code;
  }

  isDomainComplete(domain: QuestionnaireDomain): boolean {
    const applicableItems = domain.items.filter(item => !item.conditional || this.worksOrStudies);
    return applicableItems.every(item => this.responses.has(item.code));
  }

  onWorksOrStudiesChange(value: boolean): void {
    this.worksOrStudies = value;
  }

  onComplete(): void {
    if (!this.questionnaireService.isComplete()) {
      this.errorModalService.showError('Completare tutte le domande prima di procedere');
      return;
    }

    this.isSubmitting = true;
    
    try {
      this.assessmentResult = this.questionnaireService.calculateScores(this.worksOrStudies);
      this.domainScores = this.assessmentResult.domainScores;
      
      // Save to backend
      const assessment: Partial<Assessment> = {
        patientId: this.patientId!.toString(),
        version: 'WHODAS-2.0-36',
        status: 'completed',
        mode: 'interviewer',
        responses: this.assessmentResult.responses,
        compileTypeId: this.compileTypeId ?? undefined
      };

      this.assessmentService.create(assessment as Assessment).subscribe({
        next: (saved) => {
          this.isSubmitting = false;
          this.showResults = true;
          this.errorModalService.showError('Valutazione salvata con successo', 'SUCCESS', 200);
        },
        error: (err) => {
          this.isSubmitting = false;
          this.showResults = true; // Show results anyway
          this.errorModalService.showError('Valutazione completata ma errore nel salvataggio: ' + err.message);
        }
      });
    } catch (e) {
      this.isSubmitting = false;
      this.errorModalService.showError('Errore nel calcolo punteggi: ' + (e as Error).message);
    }
  }

  onSaveDraft(): void {
    const draft: Partial<Assessment> = {
      patientId: this.patientId!.toString(),
      version: 'WHODAS-2.0-36',
      status: 'draft',
      mode: 'interviewer',
      responses: Array.from(this.responses.entries())
        .filter(([k]) => k !== 'worksOrStudies')
        .map(([itemCode, score]) => ({ itemCode, score })),
      compileTypeId: this.compileTypeId ?? undefined
    };

    this.assessmentService.create(draft as Assessment).subscribe({
      next: () => this.errorModalService.showError('Bozza salvata', 'SUCCESS', 200),
      error: (err) => this.errorModalService.showError('Errore salvataggio bozza: ' + err.message)
    });
  }

  onNewAssessment(): void {
    this.questionnaireService.reset();
    this.responses.clear();
    this.worksOrStudies = false;
    this.showWorksOrStudiesQuestion = true;
    this.isComplete = false;
    this.showResults = false;
    this.assessmentResult = null;
    this.domainScores = [];
    this.questionnaireService.initializeQuestionnaire(this.patientId!);
  }

  onBackToPatients(): void {
    this.router.navigate(['/questionnaire']);
  }

  getAnsweredCount(): number {
    return this.responses.size;
  }

  getDomainProgress(domain: QuestionnaireDomain): { answered: number; total: number } {
    const applicableItems = domain.items.filter(item => !item.conditional || this.worksOrStudies);
    const answered = applicableItems.filter(item => this.responses.has(item.code)).length;
    return { answered, total: applicableItems.length };
  }

  getAllItems(): QuestionnaireItem[] {
    return this.questionnaireService.getAllItems();
  }

  isItemAnswered(itemCode: string): boolean {
    return this.responses.has(itemCode);
  }

  isItemCurrent(itemCode: string): boolean {
    return this.currentItem?.code === itemCode;
  }

  getScaleLabels() {
    return this.scaleLabels;
  }

  getDomainItemRange(domain: QuestionnaireDomain): string {
    const applicableItems = domain.items.filter(item => !item.conditional || this.worksOrStudies);
    if (applicableItems.length === 0) return '';
    const first = applicableItems[0].order;
    const last = applicableItems[applicableItems.length - 1].order;
    return `${first}-${last}`;
  }

  getDisabilityLevel(percent: number): string {
    if (percent < 5) return 'Nessuna disabilità';
    if (percent < 25) return 'Disabilità lieve';
    if (percent < 50) return 'Disabilità moderata';
    if (percent < 75) return 'Disabilità grave';
    return 'Disabilità estrema';
  }
}