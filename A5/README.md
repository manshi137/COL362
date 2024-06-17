# Assignment 5: Rule Based Query Optimization
### Goal
- Implement a simple rule based query optimizer in Apache Calcite
- In the last assignment, we had implemented physical operators by simply mapping logical operators to physical operators
- Here we implement query optimizer which optimize query plan by merging projection and filter operators

### Idea Overview
- First we implemented the rule in PRules.java which merged the projection and filter operators
- Next I didn't write any new code, I borrowed the code for projection and filter from the last assignment on pipelined execution and made some minor changes to merge the two operations

This was the easiest assingment of all the DBMS assignments but it gave exposure to query optimisation as well.

Let me know if you have any doubts in the code :)