select * from department;
insert into department values('CSE', 'cse1');
insert into department values('EEE', 'eee1');
insert into department values('abc', 'abc1');
select * from department;
select * from valid_entry;

insert into valid_entry values('CSE', 2020, 1);
insert into valid_entry values('EEE', 2020, 1);
insert into valid_entry values('abc', 2020, 1);

insert into valid_entry values('CSE', 2021, 1);
insert into valid_entry values('EEE', 2021, 1);
insert into valid_entry values('abc', 2021, 1);

-- update valid_entry set seq_number = 2 where dept_id = 'CSE' and entry_year = 2020;

insert into student values('A', 'B', '2020CSE001', 'xyz', '0000000001', '2020CSE001@CSE.iitd.ac.in', 0, 'CSE');
insert into student values('C', 'D', '2020CSE002', 'xyz', '0000000002', '2020CSE002@CSE.iitd.ac.in', 0, 'CSE');
insert into student values('A', 'B', '2020EEE001', 'xyz', '0000000004', '2020EEE001@EEE.iitd.ac.in', 0, 'EEE');
insert into student values('C', 'D', '2020EEE002', 'xyz', '0000000006', '2020EEE002@EEE.iitd.ac.in', 0, 'EEE');
insert into student values('A', 'B', '2020abc001', 'xyz', '0000000008', '2020abc001@abc.iitd.ac.in', 0, 'abc');

-- delete from student where student_id = '2020abc001';
-- insert into student values('A', 'B', '2020abc002', 'xyz', '0000000008', '2020abc002@abc.iitd.ac.in', 0, 'abc');
-- insert into student values('E', 'F', '2020aaa001', 'xyz', '0000000003', '2020aaa001@aaa.iitd.ac.in', 0, 'aaa');
-- insert into student values('E', 'F', '2020CSE003', 'xyz', '0000000003', '2020CSE003@CSE.iitd.ac.in', 0, 'abc');
-- insert into student values('E', 'F', '2020CSE003', 'xyz', '0000000003', '2020CSE003@CSE.iitd.ac.in', 0, 'aaa');
-- insert into student values('A', 'B', '2020aaa001', 'xyz', '0000000005', '2020aaa001@aaa.iitd.ac.in', 0, 'abc');
-- insert into student values('A', 'B', '2021aaa002', 'xyz', '0000000008', '2021aaa002@aaa.iitd.ac.in', 0, 'aaa');
select * from valid_entry;
select * from student;


insert into courses values('CSE101', 'Intro to CSE1', 'Intro to CSE1', 4, 'CSE');
insert into courses values('CSE102', 'Intro to CSE2', 'Intro to CSE2', 4, 'CSE');
insert into courses values('CSE103', 'Intro to CSE3', 'Intro to CSE3', 4, 'CSE');
insert into courses values('CSE104', 'Intro to CSE4', 'Intro to CSE4', 4, 'CSE');
insert into courses values('CSE105', 'Intro to CSE5', 'Intro to CSE5', 4, 'CSE');

insert into courses values('CSE106', 'Intro to CSE6', 'Intro to CSE6', 18, 'CSE');
insert into courses values('CSE107', 'Intro to CSE7', 'Intro to CSE7', 5, 'CSE');

insert into professor values('prof1', 'A', 'B', 'xyz', '0000000001', 2020, 2040, 'CSE');
insert into professor values('prof2', 'C', 'D', 'xyz', '0000000002', 2020, 2040, 'CSE');
insert into professor values('prof3', 'E', 'F', 'xyz', '0000000003', 2020, 2040, 'CSE');


insert into course_offers values('CSE101', '2020-2021', 1, 'prof1', 100, 0);
insert into course_offers values('CSE102', '2020-2021', 1, 'prof2', 100, 0);
insert into course_offers values('CSE103', '2020-2021', 1, 'prof3', 100, 0);
insert into course_offers values('CSE104', '2020-2021', 1, 'prof1', 100, 0);
insert into course_offers values('CSE105', '2020-2021', 1, 'prof2', 100, 0);
insert into course_offers values('CSE106', '2020-2021', 1, 'prof3', 100, 0);
-- select * from course_offers;

