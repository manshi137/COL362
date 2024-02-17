with tmp as
(select pharmacy_id, subject_id from prescriptions
order by pharmacy_id)
,
tmp2 as(
select pharmacy_id, count(distinct subject_id) from tmp
group by pharmacy_id
)
select distinct pharmacy_id,count from tmp2 where count = (select min(count) from tmp2)
order by pharmacy_id;
