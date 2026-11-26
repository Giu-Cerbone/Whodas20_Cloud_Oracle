import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { CeilValuePipe } from '../../pipes/ceil-value.pipe';
import { Certificate, CertificateService } from '../../services/certificate.service';
import { ErrorModalService } from '../../services/error-modal.service';
import { AlertModalService } from '../../services/alert-modal.service';
import { PatientService } from '../../services/patient.service';
import { Assessment, AssessmentService } from '../../services/assessment.service';

type SortDirection = 'asc' | 'desc' | '';

@Component({
  selector: 'app-certificate-management',
  standalone: true,
  imports: [CeilValuePipe, CommonModule, FormsModule],
  templateUrl: './certificate-management.component.html',
  styleUrls: ['./certificate-management.component.css']
})
export class CertificateManagementComponent implements OnInit {
  certificates: Certificate[] = [];
  isLoading = false;

  statusOptions = ['ISSUED', 'DRAFT', 'REVOKED', 'EXPIRED'];

  filters = {
    certificateNumber: '',
    patientName: '',
    fiscalCode: '',
    assessmentId: '',
    status: '',
    issueDateFrom: '',
    issueDateTo: ''
  };

  sortState: { column: string; direction: SortDirection } = { column: '', direction: '' };
  private fiscalCodes = new Map<string, string>();

  selected: Certificate | null = null;

  readonly pageSize = 10;
  currentPage = 1;

  formVisible = false;
  editingId: number | null = null;
  form: Certificate = {};
  submitted = false;
  isSaving = false;

  // Ricerca paziente nel modal
  patientSearchFirstName = '';
  patientSearchLastName = '';
  patientSearchFiscalCode = '';
  patientSearchResults: any[] = [];
  patientResultPage = 1;
  readonly patientPageSize = 5;
  get paginatedPatientResults(): any[] {
    const start = (this.patientResultPage - 1) * this.patientPageSize;
    return this.patientSearchResults.slice(start, start + this.patientPageSize);
  }
  get totalPatientResultPages(): number {
    return Math.max(1, Math.ceil(this.patientSearchResults.length / this.patientPageSize));
  }
  patientPrevPage(): void { if (this.patientResultPage > 1) this.patientResultPage--; }
  patientNextPage(): void { if (this.patientResultPage < this.totalPatientResultPages) this.patientResultPage++; }
  isSearchingPatients = false;

  constructor(
    private certificateService: CertificateService,
    private errorModalService: ErrorModalService,
    private alertModalService: AlertModalService,
    private patientService: PatientService,
    private assessmentService: AssessmentService
  ) {}

  ngOnInit(): void {
    this.patientService.getAll().subscribe({
      next: patients => patients.forEach(p => {
        if (p.id != null && p.fiscalCode) {
          this.fiscalCodes.set(String(p.id), p.fiscalCode);
        }
      }),
      error: () => {}
    });
    this.loadAll();
  }

  fiscalCodeOf(c: Certificate): string {
    return this.fiscalCodes.get((c.patientId ?? '').trim()) ?? '';
  }

  get hasActiveFilters(): boolean {
    return Object.values(this.filters).some(v => `${v}`.trim() !== '');
  }

  get filteredCertificates(): Certificate[] {
    const f = this.filters;
    const contains = (value: string | undefined, term: string) =>
      !term.trim() || (value ?? '').toLowerCase().includes(term.trim().toLowerCase());

    let list = this.certificates.filter(c => {
      if (!contains(c.certificateNumber, f.certificateNumber)) return false;
      if (!contains(c.patientName, f.patientName)) return false;
      if (!contains(this.fiscalCodeOf(c), f.fiscalCode)) return false;
      if (!contains(c.assessmentId != null ? String(c.assessmentId) : '', f.assessmentId)) return false;
      if (f.status && c.status !== f.status) return false;
      const issue = (c.issueDate ?? '').substring(0, 10);
      if (f.issueDateFrom && (!issue || issue < f.issueDateFrom)) return false;
      if (f.issueDateTo && (!issue || issue > f.issueDateTo)) return false;
      return true;
    });

    const { column, direction } = this.sortState;
    if (column && direction) {
      const factor = direction === 'asc' ? 1 : -1;
      list = [...list].sort((a, b) => {
        const va = this.sortValue(a, column);
        const vb = this.sortValue(b, column);
        if (va === vb) return 0;
        if (va === '') return 1;
        if (vb === '') return -1;
        if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * factor;
        return String(va).localeCompare(String(vb), 'it', { sensitivity: 'base' }) * factor;
      });
    }
    return list;
  }

