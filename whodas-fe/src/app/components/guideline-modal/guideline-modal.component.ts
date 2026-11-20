import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GuidelineService, GuidelineSection } from '../../services/guideline.service';

@Component({
  selector: 'app-guideline-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './guideline-modal.component.html',
  styleUrls: ['./guideline-modal.component.css']
})
export class GuidelineModalComponent implements OnInit {
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();
  sections: GuidelineSection[] = [];
  activeSection: GuidelineSection | null = null;
  searchTerm = '';

  constructor(private guidelineService: GuidelineService) {}

  ngOnInit(): void {
    this.sections = this.guidelineService.getAllSections();
    if (this.sections.length > 0) {
      this.activeSection = this.sections[0];
    }
  }

  close(): void {
    this.isOpen = false;
    this.searchTerm = '';
    this.closed.emit();
  }

  selectSection(section: GuidelineSection): void {
    this.activeSection = section;
  }

  get filteredSections(): GuidelineSection[] {
    if (!this.searchTerm.trim()) return this.sections;
    const term = this.searchTerm.toLowerCase();
    return this.sections.filter(s => 
      s.title.toLowerCase().includes(term) || 
      s.content.toLowerCase().includes(term) ||
      s.keywords.some(k => k.toLowerCase().includes(term))
    );
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close();
    }
  }
}