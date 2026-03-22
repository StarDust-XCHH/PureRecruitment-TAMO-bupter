### EBU6304 Software Engineering Group Project

2025-26

| Group number | 16 |  |
| --- | --- | --- |
| member 1 | QM no: 231220725 | Name: Yuhan Guan |
| member 2 | QM no: 231221216 | Name: Fenghao Zhang |
| member 3 | QM no: 231222556 | Name: Yufeng Li |
| member 4 | QM no: 231222305 | Name: Yilin Wang |
| member 5 | QM no: 231221788 | Name: Huiying Liu |
| member 6 | QM no: 231220998 | Name: Bowen Pang |

### First report

---

## 1. Requirements

This report explains how Group 16 converted the handout's high-level requirements into a practical Agile plan for a lightweight Java Servlet/JSP recruitment system covering three roles (TA, MO, and Admin). In line with First Assessment requirements, Section 1 focuses on fact-finding techniques, iteration planning, prioritisation, and estimation, while supporting evidence and materials are provided in Section 2.

> Page-limit note: Section 1 is the main report body (maximum 5 pages).

---

### 1.1 Requirement Elicitation

To avoid assumption-driven design, we used multiple requirement-finding methods and cross-checked findings across sources.

#### 1.1.1 Handout-Based Requirement Extraction

From the handout, we extracted mandatory technical constraints (Java-based solution with Servlet/JSP accepted; text-format data storage only in JSON/CSV/XML/TXT, with no database), core functional direction (TA profile/CV/job/application/status, MO posting and selection, and Admin workload checking), and process expectations (Agile iterations, prioritised delivery, and documented decisions/trade-offs).

This formed the baseline boundary for scope control.

#### 1.1.2 Stakeholder Workshop Clarification

We used the official story-writing workshop as direct fact-finding with end users.

In this workshop, the team and stakeholders clarified that the product should be a **recruitment system** (not a timetable or temporary-task system), recruitment should target semester-scale TA hiring, MO job posts should include required skills, a major current pain point is that **TAs cannot track MO review progress**, and the Admin role should have the highest access permission.

These findings directly influenced our scope and backlog ordering.

#### 1.1.3 Forum Clarification of Requirement Conflict

We identified a requirement conflict between expected user behavior (CVs are usually PDF) and the technical constraint of text-file storage.

Based on the forum exchange, the team adopted a clear decision rule: propose and agree a practical solution with stakeholders, use a simple implementation approach, and when needed store PDF files in a folder while recording relative paths in text-based metadata (e.g., JSON).

This clarification became a concrete architectural decision rule.

#### 1.1.4 User Story and Workflow Decomposition

We translated requirement findings into structured user stories and mapped dependencies across a full workflow chain: role entry/account, TA profile and job browsing, application and status tracking, MO posting and applicant review, selection feedback to TA, and admin oversight.

This helped avoid implementing isolated UI without process continuity.

#### 1.1.5 Prototype-Led Discovery

By building low/medium-fidelity prototypes, we discovered additional requirements that text-only stories did not capture clearly, including TA/MO entry separation, unified workspace navigation, job detail visibility before application, editable profile/settings, and timeline-style status visibility.

#### 1.1.6 Impact on Scope and Planning

These findings changed implementation and planning priorities in concrete ways: the workshop pain point ("cannot track MO review progress") raised the priority of status timeline and update flow, scope clarification prevented timetable-centric design drift, CV conflict clarification informed a practical architecture decision (file storage + text metadata path) consistent with handout constraints, and admin high-permission clarification shaped role-boundary planning for later sprints.

#### 1.1.7 Requirement Quality Criteria

To improve requirement quality before implementation, we applied four checks to each high-priority story: **clarity** (single interpretation, explicit actor/action/outcome), **testability** (observable acceptance conditions), **feasibility** (compatible with Servlet/JSP and text-file persistence constraints), and **traceability** (clear mapping from requirement to backlog item and planned sprint). This prevented ambiguous scope expansion and made early acceptance discussion more efficient.

---

### 1.2 Iteration Planning

#### 1.2.1 Planning Principles

