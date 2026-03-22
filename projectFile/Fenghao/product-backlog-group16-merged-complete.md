# Product Backlog — Group 16 (Merged, single source)

**Branch:** `Fenghao/General`  
**Canonical copy:** this file under `projectFile/Fenghao/`.  
A duplicate for first-assessment packaging may also live at `projectFile/1st assignment/product-backlog-merged-user-stories.md` — keep in sync with this document when updating.

---

## Overview

This document consolidates user stories from project materials into one **scoring-aligned** Product Backlog, with explicit coverage for:

- clear user and scope definition (`User Role` + `Scope`)
- proper user story format (`As a..., I want..., so that...`)
- backlog completeness (`Priority` + iteration / `Sprint` + **estimation** + **verifiable acceptance criteria**)
- traceability (`Dependency`, `MVP`, detailed `Source`)

**Extra columns** (vs QM+ Excel minimum): `User Role`, `Scope`, `Dependency`, `MVP`, `Source`, free-text `Notes`, and handout-based **planned dates**.

### Primary sources

- `projectFile/Yilin/user_stories.md`
- `projectFile/StarDustXCHH/Product_Backlog_Stardust.md`
- `projectFile/Huiying Liu/brainstorm.md` (reference only)
- `projectFile/Fenghao/Evidence.md` (workshop + forum: recruitment scope, CV/PDF + text path, MO review visibility pain point, admin highest access)

### Handout reference (dates & submission rules)

From **`projectFile/EBU6304_GroupProjectHandout.txt`** (same content as QM+ PDF; no separate `.md` in repo).

| Item | Date |
|------|------|
| Handout release | 6 March 2026 |
| First assessment (BUPT Week 3 — Iteration 1) | **22 March 2026** |
| Iteration 1 continues (Week 4) | 29 March 2026 |
| Iteration 2 starts (Week 5) | 5 April 2026 |
| Intermediate assessment (Week 6) | **12 April 2026** |
| Iteration 4 (Week 9) | 3 May 2026 |
| Final assessment (Week 12) | **24 May 2026** |

**QM+ First assessment (30%)** — backlog file must include: user stories, **acceptance criteria**, **priority**, **estimation**, **iteration planning** (all in the table below).

### Story point scale (estimation)

> `2` = small · `3` = medium · `5` = large · `8` = high uncertainty  

Use **Estimation (Story Points)** as the **estimation** column when copying to `ProductBacklog_groupXXX.xlsx`.

### MVP column

> **`MVP = Yes`** = first-assessment / core workflow emphasis (prototype + early iteration), not “full final product”.

### Planned dates (team plan aligned to handout)

| Sprint | Planned start | Planned end / milestone |
|--------|---------------|-------------------------|
| 1 | 6 March 2026 | 22 March 2026 (First assessment) |
| 2 | 29 March 2026 | 12 April 2026 (Intermediate assessment) |
| 3 | 13 April 2026 | 2 May 2026 (pre–Iteration 4) |
| 4 | 3 May 2026 | 24 May 2026 (Final assessment) |

- Iteration **`1-2`** → date span **6 Mar 2026 – 12 Apr 2026**.  
- Iteration **`2-3`** → **29 Mar 2026 – 2 May 2026**.

---

## Product Backlog (full table)

