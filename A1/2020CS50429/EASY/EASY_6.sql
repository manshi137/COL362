with tmp(icd_code, icd_version) as (
    select icd_code, icd_version from d_icd_diagnoses where long_title = 'Cholera due to vibrio cholerae'
)
select count(distinct diagnoses_icd.hadm_id)
from tmp join diagnoses_icd 
on diagnoses_icd.icd_code = tmp.icd_code and diagnoses_icd.icd_version = tmp.icd_version;