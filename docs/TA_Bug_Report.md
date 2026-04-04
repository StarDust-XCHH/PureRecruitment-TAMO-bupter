# TA Module Bug Report

## Overview
This document records bugs and improvement suggestions found during testing of the TA module.

## Passed Functions
- Registration basic flow works correctly
- Duplicate username with leading spaces is handled
- Login works correctly
- Profile editing works correctly

## Bug 1: Registration allows spaced numeric username as new account
**Module:** Registration

**Description:**  
When spaces are inserted within numeric usernames, the system treats them as different accounts.


**Examples:**  
- `2023213143`
- `2 023213143`
- `2 0 23213143`

**Steps to Reproduce:**  
1. Register an account using `2023213143`
2. Register another account using `2 023213143`

**Expected Result:**  
The system should normalize the username and identify both as the same account.

**Actual Result:**  
The system treats them as different usernames and allows duplicate registration.

**Impact:**  
This may allow duplicate TA accounts and cause inconsistent user data.

**Severity:** High

---

## Bug 2: Dark mode option text not visible
**Module:** Personal Information Settings

**Description:**  
In dark mode, option contents are invisible unless the mouse hovers over them.

**Expected Result:**  
All option text should remain visible in dark mode.

**Actual Result:**  
Users cannot clearly see option text without hovering.

**Impact:**  
This reduces usability and may confuse users when navigating settings.

**Severity:** Medium

---

## Bug 3: Resume checklist links cannot jump correctly in the same page
**Module:** Resume Completion Checklist

**Description:**  
The new links cannot navigate correctly within the same page. All three links fail.

**Expected Result:**  
Clicking each link should jump to the corresponding section.

**Actual Result:**  
The page does not navigate correctly.

**Severity:** Medium

---

## Bug 4: Resume checklist links open a new page but still show initial page
**Module:** Resume Completion Checklist

**Description:**  
When opening in a new page, the link still leads to the initial page and does not indicate where the user should modify content.

**Expected Result:**  
The new page should open at the relevant target section.

**Actual Result:**  
The user still lands on the initial page without guidance.

**Severity:** Medium

---

## Improvement Suggestions (Validation & UX Enhancements)

### 1. Duplicate email and phone check
The registration page should also prevent duplicate email addresses and phone numbers.

### 2. Phone number validation
The phone number should be validated as an 11-digit numeric string.

### 3. Password validation
A password containing only spaces, such as six consecutive spaces, should not pass validation.

### 4. Application intention validation
The system could validate whether the application intention field contains meaningful input instead of allowing pure numbers.