| Story ID | User Role | Scope | Story Name | Description (User Story) | Priority | Iteration (Sprint) | Estimation (Story Points) | Dependency | MVP | Acceptance Criteria (Verifiable) | Source | Notes | Date started (planned) | Date finished (planned) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| MB-01 | TA/MO/Admin | Auth | Secure Login | As a system user, I want to log in securely, so that I can access my role-specific functions. | High | 1 | 3 | - | Yes | 1) Users can log in with identifier + password. 2) Invalid credentials show an error message. 3) Successful login redirects to the correct role workspace. | Huiying + StarDust | Core auth; text-based credential store per handout. | 2026-03-06 | 2026-03-22 |
| MB-02 | TA/MO | Auth | Role Selection | As a user, I want to choose my role before login, so that I can enter the correct workflow. | High | 1 | 2 | MB-01 | Yes | 1) TA/MO role entry is visible. 2) Selected role state is applied in login flow. 3) Incorrect role path is blocked by front-end validation. | StarDust US-001 | Reduces wrong-entry confusion; aligns with prototype direction. | 2026-03-06 | 2026-03-22 |
| MB-03 | TA | Auth/Profile | TA Registration with Auto ID | As a TA, I want to register with an auto-generated TA ID, so that I can start quickly. | High | 1 | 3 | MB-02 | Yes | 1) TA ID is auto-generated on registration. 2) Form validation is required before submit. 3) Registration data is persisted in text-based storage. | StarDust US-007/015 | JSON/text persistence only (no DB). | 2026-03-06 | 2026-03-22 |
| MB-04 | TA | Profile | Profile Create/Edit | As a TA, I want to create and edit my profile, so that MO can evaluate my qualifications. | High | 1 | 3 | MB-03 | Yes | 1) Profile form supports core info and skills. 2) Saved data is reloaded on revisit. 3) Updates correctly overwrite old values. | Yilin Story 1 + US-032 | Handout: TA applicant profile. | 2026-03-06 | 2026-03-22 |
| MB-05 | TA | Profile | CV Upload | As a TA, I want to upload my CV, so that I can apply for positions. | High | 1 | 3 | MB-04 | Yes | 1) CV upload supports file validation. 2) Storage layer saves file path and metadata. 3) MO can open the uploaded CV. | Yilin Story 2 | See MB-29 for PDF + path evidence alignment. | 2026-03-06 | 2026-03-22 |
| MB-06 | TA | Jobs | Browse/Search Jobs | As a TA, I want to browse and search available jobs, so that I can find suitable opportunities efficiently. | High | 1 | 3 | MB-01 | Yes | 1) Job list is displayed from local data. 2) Keyword search filters the list. 3) Empty-state messaging is shown when no jobs exist. | Yilin Story 3 + US-024 | Handout: find available jobs. | 2026-03-06 | 2026-03-22 |
| MB-07 | TA | Jobs | Job Detail View | As a TA, I want to view job details before applying, so that I can make informed decisions. | High | 1 | 2 | MB-06 | Yes | 1) Job details can be opened from job list. 2) Details include requirements and schedule/location. 3) Detail view is linked to apply action context. | StarDust US-027 | Supports informed apply (prototype-led). | 2026-03-06 | 2026-03-22 |
| MB-08 | TA | Application | One-Click Apply | As a TA, I want to apply in one clear action, so that my application is recorded immediately. | High | 2 | 5 | MB-05, MB-07 | No | 1) Apply action creates a new application record. 2) Duplicate applications are prevented or warned. 3) New record appears in status tracking. | Yilin Story 4 + US-042 | Handout: apply for jobs. | 2026-03-29 | 2026-04-12 |
| MB-09 | TA | Application | Status Timeline | As a TA, I want a timeline for application status, so that I always know progress. | High | 1-2 | 3 | MB-08 | Yes | 1) Application status can be queried by TA ID. 2) Each item shows current status and update time. 3) Step history can be expanded/viewed. | Yilin Story 5 + US-028 | Workshop pain point: track MO review (see MB-30). | 2026-03-06 | 2026-04-12 |
| MB-10 | TA | Application | Status Summary/Notifications | As a TA, I want summary counts and notifications, so that I can monitor all applications quickly. | High | 1-2 | 2 | MB-09 | Yes | 1) Summary counters are visible. 2) Notification panel shows pending actions. 3) Neutral empty state is shown when no reminders exist. | StarDust US-029 | Complements timeline for at-a-glance monitoring. | 2026-03-06 | 2026-04-12 |
| MB-11 | MO | Auth | MO Secure Login | As a MO, I want to log in securely, so that I can manage recruitment tasks. | High | 1 | 2 | MB-01 | Yes | 1) MO credentials are validated. 2) Login failures show clear messages. 3) Successful login opens MO workspace. | Yilin Story 6 | MO role entry. | 2026-03-06 | 2026-03-22 |
| MB-12 | MO | Jobs | Post TA Jobs | As a MO, I want to post TA jobs with requirements, so that students can apply to suitable roles. | High | 1-2 | 3 | MB-11 | Yes | 1) Posting form includes module and skills requirements. 2) Saved jobs appear in TA-visible list. 3) Job edits are persisted. | Yilin Story 7 | Workshop: MO should state required skills when posting. | 2026-03-06 | 2026-04-12 |
| MB-13 | MO | Application | View Applicants by Job | As a MO, I want to view applicants per job, so that I can compare candidates. | High | 2 | 3 | MB-12, MB-08 | No | 1) Applicants are listed by job. 2) Profile and CV can be opened per applicant. 3) Basic filtering/sorting is available. | Yilin Story 8 | Depends on applications existing. | 2026-03-29 | 2026-04-12 |
| MB-14 | MO | Application | Select Applicants | As a MO, I want to select suitable applicants and publish results, so that decisions are finalized and visible to TA. | High | 2 | 5 | MB-13 | No | 1) MO can mark accepted/rejected candidates. 2) Decisions are persisted. 3) TA status view reflects decision updates. | Yilin Story 9 | Handout: MO selects applicants. | 2026-03-29 | 2026-04-12 |
| MB-15 | Admin | Admin | Workload Check | As an admin, I want to review TA workload, so that I can avoid over-allocation. | Medium | 3 | 3 | MB-14 | No | 1) Admin panel shows TA workload metrics. 2) Potentially overloaded TAs are identifiable. 3) Data can be viewed by role/job dimension. | Yilin Story 10 | Handout suggested admin function. | 2026-04-13 | 2026-05-02 |
| MB-16 | Admin | Admin | Applicant Record Management | As an admin, I want to manage applicant records, so that data remains accurate and organized. | Medium | 3 | 3 | MB-13 | No | 1) Admin can search applicant records. 2) Incorrect fields can be updated. 3) Changes are persisted and traceable. | Yilin Story 11 | Governance / data hygiene. | 2026-04-13 | 2026-05-02 |
| MB-17 | TA | Intelligence | Missing Skills Feedback | As a TA, I want feedback on missing skills, so that I can improve future applications. | Low | 4 | 5 | MB-04, MB-12 | No | 1) System compares TA skills with job requirements. 2) Missing skills list is generated. 3) Feedback is clearly presented. | Yilin Story 12 | Handout AI idea; explainable per handout 2.3. | 2026-05-03 | 2026-05-24 |
| MB-18 | MO | Intelligence | Candidate Matching Support | As a MO, I want system-generated candidate matching, so that I can decide faster. | Low | 4 | 8 | MB-13 | No | 1) Matching score is generated per candidate. 2) Candidate list is ranked by score. 3) Key matching factors are shown. | Yilin Story 13 | Handout AI idea; explainable ranking. | 2026-05-03 | 2026-05-24 |
| MB-19 | Admin | Intelligence/Admin | Workload Warning Alerts | As an admin, I want workload warnings, so that I can balance assignments fairly. | Low | 4 | 3 | MB-15 | No | 1) Overload threshold is configurable. 2) Warning appears when threshold is exceeded. 3) Risk is visible before final assignment confirmation. | Yilin Story 14 | Handout AI idea: balancing workload. | 2026-05-03 | 2026-05-24 |
| MB-20 | TA | Profile | Settings Center | As a TA, I want one place for profile/security/preferences, so that account maintenance is simple. | High | 1 | 3 | MB-04 | Yes | 1) Settings center includes tabbed sections. 2) Explicit save is required after edits. 3) Saved/unsaved states are clearly indicated. | StarDust US-031/033 | Single workspace maintenance. | 2026-03-06 | 2026-03-22 |
| MB-21 | TA | Profile | Avatar Upload/Crop | As a TA, I want to upload and crop my avatar, so that my profile appears complete and professional. | High | 1 | 3 | MB-20 | Yes | 1) File type and size validation is enforced. 2) Crop flow is supported before upload. 3) Final avatar is stored and rendered reliably. | StarDust US-035/036 | Optional polish; text/path rules still apply. | 2026-03-06 | 2026-03-22 |
| MB-22 | TA | Security | Password Change | As a TA, I want to change my password securely, so that my account remains protected. | High | 1 | 3 | MB-01 | Yes | 1) Current password and new password rules are validated. 2) Weak/reused passwords are rejected. 3) New salt/hash is persisted successfully. | StarDust US-038 | Security hardening. | 2026-03-06 | 2026-03-22 |
| MB-23 | TA | Intelligence | Explainable Recommendations | As a TA, I want recommendations with reasons, so that I can trust and act on suggestions. | Medium | 2 | 5 | MB-06, MB-04 | No | 1) Recommended jobs are ranked by score. 2) Each recommendation includes explanation factors. 3) Recalculation works after profile updates. | StarDust US-041/051 | Explainability if AI-assisted. | 2026-03-29 | 2026-04-12 |
| MB-24 | TA | Intelligence | Availability Matching | As a TA, I want to record availability and avoid schedule conflicts, so that I apply only to feasible jobs. | High | 2 | 5 | MB-08, MB-06 | No | 1) Availability editor is provided. 2) Apply flow checks schedule conflicts. 3) Conflicts trigger clear warning messages. | StarDust US-044 | Not a full timetable product (workshop scope). | 2026-03-29 | 2026-04-12 |
| MB-25 | TA/MO/Admin | Security | Session Access Control | As a user, I want protected routes to require valid session, so that unauthorized access is blocked. | High | 2 | 5 | MB-01 | No | 1) Protected routes require session validation. 2) Unauthenticated requests are redirected or return 401. 3) Role-based access boundaries are enforced. | StarDust US-047 | Production-like demo. | 2026-03-29 | 2026-04-12 |
| MB-26 | TA/MO/Admin | Security | Brute-Force Lockout | As a platform owner, I want lockout for repeated failed logins, so that accounts are safer from abuse. | High | 2 | 3 | MB-01 | No | 1) Failed-attempt threshold triggers temporary lockout. 2) UI shows lockout and retry guidance. 3) Lockout metadata is persisted. | StarDust US-048 | Builds on failed-attempt counting. | 2026-03-29 | 2026-04-12 |
| MB-27 | TA | UX | Mobile-Friendly Experience | As a TA, I want mobile-friendly pages, so that I can check jobs and status on phone/tablet. | Medium | 2 | 3 | MB-06, MB-09 | No | 1) Core pages are usable on narrow screens. 2) Primary actions support touch interaction. 3) Typography and spacing remain readable on mobile. | StarDust US-049 | UX completeness. | 2026-03-29 | 2026-04-12 |
| MB-28 | TA | Analytics | Recruitment Dashboard | As a TA, I want analytics charts for applications and outcomes, so that I can understand progress over time. | Medium | 3 | 5 | MB-09, MB-10 | No | 1) Dashboard shows key metrics and trends. 2) Time-range filtering is available. 3) Empty data states are handled with clear guidance. | StarDust US-050 | Analytics on status data. | 2026-04-13 | 2026-05-02 |
| MB-29 | TA/MO | Storage | CV Path-Based Storage Compliance | As a MO, I want uploaded CV files stored with text-based relative paths, so that the system stays compliant with text-file storage constraints while preserving original PDF formatting. | High | 1 | 3 | MB-05 | Yes | 1) CV files are saved in a controlled folder. 2) Text metadata stores relative file paths for each CV. 3) MO can open original CV files from stored paths. | Fenghao Evidence | Forum (Dr. Ling Ma): PDF in folder + path in text file; keep simple. | 2026-03-06 | 2026-03-22 |
| MB-30 | TA | Application | MO Review Progress Visibility | As a TA, I want to see MO review progress stages, so that I can track what is happening after submission. | High | 1-2 | 3 | MB-09 | Yes | 1) Status stages include at least Submitted, In Review, Interview, and Final Decision. 2) Each stage change records an update timestamp. 3) TA timeline clearly shows current stage and recent transition. | Fenghao Evidence | Workshop: cannot track MO review progress — key pain point. | 2026-03-06 | 2026-04-12 |
| MB-31 | Admin | Security/Admin | Highest-Privilege Admin Access Control | As an admin, I want highest-level permission boundaries with controlled role actions, so that governance is clear and unauthorized operations are prevented. | High | 2-3 | 5 | MB-25 | No | 1) Role permissions are explicitly defined for TA, MO, and Admin. 2) Admin-only operations are protected from TA/MO access. 3) Unauthorized role actions return clear denial responses. | Fenghao Evidence | Workshop: admin has highest access. | 2026-03-29 | 2026-05-02 |
| MB-32 | TA | Application | Withdraw Application | As a TA, I want to withdraw an application before final decision, so that I can correct mistakes or change plans. | Medium | 2 | 3 | MB-08 | No | 1) TA can withdraw only non-final applications. 2) Status changes to Withdrawn with timestamp. 3) MO view updates immediately. | Fenghao Brainstorm | Process refinement. | 2026-03-29 | 2026-04-12 |
| MB-33 | MO | Jobs | Job Closing Control | As a MO, I want to close a job posting, so that no further applications are accepted after deadline or quota. | High | 2 | 3 | MB-12 | Yes | 1) MO can set Open/Closed state. 2) Closed jobs block new applications. 3) TA side clearly shows closed status. | Fenghao Brainstorm | MO operational control. | 2026-03-29 | 2026-04-12 |
| MB-34 | TA/MO | Notification | Decision Notification | As a TA, I want to receive a notification when MO updates my result, so that I do not need to check manually. | High | 2 | 3 | MB-14 | Yes | 1) Status change triggers in-app notification. 2) Notification contains job and decision summary. 3) Read/unread state is supported. | Fenghao Brainstorm | Reinforces decision visibility. | 2026-03-29 | 2026-04-12 |
| MB-35 | MO | Application | Applicant Shortlist | As a MO, I want to shortlist candidates before final decision, so that I can manage review in stages. | Medium | 2 | 5 | MB-13 | No | 1) MO can mark shortlisted applicants. 2) Shortlist filter is available in applicant list. 3) Shortlisted state is persisted. | Fenghao Brainstorm | Staged review workflow. | 2026-03-29 | 2026-04-12 |
| MB-36 | Admin | Audit | Action Audit Log | As an admin, I want an audit trail of key actions, so that disputes and mistakes can be traced. | High | 3 | 5 | MB-25 | No | 1) Key actions (post/apply/select/update) are logged. 2) Log includes actor, time, action, target. 3) Admin can query logs by role/time. | Fenghao Brainstorm | Accountability. | 2026-04-13 | 2026-05-02 |
| MB-37 | TA | Profile | Mandatory Completion Gate | As a TA, I want clear mandatory profile checks, so that I know what to complete before applying. | High | 1 | 2 | MB-04 | Yes | 1) Required fields are marked explicitly. 2) Apply button is disabled until mandatory data is complete. 3) Missing fields are listed to user. | Fenghao Brainstorm | Enforces complete-before-apply. | 2026-03-06 | 2026-03-22 |
| MB-38 | MO/Admin | Data | CSV Export for Reports | As a MO/Admin, I want to export applicants and decisions to CSV, so that reporting is easier. | Medium | 3 | 3 | MB-13, MB-14 | No | 1) Export includes selected filter range. 2) CSV has consistent headers and encoding. 3) Export file is downloadable and readable in Excel. | Fenghao Brainstorm | Aligns with text/CSV storage theme. | 2026-04-13 | 2026-05-02 |
| MB-39 | TA/MO/Admin | Security | Password Reset (Admin-assisted) | As a user, I want a secure reset process when password is forgotten, so that account recovery is possible. | Medium | 3 | 5 | MB-22 | No | 1) Reset request is recorded and verified by role policy. 2) Temporary reset token/code has expiry. 3) Reset success invalidates old credentials. | Fenghao Brainstorm | Keep implementation simple. | 2026-04-13 | 2026-05-02 |
| MB-40 | TA | UX | Application Draft Save | As a TA, I want to save a draft before final submit, so that I can finish application details later. | Low | 3 | 3 | MB-08 | No | 1) TA can save and reopen draft applications. 2) Drafts are not visible to MO until submit. 3) Submit converts draft to official application record. | Fenghao Brainstorm | Optional UX. | 2026-04-13 | 2026-05-02 |
| MB-41 | TA/MO | Reliability | Concurrent Update Protection | As a user, I want the system to prevent accidental overwrite when data was changed by others, so that records stay consistent. | Medium | 3 | 5 | MB-12, MB-13 | No | 1) Update checks version/timestamp. 2) Conflict returns clear message. 3) User can reload and retry safely. | Fenghao Brainstorm | Consistency under concurrent edits. | 2026-04-13 | 2026-05-02 |

