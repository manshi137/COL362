with age_filter as(
    select patients.subject_id, admissions.hadm_id, patients.anchor_age from 
    patients join admissions on patients.subject_id = admissions.subject_id where anchor_age<50
),
proc_filter as(
    select distinct age_filter.subject_id, age_filter.hadm_id, age_filter.anchor_age, procedures_icd.icd_code, procedures_icd.icd_version 
    from age_filter join procedures_icd 
    on age_filter.subject_id = procedures_icd.subject_id and age_filter.hadm_id = procedures_icd.hadm_id
),
proc_count(subject_id, icd_code, icd_version, anchor_age, count) as(
    select subject_id, icd_code, icd_version, anchor_age, count(hadm_id) from proc_filter
    group by subject_id, icd_code, icd_version, anchor_age
)
,
ans as(
    select distinct subject_id, anchor_age from proc_count where count>1
    order by subject_id, anchor_age
)
select * from ans;