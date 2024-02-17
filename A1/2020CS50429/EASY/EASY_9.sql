with abnormal as(
    select subject_id, hadm_id from labevents where flag = 'abnormal'
),
expire as(
    select subject_id, hadm_id from admissions where hospital_expire_flag = 1
),
tmp as(
    select distinct subject_id, hadm_id from abnormal intersect select * from expire
)
select count(*) from tmp;