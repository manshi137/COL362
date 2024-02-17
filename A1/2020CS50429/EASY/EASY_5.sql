with tmp as(
    select patients.subject_id, patients.anchor_age, count(first_careunit)
    from icustays join patients on icustays.subject_id = patients.subject_id
    where icustays.first_careunit = 'Coronary Care Unit (CCU)'
    group by patients.subject_id
)

select subject_id, anchor_age, count from tmp
where count = (select max(count) from tmp)
order by -anchor_age, -subject_id ;