Iteration planning followed four principles: deliver the core workflow first, build dependencies before downstream stories, align with assessment milestones, and postpone high-uncertainty enhancements.

#### 1.2.2 Sprint Structure

Based on the handout timeline and backlog, Sprint 1 focused on first-assessment deliverables (backlog with acceptance criteria, low/medium-fidelity prototype, role entry plus TA workspace skeleton, JSON persistence foundation, and this brief report); Sprint 2 focused on completing TA application flow, MO posting/review actions, decision write-back to status, and stronger data/session consistency; Sprint 3 targeted admin workload/record views and reliability/testing expansion; Sprint 4 reserved explainable AI-oriented enhancements (matching, missing skills, workload balancing).

#### 1.2.3 Planning Rationale

This plan reduces delivery risk and supports incremental demonstration by producing early visible stakeholder value, enabling faster feedback incorporation, and controlling technical complexity under coursework constraints.

#### 1.2.4 Iteration-1 Boundary (What Is In / Out)

To keep Iteration 1 deliverable-focused, we defined explicit boundaries. **In scope:** coherent role entry, TA core workspace skeleton (profile/jobs/status), backlog completeness with acceptance criteria, and a medium-fidelity prototype covering major user paths. **Out of scope for Iteration 1:** deep analytics, full admin governance automation, and advanced AI assistance workflows. This boundary control ensures a complete and usable baseline rather than fragmented partial features.

#### 1.2.5 Dependency-First Delivery Logic

The delivery sequence follows dependency direction: account/role access before route-level functions, route-level functions before cross-role status synchronization, and synchronization before advanced optimization features. This ordering reduces rework because downstream flows (e.g., MO review write-back, TA status visibility) rely on upstream data contracts being stable first.

---

### 1.3 Prioritisation

#### 1.3.1 Prioritisation Criteria

We used a practical priority model combining user value/pain-point impact, dependency criticality, implementation feasibility under sprint deadlines, and assessment relevance.

#### 1.3.2 Priority Rules

Priority rules were: **High** for core-workflow blockers or direct pain-point features, **Medium** for usability/completeness improvements, and **Low** for advanced intelligence or non-blocking enhancements.

#### 1.3.3 Core-First Outcome

Stories such as login, profile, job browse, status tracking, and MO posting/review were prioritised before AI extensions.  
This directly reflects the identified user pain-point that status traceability must be visible and reliable.

#### 1.3.4 Trade-Off Statement

We intentionally chose a coherent end-to-end recruitment baseline instead of many partially implemented advanced features.

This trade-off is consistent with Agile incremental delivery and first-assessment objectives.

#### 1.3.5 Prioritisation Outcome by Role

The practical outcome of prioritisation is role-balanced but core-first:

- **TA:** profile completion, job browsing, application submission path, and status visibility are treated as first-line value.
- **MO:** posting and screening are prioritised where they directly affect TA outcomes.
- **Admin:** governance visibility is prioritised before deeper automation.

This ensures each role has demonstrable value in the first release while preserving cross-role process continuity.

---

### 1.4 Estimation

#### 1.4.1 Relative Story-Point Estimation

We used lightweight story points (2/3/5/8) in early stories: 2 for small/low-uncertainty work, 3 for moderate features with multi-step logic, 5 for larger features with persistence/dependencies, and 8 for high-uncertainty cross-module effort.

#### 1.4.2 T-Shirt Sizing

In expanded backlog planning, we also used T-shirt sizing: S for isolated straightforward work, M for moderate front/back coordination, and L for cross-module or schema-impacting features.

#### 1.4.3 Estimation Factors

Estimation considered affected modules/routes, validation and state complexity, persistence impact (including JSON schema changes), front-end/back-end integration effort, and uncertainty/test risk.

#### 1.4.4 Method Suitability

Because this stage is discovery-heavy, relative estimation is more realistic than hour-based fixed prediction.

#### 1.4.5 Estimation-to-Planning Conversion

Estimation values were converted into sprint planning through a simple workload-balancing rule: each sprint combines a small number of high-complexity items with multiple medium/small items, while reserving contingency for integration and bug fixing. This avoids overloading early iterations and improves delivery predictability under uncertainty.

