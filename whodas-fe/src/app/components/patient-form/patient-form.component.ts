import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Patient, PatientService } from '../../services/patient.service';
import { ErrorModalService } from '../../services/error-modal.service';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.css']
})
export class PatientFormComponent implements OnInit {
  patientForm: FormGroup;
  isEdit = false;
  saving = false;
  patientId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private patientService: PatientService,
    private router: Router,
    private route: ActivatedRoute,
    private errorModalService: ErrorModalService
  ) {
    this.patientForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      fiscalCode: [''],
      birthDate: [''],
      birthPlace: [''],
      gender: [''],
      address: [''],
      phone: [''],
      email: ['', Validators.email],
      healthCondition: [''],
      notes: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.patientId = +id;
      this.loadPatient(this.patientId);
    }
  }

  loadPatient(id: number): void {
    this.patientService.findById(id).subscribe({
      next: patient => this.patientForm.patchValue(patient),
      error: () => this.errorModalService.showError('Impossibile caricare i dati del paziente')
    });
  }

  onSubmit(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    const patient: Patient = this.patientForm.getRawValue();

    if (this.isEdit && this.patientId) {
      this.patientService.update(this.patientId, patient).subscribe({
        next: () => this.router.navigate(['/patients']),
        error: () => {
          this.saving = false;
          this.errorModalService.showError('Impossibile salvare le modifiche');
        }
      });
    } else {
      this.patientService.create(patient).subscribe({
        next: () => this.router.navigate(['/patients']),
        error: () => {
          this.saving = false;
          this.errorModalService.showError('Impossibile creare il paziente');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/patients']);
  }
}
