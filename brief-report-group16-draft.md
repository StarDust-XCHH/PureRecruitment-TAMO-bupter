  # Brief Report Draft - Group 16

## Project Overview

This document is a draft version of the brief report required for the first assessment of the Teaching Assistant Recruitment System project. It summarises the fact-finding techniques, iteration planning, prioritisation approach, and estimation method used by the team during the early stage of development. It also includes supporting materials that can later be moved into appendices when the document is exported to PDF.

The project goal is to develop a lightweight Java Servlet/JSP-based recruitment system for BUPT International School. The system is intended to improve the current manual workflow based on forms and spreadsheets by providing a clearer and more efficient process for Teaching Assistants (TAs), Module Organisers (MOs), and administrators.

## 1. Fact-Finding Techniques

To identify appropriate requirements for the project, the team used a combination of document analysis, scenario analysis, user-story brainstorming, prototype-driven exploration, and internal feedback review.

### 1.1 Document Analysis

The first source of requirements was the coursework handout, which defined the project scope at a high level. From this document, the team extracted the mandatory constraints and initial functional expectations, including:

- the need to build either a Java desktop application or a Java Servlet/JSP web application;
- the restriction that data must be stored in plain files rather than a database;
- the expectation that the system should support core recruitment actions such as profile creation, job posting, job application, applicant selection, and workload checking;
- the requirement to apply Agile methods in requirements discovery, prioritisation, iteration planning, implementation, and testing.

This helped the team distinguish between mandatory constraints, suggested features, and optional innovation opportunities such as AI-assisted recommendations.

### 1.2 Brainstorming from Multiple User Perspectives

The team then generated user stories from the perspectives of different stakeholders, especially TA, MO, and admin users. This was important because the handout only described the domain in an abstract way. The brainstorming activity converted the broad project theme into concrete functional goals.

Examples of early stories included:

- TA creates profile;
- TA uploads CV;
- TA views available jobs;
- TA applies for a job;
- TA checks application status;
- MO logs in;
- MO posts a job;
- MO views applicants;
- MO selects an applicant;
- Admin checks TA workload.

Each story was written with acceptance criteria, priority, estimation, and an intended sprint. This made the backlog usable for sprint planning rather than just as an idea list.

### 1.3 Scenario and Workflow Analysis

The team analysed the existing recruitment scenario described in the brief and decomposed it into a workflow:

1. a TA creates an account and profile;
2. a TA browses jobs and submits an application;
3. an MO creates and manages job postings;
4. an MO reviews applicants and makes decisions;
5. an admin monitors recruitment workload and records.

This workflow analysis helped the team identify dependencies between features. For example, a TA cannot apply before registration and profile setup exist, and an MO cannot review applicants before job-post and application data structures are in place.

### 1.4 Prototype-Driven Fact Finding

The team also used low- to medium-fidelity prototype design as a fact-finding technique. By sketching the main pages and flows for TA, MO, and admin roles, the team was able to expose missing requirements early.

For example, prototype exploration highlighted the need for:

- role-based entry points on the landing page;
- a usable navigation structure inside the TA workspace;
- job detail views before applying;
- status tracking after application submission;
- settings/profile maintenance rather than one-time data entry only.

Prototype discussion was especially useful because it forced abstract requirements to become visible interface decisions.

### 1.5 Internal Feedback and Evidence Review

The team reviewed existing notes, backlog drafts, and implemented screens to validate which stories were already realistic for the first iteration and which should be postponed. This included comparing user-story notes with the current repository structure and backlog evidence.

This review helped identify two categories of requirements:

- **core requirements**, which were essential for demonstrating the main recruitment workflow;
- **extended or innovative requirements**, such as recommendation logic, explainable matching, stronger security controls, and analytics dashboards.

As a result, the team kept the initial iteration focused on core end-to-end functionality and left advanced features for later sprints.

## 2. Iteration Planning

The project was planned using an incremental Agile approach. The team organised stories into iterations according to dependency order, business value, and implementation feasibility.

### 2.1 Planning Principles

The iteration plan followed four principles:

1. **Build the basic end-to-end workflow first** so the system can demonstrate meaningful value early.
2. **Implement foundational features before dependent ones** such as login, profile, and job data structures.
3. **Keep early iterations small enough to finish within the assessment timeline**.
4. **Leave enhancement and optimisation features for later iterations** after the core workflow is stable.

### 2.2 Sprint 1 Focus

Sprint 1 was planned around the minimum viable recruitment workflow and the first assessment deliverables. The main goal was to ensure that the project had a clear backlog, prototype evidence, and an initial working direction.

Sprint 1 mainly focused on:

- login and registration entry;
- TA profile creation and settings foundation;
- job browsing and job detail display;
- MO-side job management foundation;
- application status visibility;
- essential persistence using JSON/text files;
- prototype completion and backlog definition.

This sprint was designed to show that the product direction was coherent and implementable.

### 2.3 Sprint 2 Focus

Sprint 2 was planned to deepen the operational workflow after the foundation was established. Stories expected in this stage included:

- full application submission and persistence;
- applicant review and selection by MO;
- stronger session or security controls;
- reminders, matching logic, or availability features if time allowed.

This sprint would strengthen the real recruitment lifecycle instead of just the visual prototype.

### 2.4 Sprint 3 and Later Iterations

Later iterations were intended for value-added improvements and product maturity, such as:

- AI-assisted recommendation and explainable matching;
- workload balancing support;
- profile completeness scoring;
- analytics and dashboards;
- responsive optimisation and usability enhancements.

These features were considered useful but not essential for the first working version. Therefore, they were intentionally scheduled later.

