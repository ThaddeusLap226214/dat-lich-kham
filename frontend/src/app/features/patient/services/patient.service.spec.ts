import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { PatientService } from './patient.service';
import {
  AddIdentityRequest,
  CreatePatientResponse,
  Gender,
  IdType,
  MergeHistoryResponse,
  Patient,
  PatientRequest,
  PatientStatus,
  SpringPage
} from '../models/patient.model';
import { environment } from '../../../../environments/environment';

describe('PatientService', () => {
  let service: PatientService;
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

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PatientService]
    });
    service = TestBed.inject(PatientService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('mine() gửi GET /patients/mine và trả về danh sách hồ sơ', () => {
    service.mine().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].id).toBe(1);
      expect(data[0].fullName).toBe('Lê Thị Hoa');
    });

    const req = httpMock.expectOne(`${apiUrl}/patients/mine`);
    expect(req.request.method).toBe('GET');
    req.flush([samplePatient]);
  });

  it('getById() gửi GET /patients/:id', () => {
    service.getById(1).subscribe((data) => {
      expect(data.id).toBe(1);
      expect(data.phone).toBe('0905123456');
    });

    const req = httpMock.expectOne(`${apiUrl}/patients/1`);
    expect(req.request.method).toBe('GET');
    req.flush(samplePatient);
  });

  it('create() gửi POST /patients trả về CreatePatientResponse', () => {
    const request: PatientRequest = {
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
      guardianRelation: null
    };

    const mockResponse: CreatePatientResponse = {
      patient: samplePatient,
      outcome: 'CREATED_ACTIVE',
      duplicateQueued: false
    };

    service.create(request).subscribe((res) => {
      expect(res.outcome).toBe('CREATED_ACTIVE');
      expect(res.patient.fullName).toBe('Lê Thị Hoa');
      expect(res.duplicateQueued).toBeFalse();
    });

    const req = httpMock.expectOne(`${apiUrl}/patients`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('update() gửi PUT /patients/:id', () => {
    const request: PatientRequest = {
      fullName: 'Lê Thị Hoa Mai',
      dateOfBirth: '1990-05-12',
      gender: Gender.FEMALE,
      phone: '0905123456',
      email: 'mai@gmail.com',
      address: 'Đà Nẵng',
      idType: null,
      idNumber: null,
      guardianName: null,
      guardianPhone: null,
      guardianRelation: null
    };

    const updatedPatient: Patient = { ...samplePatient, fullName: 'Lê Thị Hoa Mai', email: 'mai@gmail.com' };

    service.update(1, request).subscribe((res) => {
      expect(res.fullName).toBe('Lê Thị Hoa Mai');
      expect(res.email).toBe('mai@gmail.com');
    });

    const req = httpMock.expectOne(`${apiUrl}/patients/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(updatedPatient);
  });

  it('addIdentity() gửi POST /patients/:id/identity với CCCD', () => {
    const idReq: AddIdentityRequest = {
      idType: IdType.CCCD,
      idNumber: '048190001234'
    };

    service.addIdentity(2, idReq).subscribe((res) => {
      expect(res.idType).toBe(IdType.CCCD);
      expect(res.idNumber).toBe('048190001234');
      expect(res.status).toBe(PatientStatus.ACTIVE);
    });

    const req = httpMock.expectOne(`${apiUrl}/patients/2/identity`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(idReq);
    req.flush({ ...samplePatient, id: 2, status: PatientStatus.ACTIVE });
  });

  it('unmerge() gửi POST /patients/:id/unmerge', () => {
    const noteReq = { note: 'Tách hồ sơ nhầm' };
    const mockRes: MergeHistoryResponse = {
      id: 50,
      action: 'UNMERGE',
      sourcePatientId: 3,
      targetPatientId: 1,
      sourcePreviousStatus: PatientStatus.TEMPORARY,
      duplicateCandidateId: 10,
      relatedMergeId: 49,
      eventId: 'evt-123',
      performedByAccountId: 2,
      performedAt: '2026-10-25T12:00:00',
      note: 'Tách hồ sơ nhầm'
    };

    service.unmerge(3, noteReq).subscribe((res) => {
      expect(res.action).toBe('UNMERGE');
      expect(res.sourcePatientId).toBe(3);
    });

    const req = httpMock.expectOne(`${apiUrl}/patients/3/unmerge`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(noteReq);
    req.flush(mockRes);
  });

  it('search() gửi GET /patients với params lọc', () => {
    const mockPage: SpringPage<Patient> = {
      content: [samplePatient],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    };

    service.search({ keyword: 'Hoa', status: PatientStatus.ACTIVE, page: 0, size: 20 }).subscribe((page) => {
      expect(page.content.length).toBe(1);
      expect(page.content[0].fullName).toBe('Lê Thị Hoa');
    });

    const req = httpMock.expectOne((r) =>
      r.url === `${apiUrl}/patients` &&
      r.params.get('keyword') === 'Hoa' &&
      r.params.get('status') === PatientStatus.ACTIVE
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });
});
