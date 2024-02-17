with tmp1 as(
    select distinct p.subject_id, p.hadm_id, p.icd_code, p.icd_version, sum(icustays.los) as sumlos from 
    procedures_icd p join icustays on p.hadm_id = icustays.hadm_id and p.subject_id = icustays.subject_id
    group by p.subject_id, p.hadm_id, p.icd_code, p.icd_version
),
avg_icu_stays as(
    select icd_code, icd_version, avg(sumlos) as avg_los from tmp1 group by icd_code, icd_version
),
tmp2 as(
    select distinct tmp1.subject_id, tmp1.icd_code, tmp1.icd_version, tmp1.sumlos, a.avg_los 
    from tmp1 join avg_icu_stays a 
    on tmp1.icd_code = a.icd_code and tmp1.icd_version = a.icd_version and tmp1.sumlos < a.avg_los
),
tmp3 as(
    select tmp2.subject_id, patients.gender, tmp2.icd_code, tmp2.icd_version
    from tmp2 join patients on tmp2.subject_id = patients.subject_id
    order by tmp2.subject_id, tmp2.icd_code desc, tmp2.icd_version desc, patients.gender
    limit 1000
)
select * from tmp3;