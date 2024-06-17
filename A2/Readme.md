# Assignment 2 : Database Design for Course Administration
- The objective of this assignment was to design a database for course administration purpose
- The schema is provided in the assignment document attached
- Initial part of the assignment involved creating the database with the given datatypes and contraints
- We were then supposed to add check constraints on top of those as given in the assignment document
- Post this we were supposed to use views, functions, procedures and triggers to support all the advanced constraints so that we don't run into loops or lead into modifying the parts of database which we don't intend to


on postgres terminal:
```drop database a2db; create database a2db;```

terminal:
```psql -U manshisagar -W -d a2db -f ASSIGNMENT_2.sql```
```psql -U manshisagar -W -d a2db -f test.sql```