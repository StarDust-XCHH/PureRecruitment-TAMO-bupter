# Brief Report - Group 16

**Module:** EBU6304 Software Engineering Group Project  
**Project:** International School Teaching Assistant Recruitment System  
**Assessment Stage:** First Assessment (Backlog + Prototype + Brief Report)

---

## Project Overview

This report explains how Group 16 converted the handout's high-level requirements into a practical and evidence-based Agile plan. The product is a lightweight Java Servlet/JSP recruitment system for three roles:

- Teaching Assistant (TA)
- Module Organiser (MO)
- Admin

The report follows the handout requirement for First Assessment and covers:

1. Fact-finding techniques
2. Iteration planning
3. Prioritisation method
4. Estimation method

Supporting evidence and materials are provided in appendix sections.

---

## 1. Fact-Finding Techniques

To avoid assumption-driven design, we used multiple requirement-finding methods and triangulated them with real evidence.

### 1.1 Handout-Driven Requirement Extraction

From the handout (`projectFile/EBU6304_GroupProjectHandout.txt`), we extracted:

- mandatory technical constraints:
  - Java-based solution (Servlet/JSP accepted)
  - data storage must use simple text formats (JSON/CSV/XML/TXT), no database
- core functional direction:
  - TA profile/CV/job/application/status
  - MO posting and selection
  - Admin workload checking
- process expectation:
  - Agile iterations, prioritised delivery, documented decisions and trade-offs

This formed the baseline boundary for scope control.

### 1.2 Story Writing Workshop (Stakeholder Clarification)

We used the official story-writing workshop as direct fact-finding with end users.

**Evidence source:** `projectFile/Fenghao/Evidence.md`

**Fact record (time/place/people/event):**

- **Time:** 13 March 2026, 08:50-09:35 (Beijing Time)
- **Place:** TB3-435
- **People:** Group 16 representatives (Yuhan Guan, Fenghao Zhang), lecturer Dr. Gokop Goteng, two TAs as end users
- **Event:** story-writing workshop requirement clarification meeting

In this workshop, the team and stakeholders clarified the following decisions:

- The product should be a **recruitment system**, not a timetable or temporary-task system.
- Recruitment is oriented to semester-scale TA hiring.
- MO job posting should include required skills.
- A major pain point in current process is **TA cannot track MO review progress**.
- Admin role has highest access permission.

These findings directly influenced our scope and backlog ordering.

### 1.3 Forum Clarification for Requirement Conflict (CV Format vs Text Storage Rule)

We identified a requirement conflict:

- expected user behavior: CVs are usually PDF
- technical constraint: text-file storage only

**Evidence source:** `projectFile/Fenghao/Evidence.md` (forum thread and lecturer reply)

**Fact record (time/people/event):**

- **Time 1:** 13 March 2026, 10:27 PM (Beijing Time)
- **People 1:** Fenghao Zhang (on behalf of Group 16)
- **Event 1:** posted clarification request on QMPlus forum about CV PDF handling under text-only storage constraint

- **Time 2:** 13 March 2026, 11:29 PM (Beijing Time)
- **People 2:** Dr. Ling Ma (MO / lecturer-side stakeholder response)
- **Event 2:** provided official guidance on acceptable simple solutions

Based on the forum exchange, the team adopted the following decision rule:

- team should propose and agree a practical solution with stakeholders
- acceptable approach: store PDF files in folder, and keep relative path in text-based metadata (e.g., JSON)
- keep the solution simple

This clarification became a concrete architectural decision rule.

### 1.4 User Story and Workflow Decomposition

We translated requirement findings into structured user stories (`projectFile/Yilin/user_stories.md`) and mapped dependencies:

1. role entry and account
2. TA profile and job browse
3. apply and status tracking
4. MO posting and applicant review
5. selection result feedback to TA
6. admin oversight

This helped avoid implementing isolated UI without process continuity.

### 1.5 Prototype-Led Discovery

By building low/medium-fidelity prototypes (`projectFile/StarDustXCHH/Product_Backlog_Stardust.md` + prototype images), we discovered additional requirements that text-only stories missed:

- clearer TA/MO entry separation
- unified workspace navigation
- job detail before apply
- editable profile/settings
- timeline-style status visibility

### 1.6 How Factual Evidence Changed Scope

The evidence above changed our implementation and planning priorities in concrete ways:

- Workshop pain-point ("cannot track MO review progress") raised the priority of status timeline and update flow.
- Workshop scope correction prevented us from building a timetable-centric product.
- Forum clarification on CV conflict led to a practical architecture decision (file storage + text metadata path) consistent with handout constraints.
- Admin high-permission clarification influenced role boundary planning for later sprints.

---

## 2. Iteration Planning

### 2.1 Planning Principles

Iteration planning followed these principles:

1. deliver core workflow first
2. build dependencies before downstream stories
3. align with assessment milestones
4. postpone high-uncertainty enhancements

### 2.2 Sprint Structure

Based on handout timeline and backlog:

- **Sprint 1 (First Assessment focus):**
  - backlog completion with acceptance criteria
  - low/medium-fidelity prototype
  - role entry + TA core workspace skeleton (profile/jobs/status)
  - JSON persistence foundation
  - evidence-ready brief report
- **Sprint 2:**
  - complete TA application flow
  - MO posting and applicant review actions
  - selection decision write-back to status
  - stronger data consistency and session handling
- **Sprint 3:**
  - admin workload/record views
  - reliability improvements and testing expansion
- **Sprint 4:**
  - explainable AI-oriented enhancements (matching, missing skills, balancing)

### 2.3 Why This Plan

This plan reduces delivery risk and supports incremental demonstration:

