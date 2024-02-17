with tmp1 as(
    select subject_id, drug, count(distinct hadm_id) from prescriptions
    group by subject_id, drug
    having count(distinct hadm_id)>1
)
select tmp1.subject_id, patients.anchor_year, tmp1.drug 
from tmp1 join patients on tmp1.subject_id=patients.subject_id
order by subject_id desc, anchor_year desc, drug desc
limit 1000;
