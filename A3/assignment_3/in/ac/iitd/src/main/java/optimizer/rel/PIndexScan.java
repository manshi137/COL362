package optimizer.rel;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;

import index.bplusTree.BPlusTreeIndexFile;
import manager.StorageManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;
import java.util.Set;
import java.util.HashSet;


// Operator trigged when doing indexed scan
// Matches SFW queries with indexed columns in the WHERE clause
public class PIndexScan extends TableScan implements PRel {
    
        private final List<RexNode> projects;
        private final RelDataType rowType;
        private final RelOptTable table;
        private final RexNode filter;
    
        public PIndexScan(RelOptCluster cluster, RelTraitSet traitSet, RelOptTable table, RexNode filter, List<RexNode> projects) {
            super(cluster, traitSet, table);
            this.table = table;
            this.rowType = deriveRowType();
            this.filter = filter;
            this.projects = projects;
        }
    
        @Override
        public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
            return new PIndexScan(getCluster(), traitSet, table, filter, projects);
        }
    
        @Override
        public RelOptTable getTable() {
            return table;
        }

        @Override
        public String toString() {
            return "PIndexScan";
        }

        public String getTableName() {
            return table.getQualifiedName().get(1);
        }

        @Override
        public List<Object[]> evaluate(StorageManager storage_manager) {
            String tableName = getTableName();
            System.out.println("Evaluating PIndexScan for table: " + tableName);

            /* Write your code here */
            List<Object[]> result = new ArrayList<>();
            // DB db = storage_manager.getDB();
            if(!storage_manager.check_file_exists(tableName)){
                System.out.println("Table does not exist");
                return null;

            }
            System.out.println("rowtype: " + rowType);
            System.out.println("filter: " + filter);

            String opString = ((RexCall) filter).getOperator().getName();
            System.out.println("opString: " + opString);
            String s = ((RexCall)filter).getOperands().get(0).toString();
            // 
            String temp = s.substring(1);
            // System.out.println("s: " + temp);

            int column = Integer.parseInt(temp);
            System.out.println("column: " + column);
            RexCall op = (RexCall) filter;
            RexLiteral rexValue = (RexLiteral) op.getOperands().get(1);
            System.out.println("rexvalue: " + rexValue.getValue());
            

            String colName = rowType.getFieldList().get(column).getName();
            System.out.println("colName: " + colName);

            String indexFile = tableName + "_" + colName + "_index";
            if(!storage_manager.check_file_exists(indexFile)){
                System.out.println("Index file does not exist");
                return null;
            }
            
            byte[] metadataBlock = storage_manager.get_data_block(tableName, 0);
            int colOffset = (metadataBlock[2*(column+1)] & 0xFF) | (metadataBlock[1 + 2*(column+1)] << 8); // little endian
            byte columnTypeByte = metadataBlock[colOffset];
            Object typeObj = getColumnType(columnTypeByte);

            Set<Integer> blockIdsSet = new HashSet<>();
            if(opString.contains("=")){
                int node_id = storage_manager.search(tableName, colName, rexValue);
                System.out.println("node_id: " + node_id);
                System.out.println("EQUALS");
                if(node_id == -1){
                    System.out.println("Value not found");
                    return null;
                }
                
                byte[] nodeData = storage_manager.get_data_block(indexFile, node_id);
                System.out.println("nodeData: " + nodeData);
                Object[] keys = getKeys(nodeData, typeObj.getClass());
                
                int[] blockIds = getBlockIds(nodeData);
                System.out.println("keys length: " + keys.length);
                System.out.println("blockids length: " + blockIds.length);
                
                while (node_id!=0) {
                    System.out.println("node_id: " + node_id);
                    for( int i=0; i<keys.length; i++){
                        System.out.println("keys[i]: " + keys[i]);
                        if( compare(keys[i], rexValue.getValue(), typeObj) == 0){
                            blockIdsSet.add(blockIds[i]);
                            // blockIdsSet.add(blockIds[i+1]);
                        }
                    }
                    node_id = nextLeafId(nodeData);
                    System.out.println("next node_id: " + node_id);
                    nodeData = storage_manager.get_data_block(indexFile, node_id);
                    keys = getKeys(nodeData, typeObj.getClass());
                    blockIds = getBlockIds(nodeData);
                }
            }

            if(opString.contains("<")){
                int node_id = storage_manager.search(tableName, colName, rexValue);
                System.out.println("LESS THAN");
                if(node_id == -1){
                    System.out.println("Value not found");
                    return null;
                }
                byte[] nodeData = storage_manager.get_data_block(indexFile, node_id);
                Object[] keys = getKeys(nodeData, typeObj.getClass());
                int[] blockIds = getBlockIds(nodeData);

                while (node_id!=0) {
                    for( int i=0; i<keys.length; i++){
                        if( compare(keys[i], rexValue.getValue(), typeObj) < 0){
                            blockIdsSet.add(blockIds[i]);
                            // blockIdsSet.add(blockIds[i+1]);
                        }
                    }
                    node_id = prevLeafId(node_id, indexFile, storage_manager);
                    nodeData = storage_manager.get_data_block(indexFile, node_id);
                    keys = getKeys(nodeData, typeObj.getClass());
                    blockIds = getBlockIds(nodeData);
                }

            }
            else if(opString.contains(">")){
                int node_id = storage_manager.search(tableName, colName, rexValue);
                System.out.println("GREATER THAN");
                if(node_id == -1){
                    System.out.println("Value not found");
                    return null;
                }
                byte[] nodeData = storage_manager.get_data_block(indexFile, node_id);
                Object[] keys = getKeys(nodeData, typeObj.getClass());
                int[] blockIds = getBlockIds(nodeData);

                while (node_id!=0) {
                    for( int i=0; i<keys.length; i++){
                        if( compare(keys[i], rexValue.getValue(), typeObj) > 0){
                            blockIdsSet.add(blockIds[i]);
                            // blockIdsSet.add(blockIds[i+1]);
                        }
                    }
                    node_id = nextLeafId(nodeData);
                    nodeData = storage_manager.get_data_block(indexFile, node_id);
                    keys = getKeys(nodeData, typeObj.getClass());
                    blockIds = getBlockIds(nodeData);
                }

            }
            // ---------now we have blockids that we need to scan
            for(Integer blockid : blockIdsSet){
                List<Object[]> records = storage_manager.get_records_from_block(tableName, blockid);
                for(Object[] record : records){
                    if(opString.contains(opString)){
                        if(compare(record[column], rexValue.getValue(), typeObj) == 0){
                            result.add(record);
                        }
                    }
                    if(opString.contains("<")){
                        if(compare(record[column], rexValue.getValue(), typeObj) < 0){
                            result.add(record);
                        }
                    }
                    else if(opString.contains(">")){
                        if(compare(record[column], rexValue.getValue(), typeObj) > 0){
                            result.add(record);
                        }
                    }
                }
            }
            return result;
        }
        // manshi----------------------------------------------------------------------------------------------------------------------------------
        Object getColumnType(byte dataTypeByte) {
            switch (dataTypeByte) {
                case 0: // Assuming 0 represents VARCHAR
                    return new String();
                case 1: // Assuming 1 represents INTEGER
                    return new Integer(0);
                case 2: // Assuming 2 represents BOOLEAN
                    return new Boolean(false);
                case 3: // Assuming 3 represents FLOAT
                    return new Float(0);
                case 4: // Assuming 4 represents DOUBLE
                    return new Double(0);
                default:
                    throw new IllegalArgumentException("Invalid byte value for datatype");
            }
        }
        
        Object[] getKeys(byte[] leafData, Class<?> typeClass) {
            int numKeys = getNumKeys(leafData);
            // System.out.println("numKeys: " + numKeys);
            Object[] keys = new Object[numKeys];
            /* Write your code here */
            int lenKeyOffset = 10;
            int keyOffset = 12;
            for(int i=0;i < numKeys; i++){
                byte[] lenKeyBytes = get_data(leafData, lenKeyOffset, 2);
                int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);
    
                byte[] keyBytes = get_data(leafData, keyOffset, lenKey);
                for(int b: keyBytes){
                    System.out.print(b + ", ");
                }
                keys[i] = convertBytesToObject(keyBytes,typeClass);
    
                lenKeyOffset += 2 + lenKey + 2;
                keyOffset += lenKey + 2 + 2;
            }
            return keys;
        }
    
        int[] getBlockIds(byte[] leafData) {
            int numKeys = getNumKeys(leafData);
            int[] block_ids = new int[numKeys];
    
            /* Write your code here */
            int blockIdOffset = 8;
            int lenKeyOffset = 10;
            for(int i=0; i<numKeys; i++){
                byte[] blockIdBytes = get_data(leafData, blockIdOffset, 2);
                int blockId = (blockIdBytes[0] << 8) | (blockIdBytes[1] & 0xFF);
                block_ids[i] = blockId;
    
                byte[] lenKeyByte = get_data(leafData, lenKeyOffset, 2);
                int lenKey = (lenKeyByte[0] << 8) | (lenKeyByte[1] & 0xFF);
                
                blockIdOffset += 2 + 2 + lenKey;
                lenKeyOffset += 2 + lenKey + 2;
            }
    
            return block_ids;
        }
        
        public int getNumKeys(byte[] data) {
            byte[] numKeysBytes = get_data(data, 0, 2);
            return (numKeysBytes[0] << 8) | (numKeysBytes[1] & 0xFF);
        }
        
        public byte[] get_data(byte[] data, int offset, int length) {
            if(offset + length > data.length){
                return null;
            }
            byte[] result = new byte[length];
            System.arraycopy(data, offset, result, 0, length);
            return result;
        }
        
        public Object convertBytesToObject(byte[] bytes, Class<?> typeClass){
            // if(typeClass.equals(String.class)){
            //     return new String(bytes);
            // }
            // else if(typeClass.equals(Integer.class)){
            //     int value = 0;
            //     for(int i=0; i<4; i++){
            //         value = (value << 8) | (bytes[i] & 0xFF);
            //     }
            //     return new Integer(value);
            // }
            // else if(typeClass.equals(Boolean.class)){
            //     return new Boolean(bytes[0] == 1);
            // }
            // else if(typeClass.equals(Float.class)){
            //     float floatValue = ByteBuffer.wrap(bytes).getFloat();
            //     return new Float(floatValue);
            // }
            // else if(typeClass.equals(Double.class)){
            //     double doubleValue = ByteBuffer.wrap(bytes).getDouble();
            //     return new Double(doubleValue);
            // }
            if(typeClass.equals(String.class)){
                return new String(bytes);
            }
            else if(typeClass.equals(Integer.class)){
                // convert to 4 byte integer
                int value = 0;
                for(int i=0; i<bytes.length; i++){
                    value = (value << 8) | (bytes[i] & 0xFF);
                }
                return new Integer(value);
            }
            else if(typeClass.equals(Boolean.class)){
                return new Boolean(bytes[0] == 1);
            }
            else if(typeClass.equals(Float.class)){
                // convert to 4 byte float
                float floatValue = ByteBuffer.wrap(bytes).getFloat();
                return new Float(floatValue);
            }
            else if(typeClass.equals(Double.class)){
                // convert to 8 byte double
                double doubleValue = ByteBuffer.wrap(bytes).getDouble();
                return new Double(doubleValue);
            }

            System.out.println("Invalid typeClass");
            return null;
        }
        
        String[] extractStrings(String s2) {
            String regex = "^(.*?)\\(\\$([a-zA-Z0-9]+),\\s*([a-zA-Z0-9]+)\\)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(s2);
            if (matcher.find()) {
                String s1 = matcher.group(1);
                String val1 = matcher.group(2);
                String val2 = matcher.group(3);
                return new String[]{s1, val1, val2};
            } else {
                // Return an empty array or handle the case where no match is found
                return new String[]{"", "", ""};
            }
        }
        
        public int nextLeafId(byte[] nodeData){
            // byte[] nodeData = storage_manager.get_data_block(tableName, node_id);
            byte[] nextLeafIdBytes = get_data(nodeData , 4, 2);
            return (nextLeafIdBytes[0] << 8) | (nextLeafIdBytes[1] & 0xFF);
        }
        
        public int prevLeafId( int node_id, String tableName, StorageManager storage_manager){
            byte[] nodeData = storage_manager.get_data_block(tableName, node_id);
            byte[] prevLeafIdBytes = get_data(nodeData, 2, 2);
            return (prevLeafIdBytes[0] << 8) | (prevLeafIdBytes[1] & 0xFF);
        }

        int compare(Object obj1, Object obj2, Object typeObj){
            if(typeObj instanceof String){
                String s2 =obj2.toString();
                return ((String)obj1).compareTo(s2);
            }
            else if(typeObj instanceof Integer){
                String s2 = obj2.toString();
                int obj2Int = Integer.parseInt(s2);

                return Integer.compare((Integer)obj1, obj2Int);
            }
            else if(typeObj instanceof Boolean){
                String s2 = obj2.toString();
                Boolean obj2Bool = Boolean.parseBoolean(s2);
                return Boolean.compare((Boolean)obj1, obj2Bool) ;
            }
            else if(typeObj instanceof Float){
                String s2 = obj2.toString();
                Float obj2Float = Float.parseFloat(s2);
                return Float.compare((Float)obj1, obj2Float) ;
            }
            else if(typeObj instanceof Double){
                String s2 = obj2.toString();
                Double obj2Double = Double.parseDouble(s2);
                return Double.compare((Double)obj1, obj2Double) ;
            }
            return 0;
        }    
}