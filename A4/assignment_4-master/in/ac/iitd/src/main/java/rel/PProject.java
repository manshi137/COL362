package rel;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import convention.PConvention;

import java.math.BigDecimal;
import java.util.List;

// Hint: Think about alias and arithmetic operations
public class PProject extends Project implements PRel {

    public PProject(
            RelOptCluster cluster,
            RelTraitSet traits,
            RelNode input,
            List<? extends RexNode> projects,
            RelDataType rowType) {
        super(cluster, traits, ImmutableList.of(), input, projects, rowType);
        assert getConvention() instanceof PConvention;
    }

    @Override
    public PProject copy(RelTraitSet traitSet, RelNode input,
                            List<RexNode> projects, RelDataType rowType) {
        return new PProject(getCluster(), traitSet, input, projects, rowType);
    }

    @Override
    public String toString() {
        return "PProject";
    }

    // returns true if successfully opened, false otherwise
    
    @Override
    public boolean open(){
        logger.trace("Opening PProject");
        /* Write your code here */
        PRel input = (PRel)this.getInput();
        if(((PRel)input).open()){
            return true;
        }
        return false;
    }

    // any postprocessing, if needed
    @Override
    public void close(){
        logger.trace("Closing PProject");
        /* Write your code here */
        PRel input = (PRel)this.getInput();
        ((PRel)input).close();
        return;
    }

    // returns true if there is a next row, false otherwise
    @Override
    public boolean hasNext(){
        logger.trace("Checking if PProject has next");
        /* Write your code here */
        PRel input = (PRel)this.getInput();
        if(((PRel)input).hasNext()){
            return true;
        }
        return false;
    }

    // returns the next row
    @Override
    public Object[] next(){
        logger.trace("Getting next row from PProject");
        /* Write your code here */
        PRel input = (PRel)this.getInput();
        PRel pProject = input;

        Object[] inputrow = pProject.next();

        List<RexNode> projects = this.getProjects();
      
        Object[] outputrow = new Object[projects.size()];
        for(int i = 0; i < projects.size(); i++){
            outputrow[i] = calculateRow(projects.get(i), inputrow);
        }
        return outputrow;
    }

    Object calculateRow(RexNode project, Object[] inputrow){
        //check if projects is an expression
        if(project instanceof org.apache.calcite.rex.RexCall){
            // get operator 
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall)project;
            if(call.getKind() == org.apache.calcite.sql.SqlKind.PLUS){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).add( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.TIMES){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).multiply( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.MINUS){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).subtract( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.DIVIDE){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).divide( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.EQUALS){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).equals( (BigDecimal)right);
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.GREATER_THAN){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) > 0;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.LESS_THAN){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) < 0;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.GREATER_THAN_OR_EQUAL){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) >= 0;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.LESS_THAN_OR_EQUAL){
                Object left = calculateRow(call.getOperands().get(0), inputrow);
                Object right = calculateRow(call.getOperands().get(1), inputrow);
                return ((BigDecimal)left).compareTo( (BigDecimal)right) <= 0;
            }
            // else if(call.getKind() == org.apache.calcite.sql.SqlKind.NOT_EQUALS){
            //     System.out.println("projects is an expression");
            //     Object left = calculateRow(call.getOperands().get(0), inputrow);
            //     Object right = calculateRow(call.getOperands().get(1), inputrow);
            //     return !((BigDecimal)left).equals( (BigDecimal)right);
            // }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.AND){
                for(int i = 0; i < call.getOperands().size(); i++){
                    if(!(boolean)calculateRow(call.getOperands().get(i), inputrow)){
                        return false;
                    }
                }
                return true;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.OR){
                for(int i = 0; i < call.getOperands().size(); i++){
                    if((boolean)calculateRow(call.getOperands().get(i), inputrow)){
                        return true;
                    }
                }
                return false;
            }
            else if(call.getKind() == org.apache.calcite.sql.SqlKind.NOT){
                return !((boolean)calculateRow(call.getOperands().get(0), inputrow));
            }
            else{
                return null;
            }
        }
        else if(project instanceof org.apache.calcite.rex.RexInputRef){
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
            Object value = ((org.apache.calcite.rex.RexLiteral)project).getValue();
            return value;
        }
        else{
            System.out.println("project type unknown -------" + project.getClass());
            return null;
        }
    }
}
