-- 1.1.1
-- 1.1.2
-- table 9
-- drop table if exists department;
create table if not exists department(
    dept_id CHAR(3) PRIMARY KEY NOT NULL UNIQUE,
    dept_name VARCHAR(40)
);
-- table 3
-- drop table if exists student;
create table if not exists student(
    first_name VARCHAR(40) NOT NULL,
    last_name VARCHAR(40),
    student_id CHAR(11) PRIMARY KEY NOT NULL , 
    address VARCHAR(100),
    contact_number CHAR(10) NOT NULL UNIQUE,
    email_id VARCHAR(50) UNIQUE,
    tot_credits INTEGER NOT NULL,
    dept_id CHAR(3),
    CONSTRAINT student_ck CHECK (tot_credits >= 0)
);
ALTER TABLE student ADD CONSTRAINT fk_dept_id FOREIGN KEY (dept_id)
REFERENCES department(dept_id)
-- DEFERRABLE INITIALLY DEFERRED
ON UPDATE CASCADE;
-- table 4
-- drop table if exists courses;
-- with tmp as 
CREATE or replace FUNCTION check_dept_exists(course_id CHAR(6)) RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (SELECT 1 FROM department WHERE dept_id = SUBSTRING(course_id FROM 1 FOR 3));
END;
$$ LANGUAGE plpgsql;

create table if not exists courses(
    course_id CHAR(6) PRIMARY KEY NOT NULL ,
    course_name VARCHAR(20) NOT NULL UNIQUE,
    course_desc TEXT,
    credits NUMERIC NOT NULL,
    dept_id CHAR(4) REFERENCES department(dept_id) ON UPDATE CASCADE,
    CONSTRAINT courses_ck CHECK (credits > 0),
    CONSTRAINT courses_ck1
    CHECK (
        LENGTH(course_id) = 6 
        AND check_dept_exists(course_id)
        AND SUBSTRING(course_id FROM 4 FOR 3) ~ '^\d{3}'
    )
);
-- table 7
-- drop table if exists professor;
create table if not exists professor(
    professor_id VARCHAR(10) PRIMARY KEY,
    professor_first_name VARCHAR(40) NOT NULL,
    professor_last_name VARCHAR(40) NOT NULL,
    office_number VARCHAR(20),
    contact_number CHAR(10) NOT NULL,
    start_year INTEGER,
    resign_year INTEGER,
    dept_id CHAR(3) REFERENCES department(dept_id) ON UPDATE CASCADE,
    CONSTRAINT professor_ck CHECK (start_year <= resign_year)
);

-- table 6
-- drop table if exists course_offers;
create table if not exists course_offers(
    course_id CHAR(6) REFERENCES courses(course_id),
    session VARCHAR(9),
    semester INTEGER NOT NULL,
    professor_id VARCHAR(10) REFERENCES professor(professor_id),
    capacity INTEGER,
    enrollments INTEGER,
    PRIMARY KEY(course_id, session, semester),
    CONSTRAINT course_offers_ck CHECK (semester = 1 OR semester = 2)
);
-- table 5
-- drop table if exists student_courses;
create table if not exists student_courses(
    student_id CHAR(11) ,
    course_id CHAR(6) ,
    session VARCHAR(9) ,
    semester INTEGER,
    grade NUMERIC NOT NULL,
    FOREIGN KEY(course_id, session, semester) REFERENCES course_offers(course_id, session, semester),
    CONSTRAINT student_courses_ck CHECK (grade >= 0 AND grade <= 10)
);

alter table student_courses add constraint fk_student_id FOREIGN KEY (student_id) REFERENCES student(student_id) ON UPDATE CASCADE;
-- table 8
drop table if exists valid_entry;
create table if not exists valid_entry(
    dept_id CHAR(3) REFERENCES department(dept_id) ON UPDATE CASCADE,
    entry_year INTEGER NOT NULL,
    seq_number INTEGER NOT NULL default 1
);

