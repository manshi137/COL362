with tmp as(
    select distinct subject_id, stay_id, los from icustays
    where first_careunit like '%MICU%' or last_careunit like '%MICU%'
), 
tmp1 as(
    select subject_id, count(distinct stay_id) as total_stays, avg(los) as avg_length_of_stay from tmp
    group by subject_id
    having count(stay_id)>=5
)
select * from tmp1 
order by -avg_length_of_stay, -total_stays, -subject_id
limit 500;

