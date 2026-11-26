import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { map } from 'rxjs/operators';

import { AssessmentService, TypeCompileQuest } from '../../services/assessment.service';
import { DocumentService, DocumentTemplate, DocumentTemplateParams } from '../../services/document.service';
import { ErrorModalService } from '../../services/error-modal.service';
import { Patient, PatientService } from '../../services/patient.service';

@Component({
  selector: 'app-document-download',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './document-download.component.html',
  styleUrls: ['./document-download.component.css']
})
export class DocumentDownloadComponent implements OnInit {
  templates: DocumentTemplate[] = [];
  typeCompileQuests: TypeCompileQuest[] = [];
  patients: Patient[] = [];
  selectedPatient: Patient | null = null;
  downloadForm: FormGroup;
  isLoading = false;
  isSearching = false;
  isDownloading = false;
  isPreviewing = false;

  constructor(
    private fb: FormBuilder,
    private documentService: DocumentService,
    private patientService: PatientService,
    private assessmentService: AssessmentService,
    private errorModalService: ErrorModalService
  ) {
    this.downloadForm = this.fb.group({
      templateCode: ['', Validators.required],
      compileTypeId: [null],
      interviewer: [''],
      assessmentDate: [''],
      searchTerm: ['']
    });
  }

  ngOnInit(): void {
    this.loadTemplates();
    this.loadTypeCompileQuests();
  }

  loadTemplates(): void {
    this.isLoading = true;
    this.documentService.getTemplates().subscribe({
      next: templates => {
        this.templates = templates;
        if (templates.length === 1 && templates[0].code) {
          this.downloadForm.patchValue({ templateCode: templates[0].code });
        }
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorModalService.showError('Impossibile caricare i modelli di documento');
      }
    });
  }

  loadTypeCompileQuests(): void {
    this.assessmentService.getTypeCompileQuests().subscribe({
      next: types => this.typeCompileQuests = types,
      error: () => this.errorModalService.showError('Impossibile caricare i tipi di compilazione')
    });
  }

  get selectedTemplate(): DocumentTemplate | null {
    const code = this.downloadForm.get('templateCode')?.value;
    return this.templates.find(template => template.code === code) || null;
  }

  get selectedCompileType(): TypeCompileQuest | null {
    const id = Number(this.downloadForm.get('compileTypeId')?.value);
    if (!id) return null;
    return this.typeCompileQuests.find(type => type.id === id) || null;
  }

  get filename(): string {
    return this.documentService.buildTemplateFilename(
      this.selectedTemplate || { code: 'whodas2', label: '' },
      this.selectedPatient || undefined,
      this.assessmentDate || undefined
    );
  }

  get assessmentDate(): string {
    return this.downloadForm.get('assessmentDate')?.value || '';
  }

  onSearchPatients(): void {
    const term = (this.downloadForm.get('searchTerm')?.value || '').toString().trim();
    if (!term) {
      this.patients = [];
      return;
    }

    this.isSearching = true;
    const request = this.isFiscalCode(term)
      ? this.patientService.findByFiscalCode(term.toUpperCase()).pipe(map(patient => [patient]))
      : this.patientService.searchByLastName(term);

    request.subscribe({
      next: patients => {
        this.patients = patients;
        this.isSearching = false;
      },
      error: () => {
        this.isSearching = false;
        this.errorModalService.showError('Impossibile cercare i pazienti');
      }
    });
  }

  onPatientSelect(patient: Patient): void {
    this.selectedPatient = patient;
    this.patients = [];
    this.downloadForm.patchValue({ searchTerm: '' });
  }

  onClearPatient(): void {
    this.selectedPatient = null;
  }

  onDownload(): void {
    if (!this.selectedTemplate) {
      this.downloadForm.markAllAsTouched();
      this.errorModalService.showError('Seleziona un modello WHODAS 2.0');
      return;
    }

    this.isDownloading = true;
    this.documentService.downloadTemplate(this.buildParams()).subscribe({
      next: blob => {
        this.documentService.triggerDownload(blob, this.filename);
        this.isDownloading = false;
      },
      error: () => {
        this.isDownloading = false;
        this.errorModalService.showError('Generazione del PDF non riuscita');
      }
    });
  }

  onPreview(): void {
    if (!this.selectedTemplate) {
      this.downloadForm.markAllAsTouched();
      this.errorModalService.showError('Seleziona un modello WHODAS 2.0');
      return;
    }

    this.isPreviewing = true;
    this.documentService.viewTemplate(this.buildParams()).subscribe({
      next: blob => {
        this.documentService.triggerPrint(blob);
        this.isPreviewing = false;
      },
      error: () => {
        this.isPreviewing = false;
        this.errorModalService.showError('Anteprima del PDF non riuscita');
      }
    });
  }

  private buildParams(): DocumentTemplateParams {
    const raw = this.downloadForm.getRawValue();
    const params: DocumentTemplateParams = {};
    if (this.selectedPatient?.id) {
      params.patientId = this.selectedPatient.id;
    }
    if (raw.compileTypeId) {
      params.compileTypeId = Number(raw.compileTypeId);
    }
    if (raw.interviewer && raw.interviewer.trim()) {
      params.interviewer = raw.interviewer.trim();
    }
    if (raw.assessmentDate) {
      params.assessmentDate = raw.assessmentDate;
    }
    return params;
  }

  private isFiscalCode(term: string): boolean {
    return /^[A-Za-z0-9]{16}$/.test(term);
  }
}
