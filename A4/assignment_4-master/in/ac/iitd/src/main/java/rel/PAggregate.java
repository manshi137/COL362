package rel;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.hint.RelHint;
import org.apache.calcite.util.ImmutableBitSet;

import convention.PConvention;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Count, Min, Max, Sum, Avg
public class PAggregate extends Aggregate implements PRel {

    public PAggregate(
            RelOptCluster cluster,
            RelTraitSet traitSet,
            List<RelHint> hints,
            RelNode input,
            ImmutableBitSet groupSet,
            List<ImmutableBitSet> groupSets,
            List<AggregateCall> aggCalls) {
        super(cluster, traitSet, hints, input, groupSet, groupSets, aggCalls);
        assert getConvention() instanceof PConvention;
    }

    @Override
    public Aggregate copy(RelTraitSet traitSet, RelNode input, ImmutableBitSet groupSet,
                          List<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls) {
        return new PAggregate(getCluster(), traitSet, hints, input, groupSet, groupSets, aggCalls);
    }

    @Override
    public String toString() {
        return "PAggregate";
    }

    // returns true if successfully opened, false otherwise
    int sortindex = -1, aggrindex = -1;
    int sortlength = 0, aggrlength = 0;
    List<Object[]> sortedList = new ArrayList<>();
    List<Object[]> aggregatedList = new ArrayList<>();
    @Override
    public boolean open() {
        logger.trace("Opening PAggregate");
        /* Write your code here */
        PRel input = (PRel) getInput();
        if (input.open()) {
            sortindex = 0;
            while(input.hasNext()) {
                sortedList.add(input.next()); sortlength++;
            }
            Aggregate aggregate = (Aggregate)this;
            ImmutableBitSet groupSet = aggregate.getGroupSet();
            List<AggregateCall> aggCalls = aggregate.getAggCallList();
            
            // multiple aggregate functions 
            List<List<Object>> tmpAggregatedList = new ArrayList<>();
            for(int ag=0; ag< aggCalls.size(); ag++){
                AggregateCall aggCall = aggCalls.get(ag);
                // System.out.println("Aggregation Function: " + aggCall.getAggregation());
                // System.out.println("Column Index: " + aggCall.getArgList());
                
                Map< List<Object>, List<Object> > groupMap = new HashMap<>();
                for( Object[] row: sortedList){
                    List<Object> groupKey = new ArrayList<>();
                    for( int i = 0; i < groupSet.length(); i++ ) {
                        groupKey.add(row[groupSet.nth(i)]);
                    }
                    if(!groupMap.containsKey(groupKey)){
                        groupMap.put(groupKey, new ArrayList<>());
                    }
                    if(aggCall.getArgList().size()>0){
                        groupMap.get(groupKey).add(row[aggCall.getArgList().get(0)]);
                    }
                    else{
                        groupMap.get(groupKey).add(null);
                    }
                }
                List<List<Object>> keySet = new ArrayList<>(groupMap.keySet());

                if(ag==0){// add keys in beginning, keep appending aggregate values later
                    tmpAggregatedList = keySet;
                }

                for(int k=0; k<keySet.size(); k++){
                    List<Object> key = keySet.get(k);
                    List<Object> values = groupMap.get(key);

                    Object aggValue = null;
                    if(aggCall.getAggregation().getName().equals("COUNT")){
                        aggValue = values.size();
                    } else if(aggCall.getAggregation().getName().equals("MIN")){
                        aggValue = groupMap.get(key).get(0);
                        for(Object value : values){
                            if(compare(value, aggValue) < 0){
                                aggValue = value;
                            }
                        }
                    } else if(aggCall.getAggregation().getName().equals("MAX")){
                        aggValue = groupMap.get(key).get(0);
                        for(Object value : values){
                            if(compare(value, aggValue) > 0){
                                aggValue = value;
                            }
                        }
                    } else if(aggCall.getAggregation().getName().equals("SUM")){
                        aggValue = 0;
                        for(Object value : values){
                            if(value instanceof Integer){
                                aggValue = (Integer)aggValue + (Integer)value;
                            } else if(value instanceof Double){
                                aggValue = (Double)aggValue + (Double)value;
                            }
                        }
                    } else if(aggCall.getAggregation().getName().equals("AVG")){
                        aggValue = 0.0;
                        for(Object value : values){
                            if(value instanceof Integer){
                                aggValue = (Double)aggValue + (Integer)value;
                            } else if(value instanceof Double){
                                aggValue = (Double)aggValue + (Double)value;
                            }
                        }
                        aggValue = (Double)aggValue / values.size();
                    }
                    tmpAggregatedList.get(k).add(aggValue);
                }
            }
            for(List<Object> row: tmpAggregatedList){
                aggregatedList.add(row.toArray());
            }
            aggrindex =0;
            aggrlength = aggregatedList.size();
            return true;
        }
        return false;
    }
    public int compare(Object val1, Object val2){
        /* Write your code here */
        if(val1 instanceof Integer){
            return ((Integer)val1).compareTo((Integer)val2);
        }
        if(val1 instanceof Double){
            return ((Double)val1).compareTo((Double)val2);
        }
        if(val1 instanceof String){
            return ((String)val1).compareTo((String)val2);
        }
        if(val1 instanceof Boolean){
            return ((Boolean)val1).compareTo((Boolean)val2);
        }
        if(val1 instanceof Float){
            return ((Float)val1).compareTo((Float)val2);
        }
        System.out.println("Invalid type -------- compare function incomp");
        return 0;
    }

    // any postprocessing, if needed
    @Override
    public void close() {
        logger.trace("Closing PAggregate");
        /* Write your code here */
        PRel input = (PRel) getInput();
        input.close();
        return;
    }

    // returns true if there is a next row, false otherwise
    @Override
    public boolean hasNext() {
        logger.trace("Checking if PAggregate has next");
        /* Write your code here */
        if(aggrindex>=0 && aggrindex < aggrlength ){
            return true;
        }
        return false;
    }

    // returns the next row
    @Override
    public Object[] next() {
        logger.trace("Getting next row from PAggregate");
        if(aggrindex>=0 && aggrindex < aggrlength){
            return aggregatedList.get(aggrindex++);
        }
        return null;
    }

}