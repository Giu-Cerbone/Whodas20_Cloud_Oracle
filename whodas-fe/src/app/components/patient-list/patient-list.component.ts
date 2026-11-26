import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ErrorModalService } from '../../services/error-modal.service';
import { Patient, PatientService } from '../../services/patient.service';

type SortDirection = 'asc' | 'desc' | '';

interface SortState {
  column: string;
  direction: SortDirection;
}

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.css']
})
export class PatientListComponent implements OnInit {
  patients: Patient[] = [];
  searchLastName = '';
  searchFirstName = '';
  searchFiscalCode = '';
  
  // Sorting
  sortState: SortState = { column: '', direction: '' };
  sortableColumns = ['id', 'firstName', 'lastName', 'fiscalCode', 'birthDate', 'gender'];
  
  // Pagination
  currentPage = 1;
  pageSize = 10;
  pageSizeOptions = [5, 10, 25, 50];
  totalPages = 1;
  
  // Loading state
  isLoading = false;

  constructor(
    private patientService: PatientService,
    private router: Router,
    private errorModalService: ErrorModalService
  ) {}

  ngOnInit(): void {
    this.loadPatients();
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

  get filteredPatients(): Patient[] {
    let result = this.patients.filter(patient => {
      const matchesLastName = !this.searchLastName ||
        patient.lastName?.toLowerCase().includes(this.searchLastName.toLowerCase());
      const matchesFirstName = !this.searchFirstName ||
        patient.firstName?.toLowerCase().includes(this.searchFirstName.toLowerCase());
      const matchesFiscalCode = !this.searchFiscalCode ||
        patient.fiscalCode?.toLowerCase().includes(this.searchFiscalCode.toLowerCase());
      return matchesLastName && matchesFirstName && matchesFiscalCode;
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

  Math = Math;

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredPatients.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
  }

  private getSortValue(patient: Patient, column: string): any {
    switch (column) {
      case 'id': return patient.id || 0;
      case 'firstName': return patient.firstName || '';
      case 'lastName': return patient.lastName || '';
      case 'fiscalCode': return patient.fiscalCode || '';
      case 'birthDate': return patient.birthDate ? new Date(patient.birthDate).getTime() : 0;
      case 'gender': return patient.gender || '';
      default: return '';
    }
  }

  onSort(column: string): void {
    if (!this.sortableColumns.includes(column)) return;
    
    if (this.sortState.column === column) {
      // Cycle through: asc -> desc -> none
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

  onNewPatient(): void {
    this.router.navigate(['/patients/new']);
  }

  onEdit(patient: Patient): void {
    this.router.navigate(['/patients/edit', patient.id]);
  }

  onDelete(patient: Patient): void {
    if (!patient.id) return;

    if (confirm(`Eliminare il paziente ${patient.firstName} ${patient.lastName}?`)) {
      this.patientService.delete(patient.id).subscribe({
        next: () => this.loadPatients(),
        error: () => this.errorModalService.showError('Impossibile eliminare il paziente')
      });
    }
  }

  onSearch(): void {
    this.currentPage = 1;
  }

  clearFilters(): void {
    this.searchLastName = '';
    this.searchFirstName = '';
    this.searchFiscalCode = '';
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

  trackByPatientId(index: number, patient: Patient): number {
    return patient.id || index;
  }
}