-- 2
-- 2.1
-- Create a trigger with the name of validate student id to validate the student id. 
-- If the entry number assigned to a student is not valid, then raise an "invalid" message; 
-- else, successfully insert the tuple in the table.
-- 2.1.1.
-- When a new student is registered, a unique student id is assigned to each student. 
-- A student id is a 10-digit unique code, with the first four digits being entry year, 
-- the next three characters are dept id, and the last three digits are seq number. 
-- When a new student is registered, your schema must validate this entry number with the below conditions:
-- • The entry year and dept id in student id should be a valid entry in valid entry table.
-- • The sequence number should start from 001 for each department (maintained in valid entry table).
-- Thus, the current sequence number is assigned when a new student is registered in a department.
CREATE OR REPLACE FUNCTION validate_student_id()
RETURNS TRIGGER AS $$
DECLARE
    current_seq_number INTEGER;
BEGIN
    SELECT seq_number INTO current_seq_number
    FROM valid_entry WHERE entry_year = CAST(SUBSTRING(new.student_id FROM 1 FOR 4) AS INTEGER) AND dept_id = NEW.dept_id;

    IF NEW.student_id ~ '^\d{4}[a-zA-Z]{3}\d{3}' THEN
        IF EXISTS (SELECT * FROM valid_entry 
        WHERE 
        dept_id = SUBSTRING(NEW.student_id FROM 5 FOR 3) 
        AND 
        entry_year = CAST(SUBSTRING(new.student_id FROM 1 FOR 4) AS INTEGER)
        )
        and substring(new.student_id from 5 for 3) = new.dept_id
        and current_seq_number = CAST(SUBSTRING(new.student_id FROM 8 FOR 3) AS INTEGER)
        THEN
            RETURN NEW;
        ELSE
            RAISE EXCEPTION 'invalid1';
        END IF;
    ELSE
        RAISE EXCEPTION 'invalid2';
    END IF;
END;
$$ language plpgsql;


create or replace trigger validate_student_id
before insert on student
for each row
execute function validate_student_id();


-- 2.1.2. If the above student id is a valid id, you add that student detail in the student table.
-- but do not forget to increase the counter, i.e., seq number in valid entry table after each insert in the student table. 
-- Thus, create a trigger with the name, update seq number, which will update the seq number in valid entry table.

CREATE OR REPLACE FUNCTION update_seq_number() RETURNS TRIGGER AS $$
BEGIN
    UPDATE valid_entry    
    SET seq_number = seq_number + 1 
    WHERE entry_year = CAST(SUBSTRING(new.student_id FROM 1 FOR 4) AS INTEGER) AND dept_id = NEW.dept_id;
    RETURN NEW;
END;
$$ language plpgsql;


CREATE OR REPLACE TRIGGER  update_seq_number
AFTER INSERT ON student
FOR EACH ROW
EXECUTE FUNCTION update_seq_number();

-- 2.1.3. Assume that before we perform an insert operation on the student table,we need to verify 
-- if the student’s email id is correct or not. 
-- A correct email id will be of the form ’YYYYABC123@ABC.iitd.ac.in’, i.e., 
-- it has two parts, one part before @ and other after it. 
-- The part before @ should match the student id for example, ’YYYYABC123’, where the first four digits being entry year, the next three characters are department ID ( dept id ), and the last three digits are sequence number (seq number). 
-- The second half of the email (after the ’@’) should start with the department ID of the student (ABC in this case). 
-- This should match the department ID, i.e., the three characters in the student id column of the student 
-- as well as the department ID in the dept id column and end with ’.iitd.ac.in’. 
-- Validate if the student’s email is correct or not. 
-- If the email is valid, continue with the insertion; otherwise, raise an "invalid" message.

CREATE OR REPLACE FUNCTION validate_email_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email_id ~ '^\d{4}[a-zA-Z]{3}\d{3}@[a-zA-Z]{3}\.iitd\.ac\.in$' THEN
        IF SUBSTRING(NEW.email_id FROM 1 FOR 10) = NEW.student_id 
        AND SUBSTRING(NEW.email_id FROM 12 FOR 3) = NEW.dept_id 
        and NEW.dept_id = SUBSTRING(NEW.student_id FROM 5 FOR 3)
        THEN
            RETURN NEW;
        ELSE
            RAISE EXCEPTION 'invalid';
        END IF;
    ELSE
        RAISE EXCEPTION 'invalid';
    END IF;
