# MO-TA Interaction Log

## Purpose

This log tracks MO-side changes that interact with TA-side interfaces or TA data files.

## New/Updated MO Interfaces

- `GET /api/mo/applicants?courseCode=...`
  - Reads TA account/profile/application status data.
  - Data sources:
    - `mountDataTAMObupter/ta/tas.json`
    - `mountDataTAMObupter/ta/profiles.json`
    - `mountDataTAMObupter/ta/application-status.json`

- `POST /api/mo/applications/select`
  - Writes MO selection decision back to TA application status data.
  - Writes to:
    - `mountDataTAMObupter/ta/application-status.json`
  - Fields updated/created include:
    - `status`
    - `statusTone`
    - `summary`
    - `moComment`
    - `nextAction`
    - `nextStep`
    - `updatedAt`
    - `jobSlug`

## TA UI Impact

- No TA page/JSP/CSS/JS file was modified.
- TA interface behavior remains unchanged.

## Notes

- MO pages are added under `pages/mo`.
- MO JS modules are added under `assets/mo/js`.
- MO backend logic is added under `com.bupt.tarecruit.mo`.
