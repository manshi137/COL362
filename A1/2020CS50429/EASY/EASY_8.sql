with typhoid as(
    select icd_code, icd_version from d_icd_diagnoses 
    where long_title ='Typhoid fever'
),
typhoid_id as(
    select subject_id, hadm_id from diagnoses_icd join typhoid 
    on typhoid.icd_code = diagnoses_icd.icd_code and typhoid.icd_version = diagnoses_icd.icd_version
),
typhoid_icu as(
    select typhoid_id.subject_id from typhoid_id join icustays 
    on typhoid_id.subject_id= icustays.subject_id and typhoid_id.hadm_id = icustays.hadm_id
)
select distinct patients.subject_id, patients.anchor_age from typhoid_icu join patients 
on typhoid_icu.subject_id = patients.subject_id 
order by subject_id, anchor_age;