END;
$$ language plpgsql;

create or replace trigger validate_email_id
BEFORE INSERT ON student
FOR EACH ROW
EXECUTE FUNCTION validate_email_id();



-- 2.1.4. (To allow or to not allow change of branch) 
-- The Institute management also wants to study the branch change statistics. 
-- For this, your schema must include an additional table student_dept_change in your
-- schema that maintains a record of students that have changed their department consisting of 
-- old student id, old dept id, new dept id, and new student id 
-- (both old dept id and new dept id must be For-eign key referring to department table). 
-- 
-- Write a single trigger (name log student dept change) that calls a function upon updating the student table. 
-- The function should do as follows: 
-- Before the update, if the update is changing the student’s department, 
-- check if their department was updated before from student dept change table; 
-- if yes, raise an exception “Department can be changed only once” (every student can only change their department once). 
-- If the department has not changed before and the entry year (entry year can be extracted from student id) is less than 2022,
-- Raise an exception: “Entry year must be >= 2022”. Only students who entered in 2022 or later can change their department. 
-- Further, check whether the average grade of the student is > 8.5 or not (from student courses table) 
-- if the average grade of the student is <= 8.5 or the student has done no courses so far raise an exception “Low Grade”. 
-- If all conditions are met, perform the update, and 
-- after the update, insert a row into the student dept change table.
-- Note: While assigning the new student id you have to check the seq number in the valid entry table 
-- to assign the valid student id. Also, do not forget to increase the counter, i.e., seq number in valid entry table after updating the student id. 
-- Also, you have to update the corresponding valid email id in the student table.
-- 

drop table if exists student_dept_change;
create table if not exists student_dept_change(
    old_student_id CHAR(11),
    old_dept_id CHAR(3) REFERENCES department(dept_id),
    new_student_id CHAR(11),
    new_dept_id CHAR(3) REFERENCES department(dept_id)
);
-- -- ???????????????????????????????????????????
create or replace function log_student_dept_change() 
returns trigger as $$
DECLARE
    avg_grade numeric;
    current_seq_number integer;
    entry_yr integer;
begin
    select avg(grade) into avg_grade from student_courses where student_id = old.student_id and grade >=5 group by student_id;
    select seq_number into current_seq_number from valid_entry where entry_year = cast(substring(old.student_id from 1 for 4) as integer) and dept_id =new.dept_id;
    select cast(substring(old.student_id from 1 for 4) as integer) into entry_yr;

    if old.dept_id <> new.dept_id then
        alter table student_courses disable trigger all;
        -- alter table student disable trigger all;
        -- alter table student_dept_change disable trigger all;
        -- alter table valid_entry disable trigger all;
        -- alter table student_courses disable trigger all;
        -- alter table department disable trigger all;
        -- alter table courses disable trigger all;
        -- alter table professor disable trigger all;
        -- alter table course_offers disable trigger all;
        -- alter table valid_entry disable trigger all;
        -- alter table student_dept_change disable trigger all;

        if exists (select * from student_dept_change where (new_student_id= old.student_id)) then
            raise exception 'Department can be changed only once';
        elsif entry_yr < 2022 then
            raise exception 'Entry year must be >= 2022';
        elsif avg_grade is NULL or avg_grade <= 8.5 then
                raise exception 'Low Grade';
        else
            new.student_id := substring(old.student_id from 1 for 4) || new.dept_id ||  current_seq_number;
            -- new.student_id := substring(old.student_id from 1 for 4) || new.dept_id ||  lpad(SELECT CAST(current_seq_number AS VARCHAR) AS string_value, 3, 0);
            update valid_entry set seq_number = current_seq_number + 1 where entry_year = entry_yr and dept_id = new.dept_id;
            update student_courses set student_id = new.student_id where student_id = old.student_id;

            insert into student_dept_change values(old.student_id, old.dept_id, new.student_id, new.dept_id);

            new.email_id = new.student_id || '@' || new.dept_id || '.iitd.ac.in';
        
            return new;
        end if;
    else
        return new;
    end if;
    
end;
$$ language plpgsql;

create or replace trigger log_student_dept_change
before update on student
for each row
execute function log_student_dept_change();

