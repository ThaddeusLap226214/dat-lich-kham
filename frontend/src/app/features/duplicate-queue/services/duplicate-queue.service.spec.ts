import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DuplicateQueueService } from './duplicate-queue.service';
import {
  CandidateStatus,
  DuplicateCandidate,
  DuplicateReason,
  MergeRequest,
  ReviewNoteRequest
} from '../models/duplicate-candidate.model';
import { Gender, IdType, MergeHistoryResponse, Patient, PatientStatus, SpringPage } from '../../patient/models/patient.model';
import { environment } from '../../../../environments/environment';

describe('DuplicateQueueService', () => {
  let service: DuplicateQueueService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl;

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

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DuplicateQueueService]
    });
    service = TestBed.inject(DuplicateQueueService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('list() gửi GET /duplicate-candidates với các query param chính xác', () => {
    const mockPage: SpringPage<DuplicateCandidate> = {
      content: [sampleCandidate],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1
    };

    service.list({ status: CandidateStatus.PENDING, page: 0, size: 10 }).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].id).toBe(10);
      expect(res.content[0].reason).toBe(DuplicateReason.SAME_NAME_DOB_PHONE);
    });

    const req = httpMock.expectOne((r) =>
      r.url === `${apiUrl}/duplicate-candidates` &&
      r.params.get('status') === CandidateStatus.PENDING &&
      r.params.get('page') === '0' &&
      r.params.get('size') === '10'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('count() gửi GET /duplicate-candidates/count với status', () => {
    service.count(CandidateStatus.PENDING).subscribe((res) => {
      expect(res.count).toBe(3);
    });

    const req = httpMock.expectOne(`${apiUrl}/duplicate-candidates/count?status=PENDING`);
    expect(req.request.method).toBe('GET');
    req.flush({ count: 3 });
  });

  it('merge() gửi POST /duplicate-candidates/:id/merge với body MergeRequest', () => {
    const mergeReq: MergeRequest = { keepPatientId: 1, note: 'Gộp hồ sơ trùng' };
    const mockRes: MergeHistoryResponse = {
      id: 100,
      action: 'MERGE',
      sourcePatientId: 2,
      targetPatientId: 1,
      sourcePreviousStatus: PatientStatus.TEMPORARY,
      duplicateCandidateId: 10,
      relatedMergeId: null,
      eventId: 'event-uuid-123',
      performedByAccountId: 2,
      performedAt: '2026-10-25T10:00:00',
      note: 'Gộp hồ sơ trùng'
    };

    service.merge(10, mergeReq).subscribe((res) => {
      expect(res.action).toBe('MERGE');
      expect(res.sourcePatientId).toBe(2);
      expect(res.targetPatientId).toBe(1);
    });

    const req = httpMock.expectOne(`${apiUrl}/duplicate-candidates/10/merge`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mergeReq);
    req.flush(mockRes);
  });

  it('reject() gửi POST /duplicate-candidates/:id/reject với body ReviewNoteRequest', () => {
    const noteReq: ReviewNoteRequest = { note: 'Không trùng, 2 người khác nhau' };
    const updatedCandidate: DuplicateCandidate = {
      ...sampleCandidate,
      status: CandidateStatus.NOT_DUPLICATE,
      reviewNote: 'Không trùng, 2 người khác nhau'
    };

    service.reject(10, noteReq).subscribe((res) => {
      expect(res.status).toBe(CandidateStatus.NOT_DUPLICATE);
      expect(res.reviewNote).toBe('Không trùng, 2 người khác nhau');
    });

    const req = httpMock.expectOne(`${apiUrl}/duplicate-candidates/10/reject`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(noteReq);
    req.flush(updatedCandidate);
  });

  it('unmerge() gửi POST /patients/:id/unmerge', () => {
    const noteReq: ReviewNoteRequest = { note: 'Tách lại hồ sơ gộp nhầm' };
    const mockRes: MergeHistoryResponse = {
      id: 101,
      action: 'UNMERGE',
      sourcePatientId: 2,
      targetPatientId: 1,
      sourcePreviousStatus: PatientStatus.TEMPORARY,
      duplicateCandidateId: 10,
      relatedMergeId: 100,
      eventId: 'event-uuid-456',
      performedByAccountId: 2,
      performedAt: '2026-10-25T11:00:00',
      note: 'Tách lại hồ sơ gộp nhầm'
    };

    service.unmerge(2, noteReq).subscribe((res) => {
      expect(res.action).toBe('UNMERGE');
      expect(res.sourcePatientId).toBe(2);
    });

    const req = httpMock.expectOne(`${apiUrl}/patients/2/unmerge`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(noteReq);
    req.flush(mockRes);
  });

  it('getMergeHistory() gửi GET /patients/:id/merge-history', () => {
    service.getMergeHistory(2).subscribe((histories) => {
      expect(histories.length).toBe(1);
      expect(histories[0].action).toBe('MERGE');
    });

    const req = httpMock.expectOne(`${apiUrl}/patients/2/merge-history`);
    expect(req.request.method).toBe('GET');
    req.flush([{
      id: 100,
      action: 'MERGE',
      sourcePatientId: 2,
      targetPatientId: 1,
      sourcePreviousStatus: PatientStatus.TEMPORARY,
      duplicateCandidateId: 10,
      relatedMergeId: null,
      eventId: 'event-uuid-123',
      performedByAccountId: 2,
      performedAt: '2026-10-25T10:00:00',
      note: 'Gộp hồ sơ trùng'
    }]);
  });
});