---

## Notes on filtering

- Purely visual styling stories (e.g. decorative animation) are **deprioritized** here.
- Overlapping items were **merged** for one coherent flow: **login → profile → jobs → application → MO review → admin**.
- For **QM+ Excel**: use **Estimation (Story Points)** as estimation; **Iteration (Sprint)** as iteration planning.

### QM+ Excel column mapping (suggested)

| QM+ / template field | This document |
|---------------------|----------------|
| Story ID | `Story ID` |
| Story name | `Story Name` |
| User story / description | `Description (User Story)` |
| Priority | `Priority` |
| Iteration / Sprint | `Iteration (Sprint)` |
| Estimation | `Estimation (Story Points)` |
| Acceptance criteria | `Acceptance Criteria (Verifiable)` |
| Notes (if present) | `Notes` + fold in `Dependency`, `Source`, `User Role`, `Scope`, `MVP` if space is limited |
| Dates (if present) | `Date started (planned)` / `Date finished (planned)` |

---

## Worktree → main repo (optional)

If edits were made in a **git worktree** (detached HEAD), merge into `D:/work/se` on **`Fenghao/General`**:

1. In worktree: `git switch -c temp/backlog-merge` → `git add` → `git commit`
2. In main repo: `git switch Fenghao/General` → `git cherry-pick <commit>`
3. Remove worktree when done: `git worktree remove <path>`

---

## References

- `projectFile/EBU6304_GroupProjectHandout.txt`
- `projectFile/Fenghao/Evidence.md`
