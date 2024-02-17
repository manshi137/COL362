with tmp as(
    select subject_id, count(hadm_id)  from admissions
    group by subject_id
    order by -count(hadm_id)
)
select subject_id, count as num_admissions from tmp
where count = (select max(count) from tmp)
order by subject_id;
