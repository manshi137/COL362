with tmp0 as(
    select distinct diagnoses_icd.subject_id, diagnoses_icd.hadm_id, diagnoses_icd.icd_code, 
    admissions.admittime, admissions.dischtime 
    from diagnoses_icd join admissions
    on diagnoses_icd.subject_id = admissions.subject_id and diagnoses_icd.hadm_id = admissions.hadm_id
    where icd_code like 'I21%'
),
tmp1 as(
    select distinct t1.subject_id from tmp0 t1, admissions t2
    where t1.subject_id = t2.subject_id and t1.hadm_id != t2.hadm_id
    and t2.admittime> t1.dischtime
)
select * from tmp1 
order by tmp1.subject_id desc
limit 1000;    