insert into student_courses values('2020CSE001', 'CSE101', '2020-2021', 1, 10);
insert into student_courses values('2020CSE001', 'CSE102', '2020-2021', 1, 8);
insert into student_courses values('2020CSE001', 'CSE103', '2020-2021', 1, 9);
insert into student_courses values('2020CSE001', 'CSE104', '2020-2021', 1, 10);

insert into student_courses values('2020CSE002', 'CSE101', '2020-2021', 1, 8);
insert into student_courses values('2020CSE002', 'CSE102', '2020-2021', 1, 8);
update student_courses set grade = 9 where student_id = '2020CSE002' and course_id = 'CSE102' and session = '2020-2021' and semester = 1;
delete from student_courses where student_id = '2020CSE002' and course_id = 'CSE101' and session = '2020-2021' and semester = 1;


-- insert into student_courses values('2020CSE001', 'CSE106', '2020-2021', 1, 10);

-- insert into student_courses values('2020CSE001', 'CSE105', '2020-2021', 1, 8);
-- insert into student_courses values('2020CSE001', 'CSE107', '2020-2021', 1, 10);

-- update courses set credits = 50 where course_id = 'CSE107';
-- insert into student_courses values('2020CSE001', 'CSE107', '2020-2021', 1, 10);

-- update course_offers set capacity = 2 where course_id = 'CSE101' and session = '2020-2021' and semester = 1;
-- insert into student_courses values('2020EEE002', 'CSE101', '2020-2021', 1, 10);

-- select * from student_courses;
-- select * from course_eval;
-- select * from student_semester_summary;

insert into valid_entry values('CSE', 2022, 1);
insert into valid_entry values('EEE', 2022, 1);

insert into student values('A', 'B', '2022CSE001', 'xyz', '0000000007', '2022CSE001@CSE.iitd.ac.in', 0, 'CSE');
insert into student values('C', 'D', '2022EEE001', 'xyz', '0000000009', '2022EEE001@EEE.iitd.ac.in', 0, 'EEE');

insert into professor values('prof4', 'A', 'B', 'xyz', '0000000004', 2020, 2040, 'CSE');
insert into professor values('prof5', 'C', 'D', 'xyz', '0000000005', 2020, 2040, 'EEE');

insert into course_offers values('CSE101', '2022-2023', 1, 'prof4', 100, 0);
insert into course_offers values('CSE102', '2022-2023', 1, 'prof5', 100, 0);

insert into student_courses values('2022CSE001', 'CSE101', '2022-2023', 1, 10);
insert into student_courses values('2022EEE001', 'CSE101', '2022-2023', 1, 10);
insert into student_courses values('2022EEE001', 'CSE102', '2022-2023', 1, 9);

delete from course_offers where course_id = 'CSE104' and session = '2020-2021' and semester = 1;
delete from course_offers where course_id = 'CSE102';
select * from course_offers;
select * from student_courses;
select * from student;

insert into courses values('CSE110', 'Intro to CSE10', 'Intro to CSE10', 4, 'CSE');
insert into courses values('CSE111', 'Intro to CSE11', 'Intro to CSE11', 4, 'CSE');
insert into courses values('CSE112', 'Intro to CSE12', 'Intro to CSE12', 4, 'CSE');
insert into courses values('CSE113', 'Intro to CSE13', 'Intro to CSE13', 4, 'CSE');

insert into course_offers values('CSE110', '2020-2021', 1, 'prof1', 100, 0);
insert into course_offers values('CSE111', '2020-2021', 1, 'prof1', 100, 0);
-- insert into course_offers values('CSE112', '2020-2021', 1, 'prof1', 100, 0);

-- insert into course_offers values('CSE113', '2020-2021', 1, 'prof1', 100, 0);
-- select * from course_offers;
update department set dept_id = 'ece' where dept_id = 'EEE';

-- delete from department where dept_id = 'ece';

insert into department values('ccc', 'ccc1');
select * from department;
delete from department where dept_id = 'ccc';

-- insert into courses values('CSE114', 'Intro to CSE14', 'Intro to CSE14', 5, 'CSE');
-- insert into course_offers values('CSE114', '2021-2022', 1, 'prof3', 100, 0);
-- insert into student_courses values('2020CSE001', 'CSE114', '2021-2022', 1, 10);


-- select * from valid_entry;
-- select * from student_semester_summary;
-- -- update student set dept_id = 'CSE' where student_id = '2022EEE001';