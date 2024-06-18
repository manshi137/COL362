package rel;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.sql.SqlKind;
import convention.PConvention;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/*
    * Implement Hash Join
    * The left child is blocking, the right child is streaming
*/
public class PJoin extends Join implements PRel {

    public PJoin(
            RelOptCluster cluster,
            RelTraitSet traitSet,
            RelNode left,
            RelNode right,
            RexNode condition,
            Set<CorrelationId> variablesSet,
            JoinRelType joinType) {
                super(cluster, traitSet, ImmutableList.of(), left, right, condition, variablesSet, joinType);
                assert getConvention() instanceof PConvention;
    }

    @Override
    public PJoin copy(
            RelTraitSet relTraitSet,
            RexNode condition,
            RelNode left,
            RelNode right,
            JoinRelType joinType,
            boolean semiJoinDone) {
        return new PJoin(getCluster(), relTraitSet, left, right, condition, variablesSet, joinType);
    }

    @Override
    public String toString() {
        return "PJoin";
    }

    // returns true if successfully opened, false otherwise
    Map<List<Object>, List<Object[]>> hashTable = new HashMap<>();
    Map<List<Object>, Integer > hashTableVis = new HashMap<>();
    List<Object[]> joinList = new ArrayList<>();
    int joinIndex = -1;
    int joinSize = 0;
    boolean flag = false; //avoid hasnext two times
    boolean flagleft = false; //added for left outer join

    // IMPLEMENT HASH JOIN
    @Override
    public boolean open() {
        logger.trace("Opening PJoin");
        // joinIndex = 0;
        PRel left = (PRel) getLeft();
        PRel right = (PRel) getRight();
        if (left.open()) {
            // reset map
            hashTable.clear();
            hashTableVis.clear();
            PJoin join = (PJoin)this;
            RexNode condition = join.getCondition();
            RexCall call = (RexCall) condition;
            // multiple join columns
            List<Integer> leftColumns = new ArrayList<>();
            List<Integer> rightColumns = new ArrayList<>();
            if (call.getKind() == SqlKind.AND) {
                List<RexNode> operands = call.getOperands();
                for (RexNode operand : operands) {
                    RexCall callOperand = (RexCall) operand;
                    RexInputRef leftRefOperand = (RexInputRef) (callOperand.getOperands()).get(0);
                    RexInputRef rightRefOperand = (RexInputRef) (callOperand.getOperands()).get(1);
                    leftColumns.add(leftRefOperand.getIndex());
                    rightColumns.add(rightRefOperand.getIndex() - left.getRowType().getFieldCount());
                }
            }
            else{
                RexInputRef leftRef = (RexInputRef) (call.getOperands()).get(0);
                RexInputRef rightRef = (RexInputRef) (call.getOperands()).get(1);
                leftColumns.add(leftRef.getIndex());
                rightColumns.add(rightRef.getIndex() - left.getRowType().getFieldCount());
            }
            while(left.hasNext()) {
                Object[] row = left.next();
                List<Object> key = new ArrayList<>();
                for (int i = 0; i < leftColumns.size(); i++) {
                    key.add(row[leftColumns.get(i)]);
                }
                if (!hashTable.containsKey(key)) {
                    hashTable.put(key, new ArrayList<>());
                }
                hashTable.get(key).add(row);
            }
            right.open();
            // read one row from right table
            boolean newRightRow = false;
            while(!newRightRow && right.hasNext()){
                Object[] rightrow = right.next();
                List<Object> key = new ArrayList<>();
                for (int i = 0; i < rightColumns.size(); i++) {
                    key.add(rightrow[rightColumns.get(i)]);
                }
                if(hashTable.containsKey(key)){
                    newRightRow = true;
                    hashTableVis.put(key, 1);
                    List<Object[]> leftrows = hashTable.get(key);
                    for(Object[] leftrow: leftrows){
                        Object[] row = new Object[leftrow.length + rightrow.length];
                        for(int i = 0; i < leftrow.length; i++){
                            row[i] = leftrow[i];
                        }
                        for(int i = 0; i < rightrow.length; i++){
                            row[i + leftrow.length] = rightrow[i];
                        }
                        joinList.add(row);
                    }
                }
            }
            joinSize = joinList.size();
            if(newRightRow){ joinIndex = 0;}
            return true;
        }

        /* Write your code here */
        return false;
    }

    // any postprocessing, if needed
    @Override
    public void close() {
        logger.trace("Closing PJoin");
        PRel left = (PRel) getLeft();
        PRel right = (PRel) getRight();
        ((PRel) left).close();
        ((PRel) right).close();
        /* Write your code here */
        return;
    }

