select distinct count(*) from procedures_icd
where subject_id = '10000117'
group by icd_code, icd_version;