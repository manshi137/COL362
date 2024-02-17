INSERT INTO department (dept_id, dept_name)
values ('CSE', 'Computer Science');
INSERT INTO department (dept_id, dept_name)
values ('ECE', 'Electrical Engineering');
INSERT INTO department (dept_id, dept_name)
VALUES ('MT1', 'Mathematics1 and computing');

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

INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES('lassan', '2022CSE001', 'XXXXXXXX90', '2022CSE001@CSE.iitd.ac.in', 0, 'CSE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES('jhatu', '2022CSE002', 'XXXXXXXX91', '2022CSE002@CSE.iitd.ac.in', 0, 'CSE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES('lasan', '2022ECE001', 'XXXXXXXX92', '2022ECE001@ECE.iitd.ac.in', 0, 'ECE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('LODA', '2022ECE002', 'XXXXXXXX93', '2022ECE002@ECE.iitd.ac.in',  0, 'ECE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('lasan', '2022MT1001', 'XXXXXXXX94', '2022MT1001@MT1.iitd.ac.in', 0, 'MT1');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
values ('lasan', '2022MT1002', 'XXXXXXXX95', '2022MT1002@MT1.iitd.ac.in', 0, 'MT1');

INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES('lasan', '2023CSE001', 'XXXXXXXX80', '2023CSE001@CSE.iitd.ac.in', 0, 'CSE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES('jhatu', '2023CSE002', 'XXXXXXXX81', '2023CSE002@CSE.iitd.ac.in', 0, 'CSE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES('lasan', '2023ECE001', 'XXXXXXXX82', '2023ECE001@ECE.iitd.ac.in', 0, 'ECE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('LODA', '2023ECE002', 'XXXXXXXX83', '2023ECE002@ECE.iitd.ac.in',  0, 'ECE');
INSERT INTO student (first_name, student_id, contact_number, email_id, tot_credits, dept_id)
VALUES ('lasan', '2023MT1001', 'XXXXXXXX84', '2023MT1001@MT1.iitd.ac.in', 0, 'MT1');
INSERT INTO student (first_name, last_name,student_id, contact_number, email_id, tot_credits, dept_id)
values ('tatta', 'tst','2023MT1002', 'XXXXXXXX85', '2023MT1002@MT1.iitd.ac.in', 0, 'MT1');

-- create 3 courses 1 for each department
INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('CSE101', 'Computer Science', 4, 'CSE');
INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('ECE101', 'Intro Batti', 4, 'ECE');
INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('MT1101', 'Intro MnC', 4, 'MT1');

INSERT INTO course_offers (course_id, semester, session, professor_id, capacity, enrollments)
VALUES ('CSE101', 1, '2022-23', 'prof1', 50, 40);
INSERT INTO course_offers (course_id, semester, session, professor_id, capacity, enrollments)
VALUES ('ECE101', 1,  '2022-23', 'prof2', 50, 40);
INSERT INTO course_offers (course_id, semester, session, professor_id, capacity, enrollments)
VALUES ('MT1101', 1,  '2022-23', 'prof3', 45, 35);

INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022CSE001', 'CSE101', 1, '2022-23', 0);
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022CSE002', 'CSE101', 1, '2022-23', 7);
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022ECE001', 'ECE101', 1, '2022-23', 9);
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022ECE002', 'ECE101', 1, '2022-23', 6);
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022MT1001', 'MT1101', 1, '2022-23', 4);
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022MT1002', 'MT1101', 1, '2022-23', 5);

-- verifying the student semester view
select * from student_semester_summary;
UPDATE student_courses
SET grade = 9
WHERE student_id = '2022CSE002'
  AND course_id = 'CSE101'
  AND semester = 1
  AND session = '2022-23';

select * from student_semester_summary;

-- verifying the first year course >= 5 credit trigger
INSERT INTO courses (course_id, course_name, credits, dept_id)
VALUES ('CSE102', 'Cs2', 5, 'CSE');

INSERT INTO course_offers (course_id, semester, session, professor_id, capacity, enrollments)
VALUES ('CSE102', 1, '2022-23', 'prof1', 50, 49);
INSERT INTO course_offers (course_id, semester, session, professor_id, capacity, enrollments)
VALUES ('CSE102', 1, '2023-24', 'prof1', 50, 49);

INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022CSE001', 'CSE102', 1, '2022-23', 0);
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022CSE002', 'CSE102', 1, '2023-24', 7);

-- verifying drop student courses
SELECT * FROM student WHERE student_id = '2022CSE001';
SELECT * FROM course_offers WHERE course_id = 'CSE102' and session = '2022-23';
DELETE FROM student_courses WHERE student_id = '2022CSE001' and course_id = 'CSE102';
DELETE FROM student_courses WHERE student_id = '2022CSE001' and course_id = 'CSE102';
SELECT * FROM student_courses WHERE student_id = '2022CSE001' and course_id = 'CSE102';
SELECT * FROM student WHERE student_id = '2022CSE001';
SELECT * FROM course_offers WHERE course_id = 'CSE102' and session = '2022-23';

-- verifying capacity full trigger
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022CSE001', 'CSE102', 1, '2022-23', 0);
INSERT INTO student_courses (student_id, course_id, semester, session, grade)
VALUES ('2022ECE002', 'CSE102', 1, '2022-23', 7);