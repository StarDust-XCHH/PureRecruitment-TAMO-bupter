1.2026年3月13日8:50-9:35（am,Beijing time）在TB3-435举行的，原文如下
The Friday tutorial session will be used as the group coursework STORY WRITING WORKSHOP. Lecturers and Teaching Assistants will be present as End Users to help you understand the requirements accurately and provide feedback.
参与者：16小组成员代表：组长Yuhan Guan，组员Fenghao Zhang，lecturer Dr.Gokop Goteng, 2 TAs
我们在其中重新明确了任务需求是创建一套纯招聘系统，而不是我们之前预想的timetable/课程表/临时工作招聘，是长期（整个学期）工作，不按照时间和任务量招聘，MO应发布Module时明确所需要的skills，以便用户投递简历，开发应以用户需求和痛点作为抓手，现场TA明确表示现有的流程无法追踪MO审核进度，这是最大的痛点。另外，我们还在workshop中明确了admin具有的最高访问权限。

2.我们通过学生论坛对CV upload提出了疑问，在workshop中TA和Dr.Gokop Goteng已表示CV被期望是PDF格式，其与handout中的纯文本存储冲突，原文如下：

Clarification needed: Handling PDF CVs under Text File Storage Constraint
by Fenghao Zhang - Friday, 13 March 2026, 10:27 PM (BJ time)
https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096494

Dear Dr. Ling Ma and all,

During today's (13 March) Story Writing Workshop, our team discussed the "Upload CV" user story and encountered a conflict between functional expectations and the technical constraints outlined in the handout. We would like to seek official clarification on this matter.

The Issue:
In a real-world context, TAs typically submit their CVs as PDF or DOC files. However, Section 2.2 of the handout strictly mandates that

"All input and output data should be stored in simple text file formats… Do not use a database."

Dr. Gokop and the TA present at the workshop confirmed that in practice, CVs are predominantly PDF files, with a minority in .doc format. However, they could not definitively advise on how to reconcile this with the "text file storage only" rule.

Our Questions:
How should we handle the storage and processing of CV files under these constraints?

1. Are we allowed to store the original PDF files directly within the project directory?

2. If so, how should we link this data?

· Should we use a text data document (e.g., JSON) to store the relative path linking to the PDF file, so that MO(s) can view the original, formatted CV?

· Or, are we expected to extract and read the text information from the PDF and store it as plain text? We are concerned that this approach might result in the loss of essential formatting and structure.

We are unsure whether the text-file constraint applies strictly to the content storage or if it prohibits storing non-plain-text files (like PDFs, images) entirely. Your guidance on the acceptable approach would be greatly appreciated.

Best regards,

Fenghao Zhang,

On behalf of Group 16

MO Dr. Ling Ma做出回复，如下：
by Ling Ma - Friday, 13 March 2026, 11:29 PM (BJ time)
https://qmplus.qmul.ac.uk/mod/forum/discuss.php?d=659577#p1096513

hi Fenghao,

When there is a conflict in requirements, the team should propose possible solutions and discuss them with stakeholders until an agreement is reached. In this case, your team needs to decide on an appropriate approach.

If you choose to store PDF files, they can be saved in a folder, and a text-based file can store the relative path of each file. Alternatively, you may choose not to use PDFs, provided stakeholders can easily access the information they need.

Keep the solution as simple as possible.

Ling

3.