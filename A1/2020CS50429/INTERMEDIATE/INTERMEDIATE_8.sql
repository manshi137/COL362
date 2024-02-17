with tmp1 as(
    select distinct subject_id, hadm_id, icd_code from diagnoses_icd
    where icd_code like 'V4%'
),
tmp2 as(
    select p.subject_id, p.hadm_id, p.drug 
    from prescriptions p 
    where lower(drug) like '%prochlorperazine%' or lower(drug) like '%bupropion%'
    and (p.subject_id, p.hadm_id) in (select subject_id, hadm_id from tmp1)
),
tmp3 as(
    select tmp1.subject_id, tmp1.hadm_id, count(distinct tmp1.icd_code) as distinct_diagnoses_count,  tmp2.drug
    from tmp1 join tmp2 
    on tmp1.subject_id=tmp2.subject_id and tmp1.hadm_id=tmp2.hadm_id
    group by tmp1.subject_id, tmp1.hadm_id, tmp2.drug
    having count(distinct tmp1.icd_code) > 1
)
select * from tmp3
order by distinct_diagnoses_count desc, subject_id desc, hadm_id desc, drug;