create or replace function after_update_student()
returns trigger as $$
begin
    if old.dept_id <> new.dept_id then
    alter table student_courses enable trigger all;
    -- alter table student enable trigger all;
    -- alter table student_dept_change enable trigger all;
    -- alter table valid_entry enable trigger all;
    -- alter table student_courses enable trigger all;
    -- alter table department enable trigger all;
    -- alter table courses enable trigger all;
    -- alter table professor enable trigger all;
    -- alter table course_offers enable trigger all;
    -- alter table valid_entry enable trigger all;
    -- alter table student_dept_change enable trigger all;
    end if;
    return new;
end;
$$ language plpgsql;

create or replace trigger after_update_student
after update on student
for each row
execute function after_update_student();

-- 2.2 Modifications to student courses table

-- 2.2.1. Evaluating the recently concluded course is essential for planning and execution of the same course in the future. 
-- It is imperative to maintain a view which provides an average, min, and max grade for a particular course 
-- whenever there is a change (insert and update of a tuple) in student course table. 
-- Such a view should contain the following columns and must be up to date at all times:
-- Note: The name of the view should be course eval.
-- course id, session, semester, number of students,. average grade, max grade, min grade

CREATE MATERIALIZED VIEW course_eval AS
SELECT course_id, session, semester, COUNT(student_id) AS number_of_students, AVG(grade) AS average_grade, MAX(grade) AS max_grade, MIN(grade) AS min_grade
FROM student_courses
GROUP BY course_id, session, semester;


CREATE OR REPLACE FUNCTION update_course_eval()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW course_eval;
    RETURN null;
END;
$$ LANGUAGE plpgsql;


create or replace trigger update_course_eval 
AFTER INSERT OR UPDATE or delete ON student_courses
FOR EACH statement
EXECUTE FUNCTION update_course_eval();


-- 2.2.2. Create a trigger which updates the student table’s tot credits column each time an entry is made into the student courses table. 
-- Each time an entry for a student pursuing any course is made in the student courses table, the following is expected.
-- Given the entry that is to be inserted into the student courses table, 
-- use the course id and the courses table to get the number of credits for that course. 
-- Now that you know the credits for this course, update that particular student’s tot credits and 
-- add the credits for this new course in the student table.

CREATE OR REPLACE FUNCTION update_tot_credits()
RETURNS TRIGGER AS $$
DECLARE
    course_credits NUMERIC;
BEGIN
    SELECT credits INTO course_credits
    FROM courses WHERE course_id = NEW.course_id;
    UPDATE student
    SET tot_credits = tot_credits + course_credits
    WHERE student_id = NEW.student_id;
    RETURN NEW;
END;
$$ language plpgsql;

create or replace trigger update_tot_credits
AFTER INSERT ON student_courses
FOR EACH ROW
EXECUTE FUNCTION update_tot_credits();

-- 2.2.3. 
CREATE OR REPLACE FUNCTION validate_courses_credits()
RETURNS TRIGGER AS $$
DECLARE
    current_credits NUMERIC;
    current_courses INTEGER;
BEGIN
    SELECT tot_credits INTO current_credits
    FROM student WHERE student_id = NEW.student_id;

    SELECT COUNT(*) INTO current_courses FROM student_courses
    WHERE student_id = NEW.student_id AND student_courses.session = NEW.session AND semester = NEW.semester;

    IF current_credits + (SELECT credits FROM courses WHERE course_id = NEW.course_id) > 60 THEN
        RAISE EXCEPTION 'invalid';
    ELSIF current_courses >= 5 THEN
        RAISE EXCEPTION 'invalid';
    ELSE
        RETURN NEW;
    END IF;
END;
$$ language plpgsql;

create or replace trigger validate_courses_credits
BEFORE INSERT ON student_courses
FOR EACH ROW
EXECUTE FUNCTION validate_courses_credits();


-- 2.2.4. Assume that we are trying to insert a record into the student courses table. 
-- Write a trigger which uses course id as the foreign key and makes sure that any course of 5 credits is taken up by the student in the student’s first year only.
-- (You can know the student’s first year since the student id begins with the year of their admission; 
-- compare this with the first four digits of the session of the course, which is usually of the form 2023-2024). 
-- If the entry is for a 5-credit course and is not in the first year of the student, 
-- Raise an ”invalid” exception; else, insert the entry into the table. 
-- Any entry with a course with less than 5 credits should be added.

