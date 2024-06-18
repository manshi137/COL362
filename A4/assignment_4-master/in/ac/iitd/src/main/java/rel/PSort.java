package rel;

import java.util.List;
import java.util.ArrayList;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.hint.RelHint;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rel.RelFieldCollation;
import java.math.BigDecimal;

import convention.PConvention;

public class PSort extends Sort implements PRel{
    
    public PSort(
            RelOptCluster cluster,
            RelTraitSet traits,
            List<RelHint> hints,
            RelNode child,
            RelCollation collation,
            RexNode offset,
            RexNode fetch
            ) {
        super(cluster, traits, hints, child, collation, offset, fetch);
        assert getConvention() instanceof PConvention;
    }

    @Override
    public Sort copy(RelTraitSet traitSet, RelNode input, RelCollation collation, RexNode offset, RexNode fetch) {
        return new PSort(getCluster(), traitSet, hints, input, collation, offset, fetch);
    }

    @Override
    public String toString() {
        return "PSort";
    }

    // returns true if successfully opened, false otherwise
    List<Object[]> sortedList = new ArrayList<>();
    int index = -1;
    int length = 0;
    @Override
    public boolean open(){
        logger.trace("Opening PSort");
        /* Write your code here */
        PRel child = (PRel)this.getInput();
        if(child.open()){
            index = 0 ;
            while(child.hasNext()){
                sortedList.add(child.next());
                length ++;
            }
            int fetch = -1 ;
            if(this.fetch != null ){
                Object fetchvalue = ((RexLiteral)this.fetch).getValue();
                if (fetchvalue instanceof BigDecimal) {
                    fetch = ((BigDecimal) fetchvalue).intValue();
                }
            }
            if(fetch!= -1){
                if(fetch < length){
                    length = fetch;
                }
            }
            PSort sortNode = (PSort) this;
            RelCollation collation = sortNode.collation;

            sortedList.sort((o1, o2) -> {
                for (RelFieldCollation fieldCollation : collation.getFieldCollations()) {
                    int fieldIndex = fieldCollation.getFieldIndex(); // Column index
                    RelFieldCollation.Direction direction = fieldCollation.getDirection(); // Sort direction
                    RelFieldCollation.NullDirection nullDirection = fieldCollation.nullDirection; // Nulls sorting order
                    // System.out.println("Column Index: " + fieldIndex + ", Direction: " + direction + ", Nulls Direction: " + nullDirection);
                    if (direction == RelFieldCollation.Direction.ASCENDING) {
                        if (((Comparable) o1[fieldIndex]).compareTo(o2[fieldIndex]) != 0) {
                            return ((Comparable) o1[fieldIndex]).compareTo(o2[fieldIndex]);
                        }
                    } else {
                        if (((Comparable) o2[fieldIndex]).compareTo(o1[fieldIndex]) != 0) {
                            return ((Comparable) o2[fieldIndex]).compareTo(o1[fieldIndex]);
                        }
                    }
                }
                return 0;
            });
            return true;
        }
        return false;
    }

    // any postprocessing, if needed
    @Override
    public void close(){
        logger.trace("Closing PSort");
        /* Write your code here */
        PRel child = (PRel)this.getInput();
        child.close();
        return;
    }

    // returns true if there is a next row, false otherwise
    @Override
    public boolean hasNext(){
        logger.trace("Checking if PSort has next");
        /* Write your code here */
        if(index < length){
            return true;
        }
        return false;
    }

    // returns the next row
    @Override
    public Object[] next(){
        logger.trace("Getting next row from PSort");
        /* Write your code here */
        if(index < length){
            return sortedList.get(index++);
        }
        return null;
    }

}