- early visible value for stakeholders
- easier feedback incorporation
- controlled technical complexity under coursework constraints

---

## 3. Prioritisation Method

### 3.1 Criteria Used

We used a practical priority model combining:

- user value and pain-point impact
- dependency criticality
- implementation feasibility under sprint deadline
- assessment relevance

### 3.2 Priority Rules

- **High priority:** core workflow blockers or direct stakeholder pain-point features
- **Medium priority:** usability/completeness improvements
- **Low priority:** advanced intelligence or non-blocking enhancements

### 3.3 Core-First Outcome

Stories such as login, profile, job browse, status tracking, MO posting/review were prioritised before AI extensions.  
This directly reflects workshop evidence that "status traceability" is a major user pain-point.

### 3.4 Trade-Off Statement

We intentionally chose:

- coherent end-to-end recruitment baseline  
instead of
- many partially implemented advanced features

This trade-off is consistent with Agile incremental delivery and first-assessment objectives.

---

## 4. Estimation Method

### 4.1 Relative Story-Point Estimation

We used lightweight story points (2/3/5/8) in early stories:

- 2: small/low uncertainty
- 3: moderate feature with multi-step logic
- 5: larger feature with persistence and dependencies
- 8: high uncertainty/cross-module effort

### 4.2 T-Shirt Sizing for Backlog Expansion

In expanded backlog planning, we also used S/M/L:

- S: isolated and straightforward
- M: moderate front/back coordination
- L: cross-module or schema-impacting features

### 4.3 Estimation Factors

Estimation considered:

- affected modules/routes
- validation and state complexity
- persistence impact (JSON schema changes)
- front-end/back-end integration effort
- uncertainty and test risk

### 4.4 Suitability

Because this stage is discovery-heavy, relative estimation is more realistic than hour-based fixed prediction.

---

## 5. Supporting Materials (Outside Main 5-Page Body if Needed)

### 5.1 Example Requirement Interview Questions

1. Which profile fields are mandatory before TA can apply?
2. Can TA edit profile after submitting applications?
3. Which applicant criteria are most important for MO?
4. Should MO support multiple selected applicants per position?
5. Which status labels are most helpful to TA?
6. What workload threshold should trigger admin warning?
7. Which job information is mandatory before apply?
8. Which actions must be role-restricted?
9. How transparent should AI recommendation explanations be?
10. What is the minimum acceptable first usable release?

### 5.2 Early Feedback Summary

From workshop/prototype review:

- role separation must be obvious
- TA needs one coherent workspace (profile/jobs/status)
- status tracking is a key pain-point and must be visible
- job details are required before application
- settings should remain editable
- solution should remain simple and modular

### 5.3 Factual Evidence Log (Time/Place/People/Event)

| No. | Time | Place / Channel | People | Event | Key Output |
|---|---|---|---|---|---|
| 1 | 13 Mar 2026, 08:50-09:35 (BJ) | TB3-435 workshop | Yuhan Guan, Fenghao Zhang, Dr. Gokop Goteng, 2 TAs | Story-writing workshop | Confirmed "recruitment system" scope, status-tracking pain point, and admin high-access role |
| 2 | 13 Mar 2026, 10:27 PM (BJ) | QMPlus forum | Fenghao Zhang (for Group 16) | Posted CV PDF vs text-storage conflict question | Raised requirement contradiction for stakeholder decision |
| 3 | 13 Mar 2026, 11:29 PM (BJ) | QMPlus forum | Dr. Ling Ma | Stakeholder reply to conflict | Accepted simple solution: keep PDF files and store relative paths in text-based records |

---

## 6. Evidence Traceability Matrix


| Report Claim                                                   | Evidence                                                               | Location                                               |
| -------------------------------------------------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------ |
| We used stakeholder workshop as fact-finding                   | Story-writing workshop record with participants and clarified scope    | `projectFile/Fenghao/Evidence.md`                      |
| Status tracking is a key user pain-point                       | Workshop note explicitly states inability to track MO review progress  | `projectFile/Fenghao/Evidence.md`                      |
| CV storage conflict was handled via stakeholder clarification  | Forum thread + lecturer response on PDF storage with text-path linkage | `projectFile/Fenghao/Evidence.md`                      |
| Core requirements and constraints come from handout            | Servlet/JSP option, no database, core functions, Agile process         | `projectFile/EBU6304_GroupProjectHandout.txt`          |
| Stories include acceptance criteria/priority/estimation/sprint | Structured story set                                                   | `projectFile/Yilin/user_stories.md`                    |
| Backlog and prototype are evidence-backed                      | Expanded backlog and image evidence                                    | `projectFile/StarDustXCHH/Product_Backlog_Stardust.md` |
| Team planning and architecture context are documented          | repository overview and structure                                      | `README.md`                                            |


---

## 7. Conclusion

Group 16 used a structured, evidence-driven Agile approach to transform abstract handout requirements into a practical backlog and prototype baseline.  
The team combined handout analysis, workshop clarification, forum-based conflict resolution, user-story decomposition, and prototype validation.

This process produced:

- clearer scope boundaries
- justified iteration ordering
- transparent prioritisation decisions
- realistic estimation under uncertainty
- documented evidence for report claims

The current direction provides a solid foundation for Sprint 2+ implementation while keeping the design simple, modular, and extensible as required.

---

## Appendix A - Group Members

- StarDust-XCHH (Yuhan Guan)
- zfh53 (Fenghao Zhang)
- 6a696c6c (Yufeng Li)
- StellaWang309 (Yilin Wang)
- TTslmy (Huiying Liu)
- Au2789 (Bowen Pang)

