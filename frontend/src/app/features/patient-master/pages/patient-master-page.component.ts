import { Component } from '@angular/core';
import { PatientMasterListComponent } from '../components/patient-master-list/patient-master-list.component';
import { PatientMaster } from '../models/patient-master.model';

@Component({
  selector: 'app-patient-master-page',
  standalone: true,
  imports: [PatientMasterListComponent],
  templateUrl: './patient-master-page.component.html',
  styleUrls: ['./patient-master-page.component.css']
})
export class PatientMasterPageComponent {

  onViewDetail(master: PatientMaster): void {
    console.log('View detail:', master);
  }

  onEditMaster(master: PatientMaster): void {
    console.log('Edit master:', master);
  }
}
