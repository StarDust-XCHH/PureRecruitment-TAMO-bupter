# MO-TA Interaction Log

## Purpose

This document records MO-side API and data behavior that interacts with TA-side interfaces or TA data files.

## Core Rule

Applicant-related identity is `jobId`-first.

- `courseCode` is no longer the primary key for opening identity.
- `courseCode` remains a display field and optional cross-check field.
- This avoids ambiguity when multiple openings share one `courseCode`.

## MO Interfaces

- `GET /api/mo/jobs`
  - Query `moId` (required), returns openings owned by this MO.

- `POST /api/mo/jobs`
  - Creates one opening.
  - `ownerMoId` is resolved from caller `moId`.

- `GET /api/mo/applicants?moId=...`
  - Without `jobId`: merged applicants for all openings owned by this MO.
  - With `jobId`: applicants for that opening only.
  - `courseCode`-only filtering is rejected.

- `GET /api/mo/applicants/detail?moId=...&applicationId=...`
  - Returns full detail for one application.
  - Ownership check resolves by snapshot `jobId`.

- `GET /api/mo/applicants/unread-count?moId=...`
  - Unread count across openings owned by this MO.
  - Ownership join is based on opening `jobId`.

- `POST /api/mo/applications/mark-read`
  - Body: `moId`, `applicationId`.
  - Writes MO read-state and updates TA application to `UNDER_REVIEW`.
  - Ownership check resolves by snapshot `jobId`.

- `POST /api/mo/applications/comment`
  - Body: `moId`, `applicationId`, `text`.
  - Appends MO-only comment thread.
  - Ownership check resolves by snapshot `jobId`.

- `POST /api/mo/applications/select`
  - Body: `jobId` (required), `taId`, `moId` (required), `decision`, optional `comment`, optional `courseCode`.
  - Writes MO decision to TA `application-status.json`.

- `GET /api/mo/applications/resume?moId=...&applicationId=...`
  - Streams resume file after ownership checks.

## Shortlist Contract

Endpoint: `/api/mo/applicants/shortlist`

- `GET`: query `moId`
- `POST`: body `moId`, `jobId`, `applicationId`, optional `taId`, `name`
- `DELETE`: query `moId`, `applicationId`

Backing file: `mountDataTAMObupter/mo/mo-applicant-shortlist.json`

- Row fields include `moId`, `jobId`, `courseCode`, `applicationId`, optional `taId`, `name`, `addedAt`
- Uniqueness: `(moId, applicationId)`

## Shared Data Files Touched

- `mountDataTAMObupter/ta/applications.json`
- `mountDataTAMObupter/ta/application-status.json`
- `mountDataTAMObupter/ta/application-events.json`
- `mountDataTAMObupter/mo/mo-application-read-state.json`
- `mountDataTAMObupter/mo/mo-application-comments.json`
- `mountDataTAMObupter/mo/mo-applicant-shortlist.json`

## Compatibility Note

- Some local tool helpers still expose course-first method signatures.
- HTTP and MO front-end paths are jobId-first.
