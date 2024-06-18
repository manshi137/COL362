package rel;

import javax.swing.text.Style;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rex.RexNode;

import convention.PConvention;

import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.NlsString;

import java.math.BigDecimal;

public class PFilter extends Filter implements PRel {

    public PFilter(
            RelOptCluster cluster,
            RelTraitSet traits,
            RelNode child,
            RexNode condition) {
        super(cluster, traits, child, condition);
        assert getConvention() instanceof PConvention;
    }

    @Override
    public Filter copy(RelTraitSet traitSet, RelNode input, RexNode condition) {
        return new PFilter(getCluster(), traitSet, input, condition);
    }

    @Override
    public String toString() {
        return "PFilter";
    }

    // returns true if successfully opened, false otherwise
    @Override 
    public boolean open(){
        logger.trace("Opening PFilter");
        /* Write your code here */
        PRel child = (PRel)this.getInput();
        if(child.open()){
            return true;
        }
        return false;
    }

    // any postprocessing, if needed
    @Override
    public void close(){
        logger.trace("Closing PFilter");
        /* Write your code here */
        PRel child = (PRel)this.getInput();
        ((PRel)child).close();
        return;
    }

    // returns true if there is a next row, false otherwise
    private Object[] nextoutput = null;
    @Override
    
    public boolean hasNext(){
        logger.trace("Checking if PFilter has next");
        /* Write your code here */
        PRel child = (PRel)this.getInput();
        Object[] row ;
        if( ((PRel)child).hasNext()){
            row = ((PRel)child).next();
        }
        else{
            return false;
        }
        RexNode condition = this.getCondition();
        // System.out.println("condition" + condition);
       
        while(row != null ){
            boolean cond = checkCondition(row, condition);
            if(cond){
                nextoutput = row;
                return true;
            }
            if(child.hasNext()){
                row = ((PRel)child).next();
            }
            else{
                return false;
            }
        }
        return false;
    }
    boolean checkCondition(Object[] row, RexNode condition){
        // if condition is null 
        if(condition == null){
            return true;}
        if( condition instanceof RexCall){
            RexCall call = (RexCall)condition;
            SqlKind kind = call.getKind();
            if(kind == SqlKind.AND){
                for(int i = 0; i < call.getOperands().size(); i++){
                    if(!checkCondition(row, call.getOperands().get(i))){
                        return false;
                    }
                }
                return true;
            }
            else if(kind == SqlKind.OR){
                for(int i = 0; i < call.getOperands().size(); i++){
                    if(checkCondition(row, call.getOperands().get(i))){
                        return true;
                    }
                }
                return false;
            }
            else if(kind == SqlKind.NOT){
                return !checkCondition(row, call.getOperands().get(0));
            }
            if(call.getOperands().get(0) instanceof RexInputRef && call.getOperands().get(1) instanceof RexInputRef){
                int column1 = ((RexInputRef)call.getOperands().get(0)).getIndex();
                int column2 = ((RexInputRef)call.getOperands().get(1)).getIndex();
                // return compare(row[column1], row[column2]) <= 0;
                if(kind == SqlKind.EQUALS){
                    return compare(row[column1], row[column2]) == 0;
                }
                if(kind == SqlKind.GREATER_THAN){
                    return compare(row[column1], row[column2]) > 0;
                }
                if(kind == SqlKind.GREATER_THAN_OR_EQUAL){
                    return compare(row[column1], row[column2]) >= 0;
                }
                if(kind == SqlKind.LESS_THAN){
                    return compare(row[column1], row[column2]) < 0;
                }
                if(kind == SqlKind.LESS_THAN_OR_EQUAL){
                    return compare(row[column1], row[column2]) <= 0;
                }
                if(kind == SqlKind.NOT_EQUALS){
                    return compare(row[column1], row[column2]) != 0;
                }
            } else if (call.getOperands().get(0) instanceof RexInputRef){
                int column = ((RexInputRef)call.getOperands().get(0)).getIndex();
                Object value = ((RexLiteral)call.getOperands().get(1)).getValue(); 
                if(kind == SqlKind.EQUALS){
                    return compare(row[column], value) == 0;
                }
                if(kind == SqlKind.GREATER_THAN){
                    return compare(row[column], value) > 0;
                }
                if(kind == SqlKind.GREATER_THAN_OR_EQUAL){
                    return compare(row[column], value) >= 0;
                }
                if(kind == SqlKind.LESS_THAN){
                    return compare(row[column], value) < 0;
                }
                if(kind == SqlKind.LESS_THAN_OR_EQUAL){
                    return compare(row[column], value) <= 0;
                }
                if(kind == SqlKind.NOT_EQUALS){
                    return compare(value, row[column]) != 0;
                }
            }
            else if( call.getOperands().get(1) instanceof RexInputRef){
                int column = ((RexInputRef)call.getOperands().get(1)).getIndex();
                Object value = ((RexLiteral)call.getOperands().get(0)).getValue(); 
                if(kind == SqlKind.EQUALS){
                    return compare(value, row[column]) == 0;
                }
                if(kind == SqlKind.GREATER_THAN){
                    return compare(value, row[column]) > 0;
                }
                if(kind == SqlKind.GREATER_THAN_OR_EQUAL){
                    return compare(value, row[column]) >= 0;
                }
                if(kind == SqlKind.LESS_THAN){
                    return compare(value, row[column]) < 0;
                }
                if(kind == SqlKind.LESS_THAN_OR_EQUAL){
                    return compare(value, row[column]) <= 0;
                }
                if(kind == SqlKind.NOT_EQUALS){
                    return compare(value, row[column]) != 0;
                }
            } else if( call.getOperands().get(0) instanceof RexCall || call.getOperands().get(1) instanceof RexCall){
                Object value1 = calculateRow(call.getOperands().get(0), row);
                Object value2 = calculateRow(call.getOperands().get(1), row);
                if(kind == SqlKind.EQUALS){
                    return compare(value1, value2) == 0;
                }
                if(kind == SqlKind.GREATER_THAN){
                    return compare(value1, value2) > 0;
                }
                if(kind == SqlKind.GREATER_THAN_OR_EQUAL){
                    return compare(value1, value2) >= 0;
                }
                if(kind == SqlKind.LESS_THAN){
                    return compare(value1, value2) < 0;
                }
                if(kind == SqlKind.LESS_THAN_OR_EQUAL){
                    return compare(value1, value2) <= 0;
                }
                if(kind == SqlKind.NOT_EQUALS){
                    return compare(value1, value2) != 0;
                }
            }
            else{ 
                // System.out.println("type of operands" + call.getOperands().getClass());
                // System.out.println(call);
                System.out.println("Invalid type -------- compare function incomp");
            }
        
            
        }
        return false;
    }
    Object calculateRow(RexNode project, Object[] inputrow){
        //check if projects is an expression
        if(project instanceof org.apache.calcite.rex.RexCall){
            // get operator 
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall)project;
            if(call.getKind() == org.apache.calcite.sql.SqlKind.PLUS){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).add( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.TIMES){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                System.out.println(left.getClass() + " " + right.getClass());
                return ((BigDecimal)left).multiply( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.MINUS){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).subtract( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.DIVIDE){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).divide( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.EQUALS){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).equals( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.GREATER_THAN){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) > 0;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.LESS_THAN){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) < 0;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.GREATER_THAN_OR_EQUAL){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) >= 0;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.LESS_THAN_OR_EQUAL){
                // System.out.println("projects is an expression");
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) <= 0;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.AND){
                // System.out.println("projects is an expression");
                for(int i = 0; i < call.getOperands().size(); i++){
                    if(!(boolean)calculateRow(call.getOperands().get(i), inputrow)){
                        return false;
                    }
                }
                return true;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.OR){
                // System.out.println("projects is an expression");
                for(int i = 0; i < call.getOperands().size(); i++){
                    if((boolean)calculateRow(call.getOperands().get(i), inputrow)){
                        return true;
                    }
                }
                return false;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.NOT){
                // System.out.println("projects is an expression");
                return !((boolean)calculateRow(call.getOperands().get(0), inputrow));
            }
            else{
                // System.out.println("projects is an expression");
                // System.out.println("operator" + call.getKind());
                return null;
            }
        }
        else if(project instanceof org.apache.calcite.rex.RexInputRef){
            // System.out.println("projects is an input ref");
            int index = ((org.apache.calcite.rex.RexInputRef)project).getIndex();
            if(inputrow[index] instanceof Integer  ){
                BigDecimal bd = new BigDecimal((int)inputrow[index]);
                return bd;
            }
            else if(inputrow[index] instanceof Double){
                BigDecimal bd = new BigDecimal((double)inputrow[index]);
                return bd;
            }
            return inputrow[index];
        }
        else if(project instanceof org.apache.calcite.rex.RexLiteral){
            // System.out.println("projects is a literal");
            Object value = ((org.apache.calcite.rex.RexLiteral)project).getValue();
            return value;
        }
        else{
            System.out.println("project type unknown -------" + project.getClass());
            return null;
        }
    }
    public int compare(Object val1, Object val2){
        /* Write your code here */
        // System.out.println("compare function");
        BigDecimal bd1 = null, bd2= null;
        NlsString ns1 = null, ns2= null;
        if(val1 instanceof Integer ){
            bd1 = new BigDecimal((int)val1);
        }
        else if(val1 instanceof Double){
            bd1 = new BigDecimal((double)val1);
        }
        else if(val1 instanceof BigDecimal){
            bd1 = (BigDecimal)val1;
        }

        if(val2 instanceof Integer){
            bd2 = new BigDecimal((int)val2);
        }
        else if(val2 instanceof Double){
            bd2 = new BigDecimal((double)val2);
        }
        else if(val2 instanceof BigDecimal){
            bd2 = (BigDecimal)val2;
        }
        if(bd1 != null && bd2 != null){
            return bd1.compareTo(bd2);
        }

        if(val1 instanceof NlsString){
            ns1 = (NlsString)val1;
        }
        else if(val1 instanceof String){
            ns1 = new NlsString((String)val1, null, null);
        }

        if(val2 instanceof NlsString){
            ns2 = (NlsString)val2;
        }
        else if(val2 instanceof String){
            ns2 = new NlsString((String)val2, null, null);
        }

        if(ns1 != null && ns2 != null){
            return ns1.compareTo(ns2);
        }
        // // // ---------------------------
        System.out.println("Invalid type -------- compare function incomp");
        return 0;
    }
    
    // returns the next row
    // Hint: Try looking at different possible filter conditions
    @Override
    public Object[] next(){
        logger.trace("Getting next row from PFilter");
        /* Write your code here */
        return nextoutput;
    }
}
