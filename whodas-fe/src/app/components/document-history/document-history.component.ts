import { CommonModule } from '@angular/common';
import { CeilValuePipe } from '../../pipes/ceil-value.pipe';
import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import {
  DocumentImportResult,
  DocumentImportStatus,
  DocumentImportSummary,
  DocumentService,
  EXTRACTION_METHOD_LABELS,
  ExtractedDocumentItem,
  ExtractionMethod,
  IMPORT_STATUS_LABELS
} from '../../services/document.service';
import { ErrorModalService } from '../../services/error-modal.service';

@Component({
  selector: 'app-document-history',
  standalone: true,
  imports: [CeilValuePipe, CommonModule, RouterModule],
  templateUrl: './document-history.component.html',
  styleUrls: ['./document-history.component.css']
})
export class DocumentHistoryComponent implements OnInit {
  importList: DocumentImportSummary[] = [];
  selectedImportId: number | null = null;
  detail: DocumentImportResult | null = null;
  currentPage = 1;
  pageSize = 20;
  pageSizeOptions = [10, 20, 50];
  totalItems = 0;
  totalPages = 1;
  isLoading = false;
  isLoadingDetail = false;
  isDownloadingFile = false;
  showWarnings = false;

  constructor(
    private documentService: DocumentService,
    private errorModalService: ErrorModalService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadImports();
  }

  loadImports(): void {
    this.isLoading = true;
    this.documentService.getImports(this.pageSize, (this.currentPage - 1) * this.pageSize).subscribe({
      next: imports => {
        this.importList = imports;
        this.totalItems = imports.length;
        this.totalPages = this.totalItems < this.pageSize ? 1 : Math.max(1, Math.ceil(this.totalItems / this.pageSize));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorModalService.showError('Impossibile caricare lo storico delle importazioni');
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 1 && (this.totalPages === 1 || page <= this.totalPages)) {
      this.currentPage = page;
      this.loadImports();
    }
  }

  onPageSizeChange(pageSize: string): void {
    this.pageSize = Number(pageSize);
    this.currentPage = 1;
    this.loadImports();
  }

  onOpen(imported: DocumentImportSummary): void {
    if (!imported.id) return;
    if (this.selectedImportId === imported.id) {
      this.closeDetail();
      return;
    }

    this.selectedImportId = imported.id;
    this.detail = null;
    this.showWarnings = false;
    this.isLoadingDetail = true;
    this.documentService.getImport(imported.id).subscribe({
      next: detail => {
        this.detail = detail;
        this.isLoadingDetail = false;
      },
      error: () => {
        this.isLoadingDetail = false;
        this.closeDetail();
        this.errorModalService.showError('Impossibile caricare il dettaglio dell\'importazione');
      }
    });
  }

  closeDetail(): void {
    this.selectedImportId = null;
    this.detail = null;
  }

  onDownloadOriginal(imported: DocumentImportSummary): void {
    if (!imported.id) return;
    this.downloadFile(imported.id, imported.fileName);
  }

  onDownloadDetailFile(): void {
    if (!this.detail?.importId) return;
    this.downloadFile(this.detail.importId, this.detail.fileName);
  }

  onDelete(imported: DocumentImportSummary): void {
    if (!imported.id) return;

    if (confirm(`Eliminare l'importazione di ${imported.fileName || 'questo documento'}?`)) {
      this.documentService.deleteImport(imported.id).subscribe({
        next: () => {
          this.closeDetail();
          this.loadImports();
        },
        error: () => this.errorModalService.showError('Impossibile eliminare l\'importazione')
      });
    }
  }

  onGoToAssessment(): void {
    const patientId = this.detail?.patient?.id;
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

  trackByImportId(index: number, imported: DocumentImportSummary): number {
    return imported.id || index;
  }

  trackByItemCode(index: number, item: ExtractedDocumentItem): string {
    return item.itemCode || `item-${index}`;
  }

  private downloadFile(importId: number, fileName: string | undefined): void {
    this.isDownloadingFile = true;
    this.documentService.getImportFile(importId).subscribe({
      next: blob => {
        this.documentService.triggerDownload(blob, fileName || `importazione_${importId}`);
        this.isDownloadingFile = false;
      },
      error: () => {
        this.isDownloadingFile = false;
        this.errorModalService.showError('Download del documento originale non riuscito');
      }
    });
  }
}