    // returns true if there is a next row, false otherwise
    @Override
    public boolean hasNext() {
        logger.trace("Checking if PJoin has next");
        /* Write your code here */
        PRel left = (PRel) getLeft();
        PRel right = (PRel) getRight();
        if(joinIndex>=0 && joinIndex < joinSize){
            return true;
        }
        
        // fetch new row from right table
        PJoin join = (PJoin)this;
        RexNode condition = join.getCondition();
        RexCall call = (RexCall) condition;
        
        List<Integer> leftColumns = new ArrayList<>();
        List<Integer> rightColumns = new ArrayList<>();
        if (call.getKind() == SqlKind.AND) { // multiple join columns
            List<RexNode> operands = call.getOperands();
            for (RexNode operand : operands) {
                RexCall callOperand = (RexCall) operand;
                RexInputRef leftRefOperand = (RexInputRef) (callOperand.getOperands()).get(0);
                RexInputRef rightRefOperand = (RexInputRef) (callOperand.getOperands()).get(1);
                leftColumns.add(leftRefOperand.getIndex());
                rightColumns.add(rightRefOperand.getIndex() - left.getRowType().getFieldCount());
            }
        } else{ // single join column
            RexInputRef leftRef = (RexInputRef) (call.getOperands()).get(0);
            RexInputRef rightRef = (RexInputRef) (call.getOperands()).get(1);
            leftColumns.add(leftRef.getIndex());
            rightColumns.add(rightRef.getIndex() - left.getRowType().getFieldCount());
        }
        // get new row from right table
        boolean newRightRow = false;
        while(!newRightRow ){
            if(flag) break;
            Object[] rightrow;
            if(right.hasNext()){
                rightrow = right.next();
            }
            else{
                flag = true;
                break;
            }
            List<Object> key = new ArrayList<>();
            for (int i = 0; i < rightColumns.size(); i++) {
                key.add(rightrow[rightColumns.get(i)]);
            }
            // print key
            for (int i = 0; i < key.size(); i++) {
            }
            if(hashTable.containsKey(key)){
                hashTableVis.put(key, 1);
                newRightRow = true;
                List<Object[]> leftrows = hashTable.get(key);
                for(Object[] leftrow: leftrows){
                    Object[] row = new Object[leftrow.length + rightrow.length];
                    for(int i = 0; i < leftrow.length; i++){
                        row[i] = leftrow[i];
                    }
                    for(int i = 0; i < rightrow.length; i++){
                        row[i + leftrow.length] = rightrow[i];
                    }
                    joinList.add(row);
                }
            }
            else{
                // if right join
                if( join.getJoinType() == JoinRelType.RIGHT || join.getJoinType() == JoinRelType.FULL){
                    newRightRow = true;
                    Object[] row = new Object[left.getRowType().getFieldCount() + right.getRowType().getFieldCount()];
                    for(int i = 0; i < left.getRowType().getFieldCount(); i++){
                        row[i] = null;
                    }
                    for(int i = 0; i < rightrow.length; i++){
                        row[i + left.getRowType().getFieldCount()] = rightrow[i];
                    }
                    joinList.add(row);
                }
            }
            joinSize = joinList.size();
        }
        if(newRightRow ){
            joinSize = joinList.size();
            return true;
        }
        else{//no next row in right table
            // if left outer join or full outer join
            if(join.getJoinType() == JoinRelType.LEFT  || join.getJoinType() == JoinRelType.FULL){
            
                if(!flagleft){
                    for(List<Object> key: hashTable.keySet()){
                        if(hashTableVis.containsKey(key)){ // already visited
                            continue;
                        }
                        List<Object[]> leftrows = hashTable.get(key);
                        for(Object[] leftrow: leftrows){
                            Object[] row = new Object[leftrow.length + right.getRowType().getFieldCount()];
                            for(int i = 0; i < leftrow.length; i++){
                                row[i] = leftrow[i];
                            }
                            for(int i = 0; i < right.getRowType().getFieldCount(); i++){
                                row[i + leftrow.length] = null;
                            }
                            joinList.add(row);
                        }
                    }
                    flagleft = true;
                }
                joinSize = joinList.size();
            }
            if(joinIndex>= joinSize){
                return false;
            }
            joinSize = joinList.size();
            return true;
        }
    }

    // returns the next row
    @Override
    public Object[] next() {
        logger.trace("Getting next row from PJoin");
        /* Write your code here */
        joinSize = joinList.size();
        return joinList.get(joinIndex++);
        // return null;
    }
}
