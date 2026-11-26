import { CommonModule } from '@angular/common';
import { CeilValuePipe } from '../../pipes/ceil-value.pipe';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { ErrorModalService } from '../../services/error-modal.service';
import { Patient, PatientService } from '../../services/patient.service';
import { AssessmentService, Assessment, TypeCompileQuest } from '../../services/assessment.service';
import { AssessmentPdfService } from '../../services/assessment-pdf.service';

type SortDirection = 'asc' | 'desc' | '';

interface SortState {
  column: string;
  direction: SortDirection;
}

@Component({
  selector: 'app-questionnaire-assessment',
  standalone: true,
  imports: [CeilValuePipe, CommonModule, FormsModule, RouterModule],
  templateUrl: './questionnaire-assessment.component.html',
  styleUrls: ['./questionnaire-assessment.component.css']
})
export class QuestionnaireAssessmentComponent implements OnInit {
  patients: Patient[] = [];
  selectedPatient: Patient | null = null;
  showPatientSelector = true;
  isLoading = false;
  
// Assessments for selected patient
  patientAssessments: Assessment[] = [];
  isLoadingAssessments = false;

  // Search
  searchLastName = '';
  searchFirstName = '';

  // Sorting
  sortState: SortState = { column: '', direction: '' };
  sortableColumns = ['firstName', 'lastName', 'fiscalCode', 'birthDate'];

  // Pagination
  currentPage = 1;
  pageSize = 10;
  pageSizeOptions = [5, 10, 25, 50];
  totalPages = 1;

  // Tipo compilazione
  typeCompileQuests: TypeCompileQuest[] = [];
  showTypeSelector = false;
  selectedType: TypeCompileQuest | null = null;

  constructor(
    private patientService: PatientService,
    private assessmentService: AssessmentService,
    private pdfService: AssessmentPdfService,
    private router: Router,
    private route: ActivatedRoute,
    private errorModalService: ErrorModalService
  ) {}

  ngOnInit(): void {
    this.loadPatients();
    this.loadTypeCompileQuests();

    const patientId = this.route.snapshot.paramMap.get('patientId');
    if (patientId) {
      this.patientService.findById(+patientId).subscribe({
        next: patient => {
          this.selectedPatient = patient;
          this.showPatientSelector = false;
          this.loadPatientAssessments(patient.id!);
        },
        error: () => this.errorModalService.showError('Paziente non trovato')
      });
    }
  }

  loadTypeCompileQuests(): void {
    this.assessmentService.getTypeCompileQuests().subscribe({
      next: types => this.typeCompileQuests = types,
      error: () => this.errorModalService.showError('Impossibile caricare i tipi di compilazione')
    });
  }

  loadPatients(): void {
    this.isLoading = true;
    this.patientService.getAll().subscribe({
      next: patients => {
        this.patients = patients;
        this.updatePagination();
        this.isLoading = false;
      },
      error: () => {
        this.errorModalService.showError('Impossibile caricare la lista pazienti');
        this.isLoading = false;
      }
    });
  }

  loadPatientAssessments(patientId: number): void {
    this.isLoadingAssessments = true;
    this.assessmentService.findByPatientId(patientId.toString()).subscribe({
      next: assessments => {
        this.patientAssessments = assessments.sort((a, b) => {
          const dateA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
          const dateB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
          return dateB - dateA; // Most recent first
        });
        this.isLoadingAssessments = false;
      },
      error: () => {
        this.errorModalService.showError('Impossibile caricare le valutazioni del paziente');
        this.isLoadingAssessments = false;
      }
    });
  }

  get filteredPatients(): Patient[] {
    let result = this.patients.filter(patient => {
      const matchesLastName = !this.searchLastName ||
        patient.lastName?.toLowerCase().includes(this.searchLastName.toLowerCase());
      const matchesFirstName = !this.searchFirstName ||
        patient.firstName?.toLowerCase().includes(this.searchFirstName.toLowerCase());
      return matchesLastName && matchesFirstName;
    });

    // Apply sorting
    if (this.sortState.column && this.sortState.direction) {
      result = [...result].sort((a, b) => {
        const aVal = this.getSortValue(a, this.sortState.column);
        const bVal = this.getSortValue(b, this.sortState.column);
        
        if (aVal === null && bVal === null) return 0;
        if (aVal === null) return this.sortState.direction === 'asc' ? 1 : -1;
        if (bVal === null) return this.sortState.direction === 'asc' ? -1 : 1;
        
        const comparison = aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
        return this.sortState.direction === 'asc' ? comparison : -comparison;
      });
    }

    return result;
  }