CREATE OR REPLACE FUNCTION validate_5_credit_courses()
RETURNS TRIGGER AS $$
DECLARE
    entry_year INTEGER;
    course_credits NUMERIC;
BEGIN
    SELECT SUBSTRING(NEW.student_id FROM 1 FOR 4) INTO entry_year;
    SELECT credits INTO course_credits
    FROM courses
    WHERE course_id = NEW.course_id;
    IF course_credits >= 5 AND entry_year <> cast(SUBSTRING(NEW.session FROM 1 FOR 4) as integer) THEN
        RAISE EXCEPTION 'invalid5';
    ELSE
        RETURN NEW;
    END IF;
END;
$$ language plpgsql;

create or replace trigger validate_5_credit_courses
BEFORE INSERT ON student_courses
FOR EACH ROW
EXECUTE FUNCTION validate_5_credit_courses();

-- 2.2.5.
CREATE  MATERIALIZED VIEW student_semester_summary AS
SELECT s.student_id, s.session, s.semester, SUM(s.grade * c.credits) / SUM(c.credits) AS sgpa, SUM(c.credits) AS credits
FROM student_courses s join courses c on s.course_id = c.course_id
WHERE s.grade >= 5
GROUP BY s.student_id, s.session, s.semester;

-- insert trigger


CREATE OR REPLACE FUNCTION insert_student_semester_summary()
RETURNS TRIGGER AS $$
DECLARE
    current_credits NUMERIC;
BEGIN
    SELECT tot_credits INTO current_credits 
    FROM student join student_courses on student.student_id = student_courses.student_id
    WHERE student.student_id = NEW.student_id
    AND student_courses.session = NEW.session
    AND student_courses.semester = NEW.semester;
    
    IF current_credits + (SELECT credits FROM courses WHERE course_id = NEW.course_id) > 26 THEN
        RAISE EXCEPTION 'invalid';
    ELSE
        REFRESH MATERIALIZED VIEW student_semester_summary;
        RETURN NEW;
    END IF;
END;
$$ language plpgsql;

create or replace trigger insert_student_semester_summary
before INSERT ON student_courses
FOR EACH ROW
EXECUTE FUNCTION insert_student_semester_summary();

-- -- update trigger

CREATE OR REPLACE FUNCTION update_student_semester_summary()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW student_semester_summary;
    RETURN NEW;
END;
$$ language plpgsql;
create or replace trigger update_student_semester_summary
AFTER UPDATE ON student_courses
FOR EACH ROW
EXECUTE FUNCTION update_student_semester_summary();

-- -- delete trigger

CREATE OR REPLACE FUNCTION delete_student_semester_summary()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE student
    SET tot_credits = tot_credits - (SELECT credits FROM courses WHERE course_id = OLD.course_id)
    WHERE student_id = OLD.student_id;

    REFRESH MATERIALIZED VIEW student_semester_summary;
    RETURN OLD;
END;
$$ language plpgsql;

create or replace trigger delete_student_semester_summary
AFTER DELETE ON student_courses
FOR EACH ROW
EXECUTE FUNCTION delete_student_semester_summary();





-- 2.2.6. Write a single trigger on insert into student courses table. 
-- Before insertion, check if the capacity of the course is full from the course offers table; 
-- if yes raise an “course is full” exception; 
-- if it isn’t full, perform the insertion, and after insertion, 
-- update the no. of enrollments in the course in course offers table.


CREATE OR REPLACE FUNCTION validate_course_vacancy()
RETURNS TRIGGER AS $$
DECLARE
    current_enrollments INTEGER;
    course_capacity INTEGER;
BEGIN
    SELECT enrollments INTO current_enrollments FROM course_offers
    WHERE course_id = NEW.course_id AND session = NEW.session AND semester = NEW.semester;
    
    SELECT capacity INTO course_capacity FROM course_offers
    WHERE course_id = NEW.course_id AND session = NEW.session AND semester = NEW.semester;
    
    IF current_enrollments >= course_capacity THEN
        RAISE EXCEPTION 'course is full';
    ELSE
        UPDATE course_offers
        SET enrollments = enrollments + 1
        WHERE course_id = NEW.course_id
        AND course_offers.session = NEW.session
        AND semester = NEW.semester;
        RETURN NEW;
    END IF;
