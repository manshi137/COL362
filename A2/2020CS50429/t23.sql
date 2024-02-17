INSERT INTO department (dept_id, dept_name)
values ('CSE', 'Computer Science');
INSERT INTO department (dept_id, dept_name)
values ('ECE', 'batti wale');
INSERT INTO department (dept_id, dept_name)
VALUES ('MT1', 'abnormal ones');

INSERT INTO valid_entry (entry_year, dept_id, seq_number)
VALUES (2022, 'CSE', 1);
INSERT INTO valid_entry (entry_year, dept_id, seq_number)
VALUES (2022, 'ECE', 1);
INSERT INTO valid_entry (entry_year, dept_id, seq_number)
VALUES (2022, 'MT1', 1);
INSERT INTO valid_entry (entry_year, dept_id, seq_number)
VALUES (2023, 'CSE', 1);
INSERT INTO valid_entry (entry_year, dept_id, seq_number)
VALUES (2023, 'ECE', 1);
INSERT INTO valid_entry (entry_year, dept_id, seq_number)
VALUES (2023, 'MT1', 1);

INSERT INTO professor (professor_id, professor_first_name, professor_last_name, office_number, contact_number, start_year, resign_year, dept_id)
VALUES ('prof1', 'professor1', 'last1', 'office1', '1234567890', 2020, 2025, 'CSE');
INSERT INTO professor (professor_id, professor_first_name, professor_last_name, office_number, contact_number, start_year, resign_year, dept_id)
VALUES ('prof2', 'professor2', 'last2', 'office2', '1234567891', 2020, 2025, 'ECE');
INSERT INTO professor (professor_id, professor_first_name, professor_last_name, office_number, contact_number, start_year, resign_year, dept_id)
VALUES ('prof3', 'professor3', 'last3', 'office3', '1234567892', 2020, 2025, 'MT1');

INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('CSE101', 'Course1', 4, 'CSE');
INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('ECE101', 'Course2', 5, 'ECE');
INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('MT1101', 'Course3', 3, 'MT1');
INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('CSE102', 'Course4', 3, 'CSE');

INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('CSE101', '2023-2024', 1, 'prof1', 10, 0);
INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('ECE101', '2023-2024', 1, 'prof2', 10, 0);
INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('MT1101', '2023-2024', 1, 'prof3', 10, 0);

INSERT INTO student (first_name, last_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('lassan', 'kumar', '2023CSE001', 'XXXXXXXX90', '2023CSE001@CSE.iitd.ac.in', 0, 'CSE');
INSERT INTO student (first_name, last_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('jhandu', 'baam', '2023CSE002', 'XXXXXXXX91', '2023CSE002@CSE.iitd.ac.in', 0, 'CSE');
INSERT INTO student (first_name, last_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('chutad', 'singh', '2023ECE001', 'XXXXXXXX92', '2023ECE001@ECE.iitd.ac.in', 0, 'ECE');
INSERT INTO student (first_name, last_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('macchar ki jhat', '', '2023ECE002', 'XXXXXXXX93', '2023ECE002@ECE.iitd.ac.in', 0, 'ECE');

INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023CSE001', 'CSE101', '2023-2024', 1, 9);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023CSE002', 'CSE101', '2023-2024', 1, 8);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023ECE001', 'ECE101', '2023-2024', 1, 7);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023ECE002', 'ECE101', '2023-2024', 1, 6);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023CSE001', 'MT1101', '2023-2024', 1, 8);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023CSE002', 'MT1101', '2023-2024', 1, 7);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023ECE001', 'CSE101', '2023-2024', 1, 9);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023ECE002', 'CSE101', '2023-2024', 1, 6);
INSERT INTO student_courses (student_id, course_id, session, semester, grade)
VALUES('2023ECE002', 'CSE101', '2023-2024', 1, 6);

-- I want to drop the couse CSE101 for session 2023-2024 and semster 1 
-- but before that print all the students who are enrolled in this course
-- and also print theier credits

-- write your query here
SELECT student_id, tot_credits
FROM student
WHERE student_id IN (SELECT student_id FROM student_courses WHERE course_id = 'CSE101' AND session = '2023-2024' AND semester = 1)
ORDER BY student_id;
SELECT * FROM student_courses WHERE course_id = 'CSE101' AND session = '2023-2024' AND semester = 1 ;

DELETE FROM course_offers WHERE course_id = 'CSE101' AND session = '2023-2024' AND semester = 1;
SELECT student_id, tot_credits
FROM student
ORDER BY student_id;

SELECT * FROM student_courses WHERE course_id = 'CSE101' AND session = '2023-2024' AND semester = 1;

-- create 1 professor entry 
INSERT INTO professor (professor_id, professor_first_name, professor_last_name, office_number, contact_number, start_year, resign_year, dept_id)
VALUES ('prof4', 'professor4', 'last4', 'office4', '1234567893', 2020, 2025, 'CSE');

-- create 4 entries for this professor for courses_offered table with session 2023-2024 
INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('CSE102', '2023-2024', 1, 'prof4', 10, 0);
INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('ECE101', '2023-2024', 2, 'prof4', 10, 0);
INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('MT1101', '2023-2024', 2, 'prof4', 10, 0);
INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('CSE102', '2023-2024', 2, 'prof4', 10, 0);

INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('CSE101', '2023-2024', 2, 'prof4', 10, 0);

INSERT INTO course_offers (course_id, session, semester, professor_id, capacity, enrollments)
VALUES ('CSE101', '2025-2026', 1, 'prof2', 10, 0);