  get paginatedPatients(): Patient[] {
    const filtered = this.filteredPatients;
    this.totalPages = Math.ceil(filtered.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    return filtered.slice(start, start + this.pageSize);
  }

  get totalItems(): number {
    return this.filteredPatients.length;
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredPatients.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
  }

  private getSortValue(patient: Patient, column: string): any {
    switch (column) {
      case 'firstName': return patient.firstName || '';
      case 'lastName': return patient.lastName || '';
      case 'fiscalCode': return patient.fiscalCode || '';
      case 'birthDate': return patient.birthDate ? new Date(patient.birthDate).getTime() : 0;
      default: return '';
    }
  }

  onSort(column: string): void {
    if (!this.sortableColumns.includes(column)) return;
    
    if (this.sortState.column === column) {
      if (this.sortState.direction === 'asc') {
        this.sortState.direction = 'desc';
      } else if (this.sortState.direction === 'desc') {
        this.sortState.direction = '';
        this.sortState.column = '';
      } else {
        this.sortState.direction = 'asc';
      }
    } else {
      this.sortState.column = column;
      this.sortState.direction = 'asc';
    }
    this.currentPage = 1;
  }

  getSortIcon(column: string): string {
    if (this.sortState.column !== column) return 'fa-sort';
    if (this.sortState.direction === 'asc') return 'fa-sort-up';
    if (this.sortState.direction === 'desc') return 'fa-sort-down';
    return 'fa-sort';
  }

  getSortClass(column: string): string {
    if (this.sortState.column === column && this.sortState.direction) {
      return 'sorted';
    }
    return '';
  }

  onPageChange(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  onPageSizeChange(): void {
    this.currentPage = 1;
    this.updatePagination();
  }

  onSearch(): void {
    this.currentPage = 1;
  }

  clearFilters(): void {
    this.searchLastName = '';
    this.searchFirstName = '';
    this.currentPage = 1;
  }

  get pages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end = start + maxVisible - 1;
    
    if (end > this.totalPages) {
      end = this.totalPages;
      start = Math.max(1, end - maxVisible + 1);
    }
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  Math = Math;

  onPatientSelect(patient: Patient): void {
    this.selectedPatient = patient;
    this.showPatientSelector = false;
    this.loadPatientAssessments(patient.id!);
  }

  onBackToSelection(): void {
    this.showPatientSelector = true;
    this.selectedPatient = null;
    this.patientAssessments = [];
  }

  onStartAssessment(): void {
    if (this.selectedPatient?.id) {
      this.showTypeSelector = true;
      this.selectedType = null;
    }
  }

  selectType(type: TypeCompileQuest): void {
    this.selectedType = type;
  }

  confirmStartAssessment(): void {
    if (!this.selectedType) {
      this.errorModalService.showError('Seleziona un tipo di compilazione');
      return;
    }
    if (this.selectedPatient?.id) {
      this.router.navigate(['/assessment/run', this.selectedPatient.id], {
        state: { compileTypeId: this.selectedType.id }
      });
    }
  }

  cancelTypeSelector(): void {
    this.showTypeSelector = false;
    this.selectedType = null;
  }

  onDownloadPdf(assessment: Assessment): void {
    if (!assessment.id) return;
    this.pdfService.downloadPdfBlob(assessment.id).subscribe({
      next: (blob) => {
        const filename = this.generateFilename(assessment);
        this.pdfService.triggerDownload(blob, filename);
      },
      error: (err) => {
        const msg = err?.message || err?.error?.message || 'Errore sconosciuto';
        this.errorModalService.showError('Errore durante il download del PDF: ' + msg);
      }
    });
  }

  onViewPdf(assessment: Assessment): void {
    if (!assessment.id) return;
    this.pdfService.viewPdfBlob(assessment.id).subscribe({
      next: (blob) => {
        this.pdfService.triggerPrint(blob);
      },
      error: (err) => {
        const msg = err?.message || err?.error?.message || 'Errore sconosciuto';
        this.errorModalService.showError('Errore durante l\'apertura del PDF: ' + msg);
      }
    });
  }

  private generateFilename(assessment: Assessment): string {
    const fiscalCode = this.selectedPatient?.fiscalCode || 'NOCF';
    const dateStr = assessment.updatedAt 
      ? new Date(assessment.updatedAt).toISOString().slice(0, 19).replace(/[-:]/g, '').replace('T', '_')
      : new Date().toISOString().slice(0, 19).replace(/[-:]/g, '').replace('T', '_');
    const status = assessment.status?.toLowerCase() || 'unknown';
    return `${fiscalCode}_questionario_${dateStr}_${status}.pdf`;
  }

  getStatusBadgeClass(status: string | undefined): string {
    switch (status?.toLowerCase()) {
      case 'completed': return 'bg-success';
      case 'draft': return 'bg-warning text-dark';
      case 'submitted': return 'bg-info';
      default: return 'bg-secondary';
    }
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

  trackByPatientId(index: number, patient: Patient): number {
    return patient.id || index;
  }

  trackByAssessmentId(index: number, assessment: Assessment): number {
    return assessment.id || index;
  }
}
