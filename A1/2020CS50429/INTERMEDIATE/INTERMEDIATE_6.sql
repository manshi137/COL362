with tmp1 as
    (select subject_id, count(*) as diagnosis_count
    from diagnoses_icd where icd_code = '5723'
    group by subject_id
    order by subject_id
    ),
tmp2 as(
    select distinct subject_id, count(distinct hadm_id) as total_admissions,
    min(admittime) as first_admission, max(admittime) as last_admission
    from admissions where subject_id in (select subject_id from tmp1)
    group by subject_id
),
tmp3 as(
    select distinct tmp1.subject_id, tmp1.diagnosis_count
    from tmp1
    group by tmp1.subject_id, tmp1.diagnosis_count
),
tmp4 as(
    select tmp2.subject_id, tmp2.total_admissions, tmp2.last_admission, tmp2.first_admission, tmp3.diagnosis_count
    from tmp2 join tmp3 on tmp2.subject_id = tmp3.subject_id
)
select tmp4.subject_id, patients.gender, tmp4.total_admissions, tmp4.last_admission, tmp4.first_admission, tmp4.diagnosis_count
from tmp4 join patients on tmp4.subject_id = patients.subject_id
order by tmp4.total_admissions DESC, tmp4.diagnosis_count DESC, tmp4.last_admission DESC, tmp4.first_admission DESC, patients.gender DESC, tmp4.subject_id DESC
limit 1000;