
with tmp1 as(
    select subject_id, hadm_id from labevents where itemid=50878
    group by subject_id, hadm_id
),
tmp2(subject_id, hadm_id, los) as(
    select tmp1.subject_id, tmp1.hadm_id, icustays.los from icustays join tmp1
    on icustays.subject_id=tmp1.subject_id and icustays.hadm_id=tmp1.hadm_id 

    where los is not null
),
ans as(
    select subject_id, avg(los) as avg_stay_duration from tmp2
    group by subject_id, hadm_id
    order by avg(los) desc, subject_id desc
)
select * from ans;