### 2.5 Why This Iteration Plan Was Chosen

The team selected this plan because it reduced delivery risk. Instead of attempting every possible feature in parallel, the plan ensured that each iteration produced something testable and reviewable. This is consistent with Agile practice, where feedback can be used to refine the next sprint.

## 3. Prioritisation Method

The team used a practical priority model based on user value, workflow dependency, technical feasibility, and assessment relevance.

### 3.1 Priority Criteria

A story was treated as **High priority** if it met one or more of the following conditions:

- it was essential to the main recruitment workflow;
- it was a prerequisite for other stories;
- it was necessary to demonstrate a usable prototype or working software;
- it addressed a direct customer need mentioned in the brief.

A story was treated as **Medium priority** if it improved usability, visibility, or completeness but was not required for the system to function end to end.

A story was treated as **Low priority** if it mainly improved presentation, polish, or optional user experience without blocking the core workflow.

### 3.2 Core-First Prioritisation

The team explicitly followed a **core-first** strategy. The first priority was to make sure the system could support the main recruitment journey:

- create account/profile;
- view jobs;
- post jobs;
- submit or manage applications;
- review outcomes.

This is why stories such as registration, login, job browsing, job posting, and applicant handling were prioritised over advanced features like analytics or AI recommendations.

### 3.3 Balancing Innovation with Feasibility

The coursework encourages creativity, including AI-powered features. However, the team did not treat innovation ideas as immediate top priority unless they clearly supported the core workflow and could be implemented reliably. This prevented the project from becoming over-ambitious too early.

For example:

- explainable recommendations were recognised as valuable;
- security hardening features were useful;
- mobile optimisation was desirable;
- but all of these were considered secondary to making the recruitment flow work correctly.

### 3.4 Trade-Offs Made

A major trade-off was to prioritise breadth of core workflow over depth of advanced intelligence. In other words, the team preferred a simpler but coherent recruitment system rather than many partially completed experimental features. This trade-off was appropriate for the first assessment stage.

## 4. Estimation Method

The team used lightweight Agile estimation to assess relative effort rather than exact hours.

### 4.1 Story Point Thinking

Early stories in the brainstorming material used small numeric story points such as 2, 3, and 5. These values represented relative complexity, uncertainty, and workload.

For example:

- **2 points** indicated a small, well-understood story with limited risk;
- **3 points** indicated a moderate feature requiring several UI or logic steps;
- **5 points** indicated a larger story with more dependencies, validation, or persistence logic.

This method helped the team compare stories quickly without pretending that all work could be predicted precisely in hours.

### 4.2 T-Shirt Size Estimation

In more detailed backlog exploration, some stories were also described using sizes such as **S**, **M**, and **L**. These labels served the same purpose as story points but at a broader planning level.

A rough interpretation used by the team was:

- **S**: limited scope, few dependencies, straightforward implementation;
- **M**: moderate UI and backend coordination, some validation or state handling;
- **L**: larger features involving several components, file persistence, or more uncertainty.

### 4.3 Estimation Factors

The team estimated stories according to the following factors:

- number of pages, modules, or components involved;
- dependency on other stories;
- complexity of validation and state management;
- file persistence requirements;
- uncertainty in requirements or design;
- amount of front-end and back-end coordination needed.

For instance, “TA views available jobs” was smaller than “TA applies for a job” because the latter required more validation and data-writing behaviour. Similarly, “MO posts a job” or “MO selects an applicant” was larger than simple data display because it affected persistent state and downstream workflow.

### 4.4 Why Relative Estimation Was Suitable

Relative estimation was suitable because this project was still in a discovery-heavy stage. The team was refining requirements while building prototypes and early code. In such a context, relative estimation is more realistic than exact hourly prediction, and it is easier to revise after feedback from later iterations.

## 5. Supporting Materials

The following supporting materials are included as draft appendix content. These can later be reorganised or shortened when preparing the final PDF submission.

### 5.1 Example Interview / Discussion Questions

The team could use the following questions when discussing requirements with a client, TA users, or internal reviewers:

1. What information must a TA provide before applying for a position?
2. Should a TA be able to edit their profile after applying?
3. What criteria does an MO use when comparing applicants?
4. Should MOs be able to select multiple applicants for one job?
5. What application statuses are most useful to show to TAs?
6. How should the system indicate that a TA is overloaded?
7. What information about a job must be visible before a TA decides to apply?
8. Which actions should be restricted by role?
9. Are recommendation or matching features helpful, and how transparent should they be?
10. Which functions are essential for the first usable release?

### 5.2 Draft Summary of User Feedback from Prototype Review

Based on the current design direction, the most likely early feedback points were:

- users need a clear separation between TA and MO entry paths;
- TAs need a simple dashboard with profile, jobs, and status in one place;
- viewing job details before application is necessary;
- application progress should be visible after submission;
- settings and profile updates should remain editable, not one-time only;
- the interface should remain simple because the main value is workflow clarity rather than visual complexity.

### 5.3 Evidence Used in This Draft

This draft was informed by the following repository materials:

- coursework brief and assessment requirements;
- sprint task notes;
- initial user-story document;
- expanded backlog draft;
- project README and current repository structure.

### 5.4 Draft Conclusion

The team used multiple fact-finding techniques to move from a high-level project brief to a practical Agile backlog. Requirements were discovered and refined through document analysis, user-story brainstorming, workflow analysis, and prototype exploration. Iteration planning followed a core-first strategy so that the system could demonstrate meaningful value early, while prioritisation and estimation were kept lightweight and practical. This approach supports incremental delivery and leaves room for feedback-driven refinement in later sprints.
