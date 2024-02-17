with latest_admissions as(
    select subject_id, max(admittime) as latest_admittime from admissions
    group by subject_id
),
icd as(
    select icd_code, icd_version from d_icd_diagnoses
    where long_title like '%Meningitis%'
),
tmp0 as(
    select distinct admissions.subject_id, admissions.hadm_id, admissions.hospital_expire_flag
    from admissions where (admissions.subject_id, admissions.admittime) in
    (select subject_id, latest_admittime from latest_admissions)
),
tmp1 as(
    select distinct diagnoses_icd.subject_id, diagnoses_icd.hadm_id 
    from diagnoses_icd
    where (diagnoses_icd.icd_code, diagnoses_icd.icd_version) in (select * from icd)
),
tmp2 as(
    select distinct tmp0.subject_id, tmp0.hospital_expire_flag 
    from tmp0 where (tmp0.subject_id, tmp0.hadm_id) in (select * from tmp1)
),
tmp3 as(
    select distinct tmp2.subject_id, patients.gender, tmp2.hospital_expire_flag 
    from tmp2 join patients
    on tmp2.subject_id = patients.subject_id
)
SELECT 
    gender,
    ROUND(SUM(CASE WHEN hospital_expire_flag = 1 THEN 1 ELSE 0 END) * 100.00 / COUNT(*), 2) 
    as mortality_rate
FROM 
    tmp3
GROUP BY 
    gender;
