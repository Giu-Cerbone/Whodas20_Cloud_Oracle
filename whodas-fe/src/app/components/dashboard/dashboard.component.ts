import { CommonModule } from '@angular/common';
import { CeilValuePipe } from '../../pipes/ceil-value.pipe';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';

import { AssessmentService, AssessmentResult } from '../../services/assessment.service';
import { CertificateService, Certificate } from '../../services/certificate.service';
import { PatientService, Patient } from '../../services/patient.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CeilValuePipe, CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  totalPatients = 0;
  totalAssessments = 0;
  totalCertificates = 0;
  recentPatients: Patient[] = [];
  recentAssessments: AssessmentResult[] = [];
  recentCertificates: Certificate[] = [];

  constructor(
    private patientService: PatientService,
    private assessmentService: AssessmentService,
    private certificateService: CertificateService
  ) {}

  ngOnInit(): void {
    this.patientService.getAll().subscribe(patients => {
      this.totalPatients = patients.length;
      this.recentPatients = patients.slice(-5);
    });
    this.assessmentService.getAll().subscribe(assessments => {
      this.totalAssessments = assessments.length;
    });
    this.certificateService.getAll().subscribe(certs => {
      this.totalCertificates = certs.length;
      this.recentCertificates = certs.slice(-5);
    });
  }
}
