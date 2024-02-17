
with tmp1 as(
    select subject_id, hadm_id from admissions 
    where hospital_expire_flag=1 and admission_type='URGENT'
),
tmp3 as(
    select d.subject_id, d.hadm_id, d.icd_code, d.icd_version from tmp1 join diagnoses_icd d
    on tmp1.subject_id=d.subject_id and tmp1.hadm_id=d.hadm_id
),
tmp4 as(
    select tmp3.subject_id, tmp3.hadm_id, d.icd_code, d.long_title 
    from d_icd_diagnoses d join tmp3
    on d.icd_code=tmp3.icd_code and d.icd_version=tmp3.icd_version
)
select * from tmp4
order by subject_id desc, hadm_id desc, icd_code desc, long_title desc 
limit 1000;