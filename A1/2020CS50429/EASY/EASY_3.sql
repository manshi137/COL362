select distinct count(*)  from labevents
where priority = 'ROUTINE' and flag = 'abnormal';