#### 1.4.6 Uncertainty and Risk Handling

For high-uncertainty stories, we used three safeguards: split stories into smaller verifiable steps, validate interface/data assumptions early, and defer non-critical variants to later iterations. This keeps risk visible and prevents one uncertain feature from blocking the full iteration objective.

---

### 1.5 Conclusion

Group 16 used a structured Agile approach to transform abstract handout requirements into a practical backlog and prototype baseline.  
The team combined handout analysis, stakeholder clarification, conflict resolution, user-story decomposition, and prototype validation.

This process produced clearer scope boundaries, justified iteration ordering, transparent prioritisation decisions, realistic estimation under uncertainty, and documented traceable design decisions.

The current direction provides a solid foundation for Sprint 2+ implementation while keeping the design simple, modular, and extensible as required.

### 1.6 Alignment with First-Assessment Marking Focus

The requirements and planning approach in Section 1 is designed to align with key first-assessment expectations:

- **Functionality orientation:** core recruitment flow is defined end-to-end across TA, MO, and Admin.
- **Usability orientation:** requirements prioritize visible status tracking, clear role separation, and coherent navigation paths.
- **Report quality orientation:** fact-finding, iteration planning, prioritisation, and estimation methods are explicitly described with clear decision rationale.
- **Evidence readiness:** while evidence details are outside the 5-page body, each major claim in Section 1 is structured for traceability to supporting materials in Section 2.

---

## 2. Supporting material

Supporting materials in this section are outside the main 5-page body.

### 2.1 Example Requirement Interview Questions

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

### 2.2 Early Feedback Summary

From workshop/prototype review:

- role separation must be obvious
- TA needs one coherent workspace (profile/jobs/status)
- status tracking is a key pain-point and must be visible
- job details are required before application
- settings should remain editable
- solution should remain simple and modular

### 2.3 Factual Evidence Log (Time/Place/People/Event)

| No. | Time | Place / Channel      | People | Event | Key Output |
|---|---|----------------------|---|---|---|
| 1 | 13 Mar 2026, 08:50-09:35 (Beijing) | TB3-435 workshop     | Yuhan Guan, Fenghao Zhang, Dr. Gokop Goteng, 2 TAs | Story-writing workshop | Confirmed "recruitment system" scope, status-tracking pain point, and admin high-access role |
| 2 | 13 Mar 2026, 10:27 PM (Beijing) | QMPlus Student Forum | Fenghao Zhang (for Group 16) | Posted CV PDF vs text-storage conflict question | Raised requirement contradiction for stakeholder decision |
| 3 | 13 Mar 2026, 11:29 PM (Beijing) | QMPlus Student Forum | Dr. Ling Ma | Stakeholder reply to conflict | Accepted simple solution: keep PDF files and store relative paths in text-based records |

Reference links for entries 2 and 3:

- Forum post: [https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096494](https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096494)
- Forum reply: [https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096513](https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096513)

### 2.4 Evidence Traceability Matrix

| Report Claim                                                   | Evidence                                                               | Location                                               |
| -------------------------------------------------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------ |
| We used stakeholder workshop as fact-finding                   | Story-writing workshop record with participants and clarified scope    | `projectFile/Fenghao/Evidence.md`                      |
| Status tracking is a key user pain-point                       | Workshop note explicitly states inability to track MO review progress  | `projectFile/Fenghao/Evidence.md`                      |
| CV storage conflict was handled via stakeholder clarification  | Forum thread + lecturer response on PDF storage with text-path linkage | `projectFile/Fenghao/Evidence.md`; [post](https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096494); [reply](https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096513) |
| Core requirements and constraints come from handout            | Servlet/JSP option, no database, core functions, Agile process         | `projectFile/EBU6304_GroupProjectHandout.txt`          |
| Stories include acceptance criteria/priority/estimation/sprint | Structured story set                                                   | `projectFile/Yilin/user_stories.md`                    |
| Backlog and prototype are evidence-backed                      | Expanded backlog and image evidence                                    | `projectFile/StarDustXCHH/Product_Backlog_Stardust.md` |
| Team planning and architecture context are documented          | repository overview and structure                                      | `README.md`                                            |

---

