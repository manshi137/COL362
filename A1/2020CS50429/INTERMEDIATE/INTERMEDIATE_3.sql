with tmp1 as(
    select distinct subject_id, hadm_id, description from drgcodes
    where lower(description) like '%alcoholic%'
),
tmp2 as(
    select subject_id, count(distinct hadm_id) from tmp1
    group by subject_id
    having count(distinct hadm_id) > 1
),
tmp3 as(
    select tmp1.subject_id, count(tmp1.hadm_id) as diagnoses_count
    from tmp1 join tmp2 on tmp1.subject_id = tmp2.subject_id
    group by tmp1.subject_id
)
select * from tmp3 
order by -tmp3.diagnoses_count, -tmp3.subject_id;

