with diagnose as(
    select icd_code, icd_version, count(distinct hadm_id) as total_admissions
    from diagnoses_icd
    group by icd_code, icd_version
),
expire as(
    select icd_code, icd_version, count(distinct admissions.hadm_id) as total_expire
    from diagnoses_icd join admissions 
    on diagnoses_icd.subject_id = admissions.subject_id and diagnoses_icd.hadm_id = admissions.hadm_id
    where hospital_expire_flag = 1
    group by icd_code, icd_version
),
mortality as(
    select distinct diagnose.icd_code, diagnose.icd_version, round(total_expire*100.0/total_admissions, 2) as mortality_rate
    from diagnose join expire 
    on diagnose.icd_code = expire.icd_code and diagnose.icd_version = expire.icd_version
),
top_mortality as(
    select * from mortality natural join d_icd_diagnoses 
    order by mortality_rate desc
    limit 245
),
tmp1 as(
    select subject_id, hadm_id, hospital_expire_flag
    from admissions
),
tmp2 as(
    select tmp1.subject_id, d.icd_code, d.icd_version, max(tmp1.hospital_expire_flag) as survival
    from tmp1 
    join diagnoses_icd d on tmp1.subject_id = d.subject_id and tmp1.hadm_id = d.hadm_id
    join top_mortality on d.icd_code = top_mortality.icd_code and d.icd_version = top_mortality.icd_version
    group by (tmp1.subject_id, d.icd_code, d.icd_version)
    having max(tmp1.hospital_expire_flag)<>1
),
tmp3 as(
    select tmp2.icd_code, tmp2.icd_version, avg(patients.anchor_age) as survived_avg_age 
    from tmp2 join patients on tmp2.subject_id = patients.subject_id 
    group by tmp2.icd_code, tmp2.icd_version
)
select tmp3.icd_code, tmp3.icd_version, d.long_title, tmp3.survived_avg_age 
from tmp3 join d_icd_diagnoses d on tmp3.icd_code = d.icd_code and tmp3.icd_version = d.icd_version
order by d.long_title, survived_avg_age desc;