  private sortValue(c: Certificate, column: string): string | number {
    switch (column) {
      case 'certificateNumber': return c.certificateNumber ?? '';
      case 'patientName': return c.patientName ?? '';
      case 'fiscalCode': return this.fiscalCodeOf(c);
      case 'assessmentId': return c.assessmentId ?? '';
      case 'issueDate': return c.issueDate ?? '';
      case 'validUntil': return c.validUntil ?? '';
      case 'status': return c.status ?? '';
      case 'overallPercentDisability': return c.overallPercentDisability ?? '';
      default: return '';
    }
  }

  onSort(column: string): void {
    if (this.sortState.column === column) {
      if (this.sortState.direction === 'asc') {
        this.sortState.direction = 'desc';
      } else {
        this.sortState = { column: '', direction: '' };
      }
    } else {
      this.sortState = { column, direction: 'asc' };
    }
    this.currentPage = 1;
  }

  getSortIcon(column: string): string {
    if (this.sortState.column !== column) return 'fa-sort';
    return this.sortState.direction === 'asc' ? 'fa-sort-up' : 'fa-sort-down';
  }

  get totalItems(): number {
    return this.filteredCertificates.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.pageSize));
  }

  get paginatedCertificates(): Certificate[] {
    const page = Math.min(this.currentPage, this.totalPages);
    const start = (page - 1) * this.pageSize;
    return this.filteredCertificates.slice(start, start + this.pageSize);
  }

  get pages(): number[] {
    const total = this.totalPages;
    const start = Math.max(1, Math.min(this.currentPage - 2, total - 4));
    const end = Math.min(total, start + 4);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  get firstItemIndex(): number {
    return this.totalItems === 0 ? 0 : (this.currentPage - 1) * this.pageSize + 1;
  }

  get lastItemIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  onPageChange(page: number): void {
    this.currentPage = Math.min(Math.max(1, page), this.totalPages);
  }

  onFilterChange(): void {
    this.currentPage = 1;
  }

  clearFilters(): void {
    this.filters = {
      certificateNumber: '',
      patientName: '',
      fiscalCode: '',
      assessmentId: '',
      status: '',
      issueDateFrom: '',
      issueDateTo: ''
    };
    this.currentPage = 1;
  }

  loadAll(): void {
    this.load(this.certificateService.getAll(), 'Impossibile caricare i certificati');
  }

  private load(request: Observable<Certificate[]>, errorMessage: string): void {
    this.isLoading = true;
    request.subscribe({
      next: result => {
        this.certificates = result ?? [];
        this.isLoading = false;
        this.currentPage = Math.min(this.currentPage, this.totalPages);
      },
      error: () => {
        this.certificates = [];
        this.isLoading = false;
        this.errorModalService.showError(errorMessage);
      }
    });
  }

  onView(certificate: Certificate): void {
    if (certificate.id == null) {
      this.selected = certificate;
      return;
    }
    this.certificateService.findById(certificate.id).subscribe({
      next: detail => this.selected = detail,
      error: () => this.errorModalService.showError('Impossibile caricare il dettaglio del certificato')
    });
  }

  closeDetail(): void {
    this.selected = null;
  }

  onNew(): void {
    this.selected = null;
    this.editingId = null;
    this.form = { status: 'ISSUED' };
    this.submitted = false;
    this.formVisible = true;
    this.certificateService.getNextCertificateNumber().subscribe({
      next: nextNum => this.form.certificateNumber = nextNum,
      error: () => {}
    });
  }

  onAssessmentBlur(): void {
    const value = this.form.assessmentId;
    if (value) {
      const assessmentIdNum = Number(value);
      if (!isNaN(assessmentIdNum)) {
        this.assessmentService.findById(assessmentIdNum).subscribe({
          next: (assessment: Assessment) => {
            this.form.patientId = assessment.patientId;
            this.form.overallPercentDisability = assessment.overallPercentDisability !== undefined
              ? Math.floor(assessment.overallPercentDisability * 100) / 100
              : undefined;
            this.form.overallComplexScore = assessment.overallComplexScore;
            // Fetch patient name separately
            if (assessment.patientId) {
              const patientIdNum = Number(assessment.patientId);
              if (!isNaN(patientIdNum)) {
                this.patientService.findById(patientIdNum).subscribe({
                  next: (patient) => this.form.patientName = `${patient.firstName || ''} ${patient.lastName || ''}`.trim(),
                  error: () => {}
                });
              }
            }
          },
          error: () => this.alertModalService.showWarning('Valutazione non trovata')
        });
      } else {
        this.alertModalService.showWarning('Valutazione non trovata');
      }
    } else {
      this.form.patientId = '';
      this.form.patientName = '';
      this.form.overallPercentDisability = undefined;
      this.form.overallComplexScore = undefined;
    }
  }

  onPatientIdBlur(): void {
    if (this.form.patientId) {
      const patientIdNum = Number(this.form.patientId);
      if (!isNaN(patientIdNum)) {
        this.patientService.findById(patientIdNum).subscribe({
          next: (patient) => this.form.patientName = `${patient.firstName || ''} ${patient.lastName || ''}`.trim(),
          error: () => this.alertModalService.showWarning('Paziente non trovato')
        });
      } else {
        this.alertModalService.showWarning('Paziente non trovato');
      }
    } else {
      this.form.patientName = '';
    }
  }

  onEdit(certificate: Certificate): void {
    this.selected = null;
    this.editingId = certificate.id ?? null;
    this.form = {
      ...certificate,
      issueDate: (certificate.issueDate ?? '').substring(0, 10),
      validUntil: (certificate.validUntil ?? '').substring(0, 10)
    };
    this.submitted = false;
    this.formVisible = true;
  }

  searchPatients(): void {
    this.isSearchingPatients = true;
    this.patientService.getAll().subscribe({
      next: (patients) => {
        const fn = this.patientSearchFirstName.trim().toLowerCase();
        const ln = this.patientSearchLastName.trim().toLowerCase();
        const fc = this.patientSearchFiscalCode.trim().toLowerCase();
        this.patientResultPage = 1;
        this.patientSearchResults = (patients ?? []).filter(p =>
          (!fn || (p.firstName || '').toLowerCase().includes(fn)) &&
          (!ln || (p.lastName || '').toLowerCase().includes(ln)) &&
          (!fc || (p.fiscalCode || '').toLowerCase().includes(fc))
        );
        this.isSearchingPatients = false;
      },
      error: () => { this.patientSearchResults = []; this.isSearchingPatients = false; }
    });
  }

  selectPatient(patient: any): void {
    this.form.patientId = String(patient.id);
    this.form.patientName = `${patient.firstName || ''} ${patient.lastName || ''}`.trim();
    // carica valutazioni del paziente e prende l'ultima
    this.assessmentService.findByPatientId(String(patient.id)).subscribe({
      next: (assessments) => {
        if (assessments && assessments.length) {
          const last = assessments[assessments.length - 1];
          this.form.assessmentId = last.id;
          this.onAssessmentBlur();
        }
      },
      error: () => {}
    });
  }

  cancelForm(): void {
    this.formVisible = false;
    this.editingId = null;
    this.form = {};
    this.submitted = false;
  }

  private isFormValid(): boolean {
    return !!this.form.assessmentId;
  }

  onlyNumbers(event: KeyboardEvent): void {
    const allowedKeys = ['Backspace', 'Tab', 'ArrowLeft', 'ArrowRight', 'Delete', 'Home', 'End'];
    if (allowedKeys.includes(event.key)) {
      return;
    }
    if (!/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  onSave(): void {
    this.submitted = true;
    if (!this.isFormValid()) {
      return;
    }
    const payload: Certificate = {
      ...this.form,
      issueDate: this.form.issueDate || undefined,
      validUntil: this.form.validUntil || undefined,
      overallPercentDisability: this.form.overallPercentDisability != null
        ? Math.ceil(Number(this.form.overallPercentDisability))
        : undefined
    };
    this.isSaving = true;
    const request = this.editingId != null
      ? this.certificateService.update(this.editingId, payload)
      : this.certificateService.create(payload);
    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.cancelForm();
        this.loadAll();
      },
      error: () => {
        this.isSaving = false;
        this.errorModalService.showError('Impossibile salvare il certificato');
      }
    });
  }

  onDelete(certificate: Certificate): void {
    if (certificate.id == null) {
      return;
    }
    if (!confirm(`Eliminare il certificato ${certificate.certificateNumber ?? certificate.id}?`)) {
      return;
    }
    this.certificateService.delete(certificate.id).subscribe({
      next: () => this.loadAll(),
      error: () => this.errorModalService.showError('Impossibile eliminare il certificato')
    });
  }

  statusClass(status?: string): string {
    switch (status) {
      case 'ISSUED': return 'bg-success';
      case 'DRAFT': return 'bg-secondary';
      case 'REVOKED': return 'bg-danger';
      case 'EXPIRED': return 'bg-warning text-dark';
      default: return 'bg-light text-dark';
    }
  }

  trackById(_index: number, c: Certificate): number | string | undefined {
    return c.id ?? c.certificateNumber;
  }
}
