import { CommonModule } from '@angular/common';
import { CeilValuePipe } from '../../pipes/ceil-value.pipe';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { AssessmentService, TypeCompileQuest } from '../../services/assessment.service';
import {
  DocumentImportOptions,
  DocumentImportResult,
  DocumentService,
  EXTRACTION_METHOD_LABELS,
  ExtractedDocumentItem,
  ExtractionMethod,
  IMPORT_STATUS_LABELS,
  DocumentImportStatus
} from '../../services/document.service';
import { ErrorModalService } from '../../services/error-modal.service';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CeilValuePipe, CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.css']
})
export class DocumentUploadComponent implements OnInit {
  readonly allowedExtensions = ['pdf', 'doc', 'docx', 'xls', 'xlsx'];
  readonly maxFileSize = 20 * 1024 * 1024;

  typeCompileQuests: TypeCompileQuest[] = [];
  importForm: FormGroup;
  selectedFile: File | null = null;
  result: DocumentImportResult | null = null;
  isDragOver = false;
  isSubmitting = false;
  isDownloadingFile = false;
  showWarnings = false;

  constructor(
    private fb: FormBuilder,
    private documentService: DocumentService,
    private assessmentService: AssessmentService,
    private errorModalService: ErrorModalService,
    private router: Router
  ) {
    this.importForm = this.fb.group({
      patientId: [''],
      compileTypeId: [null],
      interviewer: [''],
      createPatientIfMissing: [true],
      saveAssessment: [true],
      submitAndScore: [true]
    });
  }

  ngOnInit(): void {
    this.assessmentService.getTypeCompileQuests().subscribe({
      next: types => this.typeCompileQuests = types,
      error: () => this.errorModalService.showError('Impossibile caricare i tipi di compilazione')
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectFile(input.files[0]);
      input.value = '';
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.selectFile(file);
    }
  }

  onRemoveFile(): void {
    this.selectedFile = null;
  }

  onSubmit(): void {
    if (!this.selectedFile) {
      this.errorModalService.showError('Seleziona un documento da importare');
      return;
    }

    const raw = this.importForm.getRawValue();
    const options: DocumentImportOptions = {
      createPatientIfMissing: !!raw.createPatientIfMissing,
      saveAssessment: !!raw.saveAssessment,
      submitAndScore: !!raw.submitAndScore
    };

    if (raw.patientId !== '' && raw.patientId !== null) {
      const patientId = Number(raw.patientId);
      if (isNaN(patientId) || patientId <= 0) {
        this.errorModalService.showError('ID paziente non valido');
        return;
      }
      options.patientId = patientId;
    }
    if (raw.compileTypeId) {
      options.compileTypeId = Number(raw.compileTypeId);
    }
    if (raw.interviewer && raw.interviewer.trim()) {
      options.interviewer = raw.interviewer.trim();
    }

    this.isSubmitting = true;
    this.documentService.importDocument(this.selectedFile, options).subscribe({
      next: result => {
        this.result = result;
        this.isSubmitting = false;
        this.showWarnings = false;
      },
      error: () => {
        this.isSubmitting = false;
        this.errorModalService.showError('Importazione del documento non riuscita');
      }
    });
  }

  onDownloadOriginal(): void {
    if (!this.result?.importId) return;
    this.isDownloadingFile = true;
    this.documentService.getImportFile(this.result.importId).subscribe({
      next: blob => {
        this.documentService.triggerDownload(blob, this.result?.fileName || `importazione_${this.result?.importId}`);
        this.isDownloadingFile = false;
      },
      error: () => {
        this.isDownloadingFile = false;
        this.errorModalService.showError('Download del documento originale non riuscito');
      }
    });
  }

  onGoToAssessment(): void {
    const patientId = this.result?.patient?.id;
    if (patientId) {
      this.router.navigate(['/questionnaire', patientId]);
    }
  }

  isItemWarning(item: ExtractedDocumentItem): boolean {
    return item.score === null || item.score === undefined || (item.confidence ?? 0) < 0.6;
  }

  getMethodLabel(method: ExtractionMethod | undefined): string {
    if (!method) return '-';
    return EXTRACTION_METHOD_LABELS[method] || method;
  }

  getStatusLabel(status: DocumentImportStatus | undefined): string {
    if (!status) return '-';
    return IMPORT_STATUS_LABELS[status] || status;
  }

  getStatusBadgeClass(status: DocumentImportStatus | string | undefined): string {
    switch (status) {
      case 'IMPORTED':
      case 'COMPLETED':
        return 'bg-success';
      case 'ANALYZED_ONLY':
      case 'SUBMITTED':
        return 'bg-info';
      case 'FAILED':
        return 'bg-danger';
      case 'DRAFT':
        return 'bg-warning text-dark';
      default:
        return 'bg-secondary';
    }
  }

  formatConfidence(confidence: number | undefined): string {
    if (confidence === null || confidence === undefined) return '-';
    return `${Math.round(confidence * 100)}%`;
  }

  formatSize(bytes: number | undefined): string {
    if (!bytes && bytes !== 0) return '-';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '-';
    try {
      return new Date(dateStr).toLocaleDateString('it-IT', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  }

  trackByItemCode(index: number, item: ExtractedDocumentItem): string {
    return item.itemCode || `item-${index}`;
  }

  private selectFile(file: File): void {
    const name = file.name.toLowerCase();
    const extension = name.includes('.') ? name.substring(name.lastIndexOf('.') + 1) : '';
    if (!this.allowedExtensions.includes(extension)) {
      this.errorModalService.showError('Formato non supportato. Carica un file PDF, DOC, DOCX, XLS o XLSX');
      return;
    }
    if (file.size > this.maxFileSize) {
      this.errorModalService.showError('Il file supera la dimensione massima consentita di 20 MB');
      return;
    }
    this.selectedFile = file;
    this.result = null;
  }
}
