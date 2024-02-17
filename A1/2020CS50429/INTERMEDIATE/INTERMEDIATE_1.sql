with tmp1 as(
    select subject_id,  count(distinct stay_id) from 
    icustays
    group by subject_id
    having count(distinct stay_id)>=5
)
select distinct tmp1.subject_id, tmp1.count from tmp1 
order by tmp1.count DESC, tmp1.subject_id DESC
limit 1000;