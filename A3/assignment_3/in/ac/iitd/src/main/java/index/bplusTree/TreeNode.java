package index.bplusTree;

import java.nio.ByteBuffer;

// TreeNode interface - will be implemented by InternalNode and LeafNode
public interface TreeNode <T> {

    public T[] getKeys();
    public void insert(T key, int block_id);

    public int search(T key);

    // DO NOT modify this - may be used for evaluation
    default public void print() {
        T[] keys = getKeys();
        for (T key : keys) {
            System.out.print(key + " ");
        }
        return;
    }
    
    // Might be useful for you - will not be evaluated
    default public T convertBytesToT(byte[] bytes, Class<T> typeClass){
        // from storage file -- T can be: 
        // enum ColumnType {
        //     VARCHAR, INTEGER, BOOLEAN, FLOAT, DOUBLE
        // };
        /* Write your code here */
        if(typeClass.equals(String.class)){
            return (T) new String(bytes);
        }
        else if(typeClass.equals(Integer.class)){
            // convert to 4 byte integer
            int value = 0;
            for(int i=0; i<bytes.length; i++){
                value = (value << 8) | (bytes[i] & 0xFF);
            }
            return (T) new Integer(value);
        }
        else if(typeClass.equals(Boolean.class)){
            return (T) new Boolean(bytes[0] == 1);
        }
        else if(typeClass.equals(Float.class)){
            // convert to 4 byte float
            float floatValue = ByteBuffer.wrap(bytes).getFloat();
            return (T) new Float(floatValue);
        }
        else if(typeClass.equals(Double.class)){
            // convert to 8 byte double
            double doubleValue = ByteBuffer.wrap(bytes).getDouble();
            return (T) new Double(doubleValue);
        }

        System.out.println("Invalid typeClass");
        return null;
    }
    

    // manshi
    default public byte[] convertTToBytes(T key, Class<T> typeClass){
        // from storage file -- <T> can be: 
        // enum ColumnType {
        //     VARCHAR, INTEGER, BOOLEAN, FLOAT, DOUBLE
        // };
        /* Write your code here */
        if(typeClass.equals(String.class)){
            return ((String)key).getBytes();
        }
        else if(typeClass.equals(Integer.class)){
            int value = (Integer)key;
            byte[] result = new byte[4];
            for(int i=3; i>=0; i--){
                result[i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
            return result;
        }
        else if(typeClass.equals(Boolean.class)){
            byte[] result = new byte[1];
            if((Boolean)key){//true
                result[0] = 1;
            }
            else{
                result[0] = 0;
            }
            return result;
        }
        else if(typeClass.equals(Float.class)){
            float value = (Float)key;
            return ByteBuffer.allocate(4).putFloat(value).array();
        }
        else if(typeClass.equals(Double.class)){
            double value = (Double)key;
            return ByteBuffer.allocate(8).putDouble(value).array();
        }
        return null;
    }

    
    default public int compare(T key1, T key2){
        // from storage file -- <T> can be: 
        // enum ColumnType {
        //     VARCHAR, INTEGER, BOOLEAN, FLOAT, DOUBLE
        // };
        /* Write your code here */
        if(key1 instanceof String){
            return ((String)key1).compareTo((String)key2);
        }
        else if(key1 instanceof Integer){
            return ((Integer)key1).compareTo((Integer)key2);
        }
        else if(key1 instanceof Boolean){
            return ((Boolean)key1).compareTo((Boolean)key2);
        }
        else if(key1 instanceof Float){
            return ((Float)key1).compareTo((Float)key2);
        }
        else if(key1 instanceof Double){
            return ((Double)key1).compareTo((Double)key2);
        }
        System.out.println("Invalid type");
        return 0;
    }
 
}