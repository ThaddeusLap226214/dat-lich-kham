import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { MyPatientsPageComponent } from './my-patients-page.component';
import { PatientService } from '../services/patient.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Gender, IdType, Patient, PatientStatus } from '../models/patient.model';

describe('MyPatientsPageComponent', () => {
  let component: MyPatientsPageComponent;
  let fixture: ComponentFixture<MyPatientsPageComponent>;
  let mockPatientService: jasmine.SpyObj<PatientService>;
  let mockToastService: jasmine.SpyObj<ToastService>;
  let mockRouter: jasmine.SpyObj<Router>;

  const samplePatient: Patient = {
    id: 1,
    ownerAccountId: 5,
    fullName: 'Lê Thị Hoa',
    dateOfBirth: '1990-05-12',
    gender: Gender.FEMALE,
    phone: '0905123456',
    email: 'hoa@gmail.com',
    address: 'Đà Nẵng',
    idType: IdType.CCCD,
    idNumber: '048190001234',
    guardianName: null,
    guardianPhone: null,
    guardianRelation: null,
    status: PatientStatus.ACTIVE,
    mergedIntoId: null,
    createdAt: '2026-10-25T08:00:00'
  };

  beforeEach(async () => {
    mockPatientService = jasmine.createSpyObj('PatientService', ['mine']);
    mockToastService = jasmine.createSpyObj('ToastService', ['success', 'error', 'info', 'warning']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    mockPatientService.mine.and.returnValue(of([samplePatient]));

    await TestBed.configureTestingModule({
      imports: [MyPatientsPageComponent],
      providers: [
        { provide: PatientService, useValue: mockPatientService },
        { provide: ToastService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyPatientsPageComponent);
    component = fixture.componentInstance;
  });

  it('khởi tạo thành công và tải danh sách hồ sơ của tôi', () => {
    fixture.detectChanges();

    expect(component.patients().length).toBe(1);
    expect(component.patients()[0].id).toBe(1);
    expect(component.patients()[0].fullName).toBe('Lê Thị Hoa');
    expect(component.isLoading()).toBeFalse();
    expect(mockPatientService.mine).toHaveBeenCalled();
  });

  it('openAddModal mở form tạo mới', () => {
    component.openAddModal();

    expect(component.isFormOpen).toBeTrue();
    expect(component.selectedPatient).toBeNull();
  });

  it('openEditModal mở form sửa với hồ sơ đã chọn', () => {
    component.openEditModal(samplePatient);

    expect(component.isFormOpen).toBeTrue();
    expect(component.selectedPatient).toEqual(samplePatient);
  });

  it('bookAppointment điều hướng sang trang /booking kèm patientId', () => {
    component.bookAppointment(samplePatient);

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/booking'], {
      queryParams: { patientId: 1 }
    });
  });
});
