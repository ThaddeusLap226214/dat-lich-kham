export interface UploadFileRequest {
  file: File;
  sourceSystemId: number;
}

export interface FileValidationResponse {
  fileToken: string;
  fileName: string;
  fileSize: number;
  rowCount: number;
  columnCount: number;
  detectedColumns: string[];
  validationErrors: ValidationError[];
  isValid: boolean;
}

export interface ValidationError {
  row: number;
  column: string;
  errorMessage: string;
  severity: 'ERROR' | 'WARNING';
}

export interface PreviewRow {
  rowNumber: number;
  data: Record<string, any>;
  validationStatus: 'VALID' | 'WARNING' | 'ERROR';
  validationMessages: string[];
  estimatedMatchScore?: number;
  matchStatus?: 'HIGH_MATCH' | 'MEDIUM_MATCH' | 'LOW_MATCH' | 'NEW_RECORD' | 'ERROR';
}

export interface StartImportRequest {
  sourceSystemId: number;
  columnMappings: Record<string, string>;
  skipDuplicates: boolean;
  duplicateThreshold: number;
  fileToken: string;
}

export interface ImportJob {
  id: number;
  fileName: string;
  sourceSystemId: number;
  sourceSystemName?: string;
  status: ImportJobStatus;
  totalRows: number;
  processedRows: number;
  successCount: number;
  errorCount: number;
  warningCount: number;
  skipCount: number;
  startedAt: string;
  completedAt?: string;
  createdBy?: string;
}

export enum ImportJobStatus {
  PENDING = 'PENDING',
  VALIDATING = 'VALIDATING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export interface ImportJobDetail {
  id: number;
  importJobId: number;
  rowNumber: number;
  status: ImportRowStatus;
  errorMessage?: string;
  patientId?: number;
  masterPatientId?: number;
  matchScore?: number;
  processedAt?: string;
}

export enum ImportRowStatus {
  PENDING = 'PENDING',
  SUCCESS = 'SUCCESS',
  ERROR = 'ERROR',
  SKIPPED = 'SKIPPED',
  WARNING = 'WARNING'
}
