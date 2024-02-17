with tmp1 as(
    select p.drug, count(p.drug) as prescription_count
    from prescriptions p join admissions a 
    on p.subject_id = a.subject_id and p.hadm_id = a.hadm_id
    where p.starttime >= a.admittime 
    and p.starttime <= a.admittime + interval '12' hour
    group by p.drug
),
ans as(
    select tmp1.drug, sum(tmp1.prescription_count) AS prescription_count  from tmp1
    group by tmp1.drug
    order by sum(tmp1.prescription_count) DESC, tmp1.drug DESC
    limit 1000
)
select * from ans;