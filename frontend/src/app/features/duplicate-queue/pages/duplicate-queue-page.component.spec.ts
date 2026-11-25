import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { DuplicateQueuePageComponent } from './duplicate-queue-page.component';
import { DuplicateQueueService } from '../services/duplicate-queue.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import {
  CandidateStatus,
  DuplicateCandidate,
  DuplicateReason
} from '../models/duplicate-candidate.model';
import { Gender, IdType, Patient, PatientStatus, SpringPage } from '../../patient/models/patient.model';

describe('DuplicateQueuePageComponent', () => {
  let component: DuplicateQueuePageComponent;
  let fixture: ComponentFixture<DuplicateQueuePageComponent>;
  let mockQueueService: jasmine.SpyObj<DuplicateQueueService>;
  let mockToastService: jasmine.SpyObj<ToastService>;

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

  const sampleCandidate: DuplicateCandidate = {
    id: 10,
    reason: DuplicateReason.SAME_NAME_DOB_PHONE,
    score: 60,
    status: CandidateStatus.PENDING,
    patient: { ...samplePatient, id: 2, status: PatientStatus.TEMPORARY },
    candidatePatient: samplePatient,
    reviewedByAccountId: null,
    reviewedAt: null,
    reviewNote: null,
    createdAt: '2026-10-25T09:00:00'
  };

  const mockPage: SpringPage<DuplicateCandidate> = {
    content: [sampleCandidate],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1
  };

  beforeEach(async () => {
    mockQueueService = jasmine.createSpyObj('DuplicateQueueService', [
      'list',
      'count',
      'merge',
      'reject',
      'unmerge'
    ]);
    mockToastService = jasmine.createSpyObj('ToastService', ['success', 'error', 'info', 'warning']);

    mockQueueService.count.and.returnValue(of({ count: 5 }));
    mockQueueService.list.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [DuplicateQueuePageComponent],
      providers: [
        { provide: DuplicateQueueService, useValue: mockQueueService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DuplicateQueuePageComponent);
    component = fixture.componentInstance;
  });

  it('khởi tạo thành công và tải số lượng pending + danh sách ứng viên', () => {
    fixture.detectChanges();

    expect(component.pendingCount()).toBe(5);
    expect(component.candidates().length).toBe(1);
    expect(component.isLoading()).toBeFalse();
    expect(mockQueueService.count).toHaveBeenCalledWith(CandidateStatus.PENDING);
    expect(mockQueueService.list).toHaveBeenCalled();
  });

  it('đổi tab trạng thái tải lại danh sách với trạng thái mới', () => {
    fixture.detectChanges();

    component.changeStatus(CandidateStatus.MERGED);

    expect(component.selectedStatus()).toBe(CandidateStatus.MERGED);
    expect(component.pageMeta().page).toBe(1);
    expect(mockQueueService.list).toHaveBeenCalledTimes(2);
  });

  it('openMergeModal mở modal và chọn đúng keepPatientId mặc định', () => {
    component.openMergeModal(sampleCandidate);

    expect(component.isMergeModalOpen).toBeTrue();
    expect(component.selectedCandidate).toBe(sampleCandidate);
    expect(component.keepPatientId).toBe(sampleCandidate.candidatePatient.id);
  });

  it('confirmMerge gọi API merge thành công và hiển thị toast', () => {
    mockQueueService.merge.and.returnValue(of({
      id: 100,
      action: 'MERGE',
      sourcePatientId: 2,
      targetPatientId: 1,
      sourcePreviousStatus: PatientStatus.TEMPORARY,
      duplicateCandidateId: 10,
      relatedMergeId: null,
      eventId: 'evt-1',
      performedByAccountId: 2,
      performedAt: '2026-10-25T10:00:00',
      note: 'Gộp hồ sơ'
    }));

    component.selectedCandidate = sampleCandidate;
    component.keepPatientId = 1;
    component.reviewNote = 'Gộp hồ sơ';

    component.confirmMerge();

    expect(mockQueueService.merge).toHaveBeenCalledWith(10, {
      keepPatientId: 1,
      note: 'Gộp hồ sơ'
    });
    expect(mockToastService.success).toHaveBeenCalledWith('Thành công', 'Đã gộp hồ sơ thành công và phát sự kiện đồng bộ lịch');
    expect(component.isMergeModalOpen).toBeFalse();
  });

  it('confirmReject gọi API reject thành công', () => {
    mockQueueService.reject.and.returnValue(of({
      ...sampleCandidate,
      status: CandidateStatus.NOT_DUPLICATE
    }));

    component.selectedCandidate = sampleCandidate;
    component.reviewNote = 'Không trùng';

    component.confirmReject();

    expect(mockQueueService.reject).toHaveBeenCalledWith(10, { note: 'Không trùng' });
    expect(mockToastService.success).toHaveBeenCalledWith('Thành công', 'Đã xác nhận không trùng thành công');
    expect(component.isRejectModalOpen).toBeFalse();
  });

  it('confirmUnmerge gọi API unmerge thành công và hiển thị toast', () => {
    const mergedCandidate: DuplicateCandidate = {
      ...sampleCandidate,
      status: CandidateStatus.MERGED,
      patient: { ...samplePatient, id: 2, status: PatientStatus.MERGED }
    };

    mockQueueService.unmerge.and.returnValue(of({
      id: 101,
      action: 'UNMERGE',
      sourcePatientId: 2,
      targetPatientId: 1,
      sourcePreviousStatus: PatientStatus.TEMPORARY,
      duplicateCandidateId: 10,
      relatedMergeId: 100,
      eventId: 'evt-2',
      performedByAccountId: 2,
      performedAt: '2026-10-25T11:00:00',
      note: 'Tách hồ sơ'
    }));

    component.selectedCandidate = mergedCandidate;
    component.reviewNote = 'Tách hồ sơ';

    component.confirmUnmerge();

    expect(mockQueueService.unmerge).toHaveBeenCalledWith(2, { note: 'Tách hồ sơ' });
    expect(mockToastService.success).toHaveBeenCalledWith('Thành công', 'Đã tách hồ sơ thành công và hoàn tác trạng thái');
    expect(component.isUnmergeModalOpen).toBeFalse();
  });
});
