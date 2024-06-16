
create table if not exists department(
    dept_id CHAR(3) PRIMARY KEY NOT NULL UNIQUE,
    dept_name VARCHAR(40) NOT NULL UNIQUE
);
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
ON UPDATE CASCADE;
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
        LENGTH(course_id) = 6 AND SUBSTRING(course_id FROM 4 FOR 3) ~ '^\d{3}'
    )
);
create or replace function check_dept_exists() returns trigger as $$
begin
    if not exists (select * from department where dept_id = substring(new.course_id from 1 for 3)) then
        raise exception 'Department does not exist';
    end if;
    return new;
end;
$$ language plpgsql;

create or replace trigger check_dept_exists
before insert on courses
for each row
execute function check_dept_exists();


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

drop table if exists valid_entry;
create table if not exists valid_entry(
    dept_id CHAR(3) REFERENCES department(dept_id) ON UPDATE CASCADE,
    entry_year INTEGER NOT NULL,
    seq_number INTEGER NOT NULL default 1
);

CREATE OR REPLACE FUNCTION validate_student_id()
RETURNS TRIGGER AS $$
DECLARE
    current_seq_number INTEGER;
BEGIN
    SELECT seq_number INTO current_seq_number
    FROM valid_entry WHERE entry_year = CAST(SUBSTRING(new.student_id FROM 1 FOR 4) AS INTEGER) AND dept_id = NEW.dept_id;


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
        RAISE EXCEPTION 'invalid';
    END IF;
    
END;
$$ language plpgsql;


create or replace trigger validate_student_id
before insert on student
for each row
execute function validate_student_id();



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



CREATE OR REPLACE FUNCTION validate_email_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email_id ~ '.{10}@.{3}\.iitd\.ac\.in$' THEN
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





drop table if exists student_dept_change;
create table if not exists student_dept_change(
    old_student_id CHAR(11),
    old_dept_id CHAR(3) REFERENCES department(dept_id),
    new_student_id CHAR(11),
    new_dept_id CHAR(3) REFERENCES department(dept_id)
);


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
        if not exists (select * from department where dept_id = old.dept_id) then
            return new; 
        end if;
        alter table student_courses disable trigger all;
        if exists (select * from student_dept_change where (new_student_id= old.student_id)) then
            raise exception 'Department can be changed only once';
        elsif entry_yr < 2022 then
            raise exception 'Entry year must be >= 2022';
        elsif avg_grade is NULL or avg_grade <= 8.5 then
                raise exception 'Low Grade';
        else
            new.student_id := substring(old.student_id from 1 for 4) || new.dept_id ||  lpad(current_seq_number::TEXT, 3, '0');
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
    end if;
    return new;
end;
$$ language plpgsql;

create or replace trigger after_update_student
after update on student
for each row
execute function after_update_student();




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
        RAISE EXCEPTION 'invalid';
    ELSE
        RETURN NEW;
    END IF;
END;
$$ language plpgsql;

create or replace trigger validate_5_credit_courses
BEFORE INSERT ON student_courses
FOR EACH ROW
EXECUTE FUNCTION validate_5_credit_courses();


CREATE  MATERIALIZED VIEW student_semester_summary AS
SELECT s.student_id, s.session, s.semester, SUM(s.grade * c.credits) / SUM(c.credits) AS sgpa, SUM(c.credits) AS credits
FROM student_courses s join courses c on s.course_id = c.course_id
WHERE s.grade >= 5
GROUP BY s.student_id, s.session, s.semester;




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



create or replace function insert_course_offers()
returns trigger as $$
declare
    current_courses integer;
    resign_yr integer;
begin
    select count(*) into current_courses from course_offers
    where professor_id is not null and professor_id = new.professor_id and session = new.session     ;

    select resign_year into resign_yr from professor
    where professor_id = new.professor_id;
    current_courses := current_courses + 1;

    if current_courses > 4 then
        raise exception 'invalid';
    elsif resign_yr < cast(substring(new.session from 1 for 4) as integer)  then
        raise exception 'invalid';
    else
        return new;
    end if;
end;
$$ language plpgsql;

create or replace trigger insert_course_offers
before insert on course_offers
for each row
execute function insert_course_offers();


create or replace function update_dept_id()
returns trigger as $$
begin
    if tg_op = 'UPDATE' and old.dept_id <> new.dept_id then
        alter table student_dept_change disable trigger all;
        alter table student disable trigger all;
        alter table courses disable trigger all;
        alter table valid_entry disable trigger all;
        alter table professor disable trigger all;
        alter table student_courses disable trigger all;
        alter table course_offers disable trigger all;


        update student_dept_change set 
        new_student_id = substring(new_student_id from 1 for 4)||new.dept_id || substring(new_student_id from 8 for 3) ,
        new_dept_id = new.dept_id where new_dept_id = old.dept_id;
        update student_dept_change set
        old_student_id = substring(old_student_id from 1 for 4)||new.dept_id || substring(old_student_id from 8 for 3) ,
        old_dept_id = new.dept_id where old_dept_id = old.dept_id;


        update student set 
        student_id = substring(student_id from 1 for 4)||new.dept_id || substring(student_id from 8 for 3) ,
        dept_id = new.dept_id where dept_id = old.dept_id;


        update courses set 
        course_id = new.dept_id || substring(course_id from 4 for 3) ,
        dept_id = new.dept_id where dept_id = old.dept_id;


        update valid_entry set
        dept_id = new.dept_id where dept_id = old.dept_id;


        update professor set
        dept_id = new.dept_id where dept_id = old.dept_id;
        return new;
    elsif tg_op = 'DELETE' then
        if (select count(*) from student where dept_id = old.dept_id )> 0 then
            raise exception 'Department has students';
        else
            delete from valid_entry where dept_id = old.dept_id;
            delete from courses where dept_id = old.dept_id;
            delete from professor where dept_id = old.dept_id;
            delete from student where dept_id = old.dept_id;
            return old;
        end if;
    end if;
    return old;
end;
$$ language plpgsql;

create or replace trigger update_dept_id
before update or delete 
on department
for each row
execute function update_dept_id();


create  or replace function after_update_dept_id()
returns trigger as $$
begin
    alter table student_dept_change enable trigger all;
    alter table student enable trigger all;
    alter table courses enable trigger all;
    alter table valid_entry enable trigger all;
    alter table professor enable trigger all;
    alter table student_courses enable trigger all;
    alter table course_offers enable trigger all;

    return new;
end;
$$ language plpgsql;

create or replace trigger after_update_dept_id
after update on department
for each row
execute function after_update_dept_id();