END;
$$ language plpgsql;

create or replace trigger validate_course_vacancy
BEFORE INSERT ON student_courses
FOR EACH ROW
EXECUTE FUNCTION validate_course_vacancy();


-- 2.3 Modifications to course offers table

-- 2.3.1. 
CREATE OR REPLACE FUNCTION remove_course_offers()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM student_courses
    WHERE course_id = OLD.course_id AND session = OLD.session AND semester = OLD.semester;
    
    UPDATE student
    SET tot_credits = tot_credits - (SELECT credits FROM courses WHERE course_id = OLD.course_id)
    WHERE student_id IN (SELECT student_id FROM student_courses WHERE course_id = OLD.course_id);


    RETURN OLD;
END;
$$ language plpgsql;

create or replace trigger remove_course_offers
before DELETE ON course_offers
FOR EACH ROW
EXECUTE FUNCTION remove_course_offers();

-- 2.3.2. Given an entry that is to be inserted into the course offers table, 
-- create a trigger that makes sure that a professor does not teach more than 4 courses in a session. 
-- Also make sure that the course is being offered before the associated professor resigns. 
-- If in any case the entry is not valid show an ”invalid” message or else insert the entry into the table.

create or replace function insert_course_offers()
returns trigger as $$
declare
    current_courses integer;
    resign_yr integer;
begin
    select count(*) into current_courses from course_offers
    where professor_id is not null and professor_id = new.professor_id and session = new.session 
    -- and semester = new.semester
    ;

    select resign_year into resign_yr from professor
    where professor_id = new.professor_id;
    current_courses := current_courses + 1;

    if current_courses > 4 then
        raise exception 'invalid1';
    elsif resign_yr < cast(substring(new.session from 1 for 4) as integer)  then
        raise exception 'invalid2';
    else
        return new;
    end if;
end;
$$ language plpgsql;

create or replace trigger insert_course_offers
before insert on course_offers
for each row
execute function insert_course_offers();

-- 2.4 Modifications to department table
create or replace function update_dept_id()
returns trigger as $$
begin
    if tg_op = 'UPDATE' and old.dept_id <> new.dept_id then

        -- update course_offers
        -- set course_id = new.dept_id || substring(course_id from 4 for 3) 
        -- where course_id ~ ('^' || old.dept_id || '...$');

        -- update courses
        -- set course_id = new.dept_id || substring(course_id from 4 for 3),
        -- dept_id = new.dept_id
        -- where dept_id = old.dept_id;

        -- update student
        -- set student_id = substring(student_id from 1 for 4) || new.dept_id || substring(student_id from 8 for 3),
        -- email_id = substring(student_id from 1 for 4) || new.dept_id || substring(student_id from 8 for 3) || '@' || new.dept_id || '.iitd.ac.in' ,
        -- dept_id = new.dept_id where dept_id = old.dept_id;

        -- update student_courses
        -- set course_id = new.dept_id || substring(course_id from 4 for 3),
        -- student_id = substring(student_id from 1 for 4) || new.dept_id || substring(student_id from 8 for 3)
        -- where course_id ~ ('^' || old.dept_id || '...$');

        -- update professor
        -- set dept_id = new.dept_id where dept_id = old.dept_id;

        -- update valid_entry
        -- set dept_id = new.dept_id where dept_id = old.dept_id;
        
    elsif tg_op = 'DELETE' then
        if (select count(*) from student where dept_id = old.dept_id )> 0 then
            raise exception 'Department has students';
        else
            delete from valid_entry where dept_id = old.dept_id;
            delete from courses where dept_id = old.dept_id;
            delete from professor where dept_id = old.dept_id;
            delete from student where dept_id = old.dept_id;
        end if;
    end if;
    return old;
end;
$$ language plpgsql;

create or replace trigger update_dept_id
-- before update 
before delete 
on department
for each row
execute function update_dept_id();
