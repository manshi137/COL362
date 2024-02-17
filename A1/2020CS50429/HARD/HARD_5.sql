with recursive nodes as(
    select distinct subject_id, hadm_id, admittime, dischtime 
    from admissions 
    order by admittime 
    limit 500
),
tmp2 as( 
    select distinct nodes.subject_id, nodes.hadm_id, diagnoses_icd.icd_code, diagnoses_icd.icd_version, nodes.admittime, nodes.dischtime
    from diagnoses_icd join nodes 
    on diagnoses_icd.hadm_id = nodes.hadm_id and diagnoses_icd.subject_id = nodes.subject_id
    where nodes.dischtime is not null and nodes.admittime is not null and nodes.admittime<=nodes.dischtime
),
edges(subject_id1, subject_id2) as(
    select n1.subject_id, n2.subject_id
    from tmp2 n1 join tmp2 n2 
    on n1.subject_id != n2.subject_id 
    where ((n1.admittime <= n2.admittime and n2.admittime <= n1.dischtime )
    or (n2.admittime <= n1.admittime and n1.admittime <= n2.dischtime) )    
    and n1.icd_code = n2.icd_code and n1.icd_version = n2.icd_version
),
check_path as (
    select subject_id1, subject_id2, 1 as path_length
    from edges
    where subject_id1 = 10001725
    union all
    select e.subject_id1, e.subject_id2, p.path_length + 1
    from edges e join check_path p on e.subject_id1 = p.subject_id2
    where p.path_length < 5  
)
select count(*) > 0 as pathexists from check_path where subject_id2 = 19438360;