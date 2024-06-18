package index.bplusTree;

import storage.AbstractFile;

import java.util.Queue;
import java.nio.ByteBuffer; 
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/*
    * Tree is a collection of BlockNodes
    * The first BlockNode is the metadata block - stores the order and the block_id of the root node

    * The total number of keys in all leaf nodes is the total number of records in the records file.
*/

public class BPlusTreeIndexFile<T> extends AbstractFile<BlockNode> {

    Class<T> typeClass;

    // Constructor - creates the metadata block and the root node
    public BPlusTreeIndexFile(int order, Class<T> typeClass) {
        
        super();
        this.typeClass = typeClass;
        BlockNode node = new BlockNode(); // the metadata block
        LeafNode<T> root = new LeafNode<>(typeClass);

        // 1st 2 bytes in metadata block is order
        byte[] orderBytes = new byte[2];
        orderBytes[0] = (byte) (order >> 8);
        orderBytes[1] = (byte) order;
        node.write_data(0, orderBytes);

        // next 2 bytes are for root_node_id, here 1
        byte[] rootNodeIdBytes = new byte[2];
        rootNodeIdBytes[0] = 0;
        rootNodeIdBytes[1] = 1;
        node.write_data(2, rootNodeIdBytes);

        // push these nodes to the blocks list
        blocks.add(node);
        blocks.add(root);
    }

    private boolean isFull(int id){
        // 0th block is metadata block
        assert(id > 0);
        return blocks.get(id).getNumKeys() == getOrder() - 1;
    }

    private int getRootId() {
        BlockNode node = blocks.get(0);
        byte[] rootBlockIdBytes = node.get_data(2, 2);
        return ((rootBlockIdBytes[0] << 8) & 0xFF) | (rootBlockIdBytes[1] & 0xFF);
    }

    public int getOrder() {
        BlockNode node = blocks.get(0);
        byte[] orderBytes = node.get_data(0, 2);
        return ((orderBytes[0] << 8) & 0xFF) | (orderBytes[1] & 0xFF);
    }

    private boolean isLeaf(BlockNode node){
        return node instanceof LeafNode;
    }

    private boolean isLeaf(int id){
        return isLeaf(blocks.get(id));
    }

    // will be evaluated
    public void insert(T key, int block_id) {

        /* Write your code here */
        System.out.println("\nInserting key: "+key);
        int rootId = getRootId();
        // int parent = -1;
        // create a map to store parent
        Map<Integer, Integer> parentMap = new HashMap<>(); //block id of child -> parent
        
        int curr = rootId;
        // int leafId = rootId;
        int index = -1;
        parentMap.put(rootId, -1);
        while(!(isLeaf(curr))){
            //curr is internal node
            int[] children = ((InternalNode)blocks.get(curr)).getChildren();
            T[] keys =(T[])((InternalNode)blocks.get(curr)).getKeys();
            for(int i=0; i<keys.length; i++){
                if(compare(key, keys[i]) < 0 ){ // key < keys[i]
                    index = i;
                    break;
                }
                else if(compare(key, keys[i]) == 0){ // key == keys[i]
                    index = i+1; // if multiple keys are same, go to leftmost
                    break;
                }
            }
            if(index ==-1){
                index = children.length - 1; //right most child
            }
            parentMap.put(children[index], curr); 
            curr = children[index];
            index = -1;
        }
        // System.out.println("Leaf node found: "+curr);
        // curr is leaf node -> insert in curr
        if(!isFull(curr)){
            // System.out.println("Leaf not full, inserting in leaf..." + "key: "+key);
            insert_in_leaf((LeafNode)blocks.get(curr), key, block_id);
        }
        else{
            LeafNode node1 = (LeafNode)blocks.get(curr); 
            insert_in_leaf(node1, key, block_id);//node1 is overflow
            LeafNode node2 = new LeafNode(typeClass);
            blocks.add(node2);
            // System.out.println("Leaf is full, splitting leaf..."+ "key: "+key);
            split_leaf(blocks.indexOf(node1), blocks.indexOf(node2), parentMap);
        }
        return;
    }

    public void split_leaf(int id1, int id2, Map<Integer, Integer> parentMap){ //returns id of parent of node1 and node2
        LeafNode node1 = (LeafNode)blocks.get(id1);
        LeafNode node2 = (LeafNode)blocks.get(id2);
        int numKeys = blocks.get(id1).getNumKeys();
        int left = (numKeys)/2;
        int right = numKeys - left;
        int copyOffset = 8;
        for(int i=0; i<left; i++){
            byte[] lenKeyBytes = node1.get_data(copyOffset+2, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);
            byte[] keyBytes = node1.get_data(copyOffset+4, lenKey);
            copyOffset += 4 + keyBytes.length;
        }
        byte[] num1 = new byte[2]; 
        // number of keys in node1 = left
        num1[0] = (byte) (left >> 8);
        num1[1] = (byte) (left & 0xFF);
        node1.write_data(0, num1);
        // nextfreeoffset in node1 = copyOffset
        byte[] nextFreeOffsetBytes = new byte[2];
        nextFreeOffsetBytes[0] = (byte) (copyOffset >> 8);
        nextFreeOffsetBytes[1] = (byte) copyOffset;
        node1.write_data(6, nextFreeOffsetBytes);
        // number of keys in node2 = right
        byte[] num2 = new byte[2];
        num2[0] = (byte) (right >> 8);
        num2[1] = (byte) (right & 0xFF) ;
        node2.write_data(0, num2);
        //   node2 <-- copy second half of keys of node1
        int writeOffset = 8;
        for(int i=0; i<right; i++){
            byte[] blockIdBytes = node1.get_data(copyOffset, 2);
            byte[] lenKeyBytes = node1.get_data(copyOffset+2, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);
            byte[] keyBytes = node1.get_data(copyOffset+4, lenKey);
            node2.write_data(writeOffset, blockIdBytes); //write key in node2
            node2.write_data(writeOffset+2, lenKeyBytes);
            node2.write_data(writeOffset+4, keyBytes);
            
            copyOffset += 4 + keyBytes.length;
            writeOffset += 4 + keyBytes.length;
        }
        // nextfreeoffset in node2 = writeOffset
        nextFreeOffsetBytes[0] = (byte) (writeOffset >> 8);
        nextFreeOffsetBytes[1] = (byte) writeOffset;
        node2.write_data(6, nextFreeOffsetBytes);
        
        // previous and next pointers
        node2.write_data(4, node1.get_data(4, 2)); //next
        byte[] id1Bytes = new byte[2];
        id1Bytes[0] = (byte) (id1 >> 8);
        id1Bytes[1] = (byte) id1;  
        node2.write_data(2, id1Bytes);     //prev  
        
        byte[] id2Bytes = new byte[2];
        id2Bytes[0] = (byte) (id2 >> 8);
        id2Bytes[1] = (byte) id2;
        node1.write_data(4, id2Bytes); //next
        
        // add k in parent of node1 and node2
        byte[] lenKBytes = node2.get_data(10, 2);
        int lenK = (lenKBytes[0] << 8) | (lenKBytes[1] & 0xFF);
        byte[] kBytes = node2.get_data(12, lenK);
        T k = convertBytesToT(kBytes, typeClass); //first key in node2

        int parentId = parentMap.get(id1);
        
        InternalNode<T> parent;
        if(parentId==-1){ //node1 is root
            // make parent as root
            parent = new InternalNode<>(k, id1, id2, typeClass);
            blocks.add(parent);
            int newRootId = blocks.indexOf(parent);
            // update metadata block
            BlockNode metadata =blocks.get(0);
            byte[] newRootIdBytes = new byte[2];
            newRootIdBytes[0] = (byte) (newRootId >> 8);
            newRootIdBytes[1] = (byte) newRootId;
            metadata.write_data(2, newRootIdBytes);
        }
        else{//node1 is not root
            parent = (InternalNode<T>)blocks.get(parentId);
            
            if(!isFull(parentId)){
                //add k in parent of node1 and node2
                parent.insert(k, id2);
            }
            else{
                //add k in parent of node1 and node2
                parent.insert(k, id2); //overflow in parent
                
                // split parent
                split_internal(parentId, parentMap);
           }
        }

    }

    public void split_internal(int nodeId1, Map<Integer, Integer> parentMap){
        InternalNode<T> node1 = (InternalNode<T>)blocks.get(nodeId1);
        
        int left = (node1.getNumKeys())/2;
        int[] children1 = node1.getChildren();
        T[] keys1 = node1.getKeys();
        assert(left+1<keys1.length);
        assert(left+2<children1.length);
        InternalNode<T> node2 = new InternalNode<>(keys1[left+1], children1[left+1], children1[left+2], typeClass); //checkkk
        blocks.add(node2);
        // insert keys in node2
        System.out.println("Left:  -------------");
        for(int i=left+2;i<keys1.length; i++){
            node2.insert(keys1[i], children1[i+1]);
        }
        // delete keys from node1
        int writeOffset = 4;
        for(int i=0;i<left;i++){
            byte[] lenKeyBytes = node1.get_data(writeOffset+2, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);
            writeOffset += 4 + lenKey;
        }
        // update number of keys and nextFreeOffset in node1
        byte[] num1 = new byte[2];
        num1[0] = (byte) (left >> 8); num1[1] = (byte) left;
        node1.write_data(0, num1);
        byte[] nextFreeOffsetBytes = new byte[2];
        nextFreeOffsetBytes[0] = (byte) (writeOffset >> 8); nextFreeOffsetBytes[1] = (byte) writeOffset;
        node1.write_data(2, nextFreeOffsetBytes);

        // add 1 key in parent of node1 
        T shiftKey = keys1[left];
        int nodeId2 = blocks.indexOf(node2);
        int parentId = parentMap.get(nodeId1);
        if(parentId ==-1){//node1 is root
            // make parent as root
            InternalNode<T> parent = new InternalNode<>(shiftKey, nodeId1, nodeId2 , typeClass);
            blocks.add(parent);
            int newRootId = blocks.indexOf(parent);
            // update metadata block
            BlockNode metadata = blocks.get(0);
            byte[] newRootIdBytes = new byte[2];
            newRootIdBytes[0] = (byte) (newRootId >> 8);
            newRootIdBytes[1] = (byte) newRootId;
            metadata.write_data(2, newRootIdBytes);
        }
        else{
            InternalNode<T> parent = (InternalNode<T>)blocks.get(parentId);
            if(!isFull(parentId)){
                //add k in parent of node1 and node2
                parent.insert(shiftKey, nodeId2);
            }
            else{
                //add k in parent of node1 and node2
                parent.insert(shiftKey, nodeId2); //overflow in parent
                // split parent
                split_internal(parentId, parentMap);
            }
           
        }
    }


    // will be evaluated
    // returns the block_id of the leftmost leaf node containing the key
    public int search(T key) {
        /* Write your code here */
        int rootOffset = getRootId();
        BlockNode root = blocks.get(rootOffset);
        BlockNode curr = root ;
        while(!(isLeaf(curr))){
            //curr is internal node
            int[] children = ((InternalNode<T>) curr).getChildren();
            T[] keys = ((InternalNode<T>) curr).getKeys();
            int index = -1;
            for(int i=0; i<keys.length; i++){
                if(compare( key, keys[i]) < 0 ){ // key < keys[i]
                    index = i;
                    break;
                }
                else if(compare(key, keys[i]) == 0){ // key == keys[i]
                    index = i; // if multiple keys are same, go to leftmost
                    break;
                }
            }
            if(index ==-1){
                index = children.length - 1; //right most child
            }
            curr = blocks.get(children[index]);
        }
        //curr is leaf node
        

        // return index(in blocks) where curr is present 
        Boolean flag = false;
        for(BlockNode node: blocks){
            if(node.equals((BlockNode)curr)){
                curr = node;
                flag = true;
                for(T k: ((LeafNode<T>)node).getKeys()){
                    if(key.equals(k)){
                        return blocks.indexOf(node);
                    }
                    else if(compare(key, k)<0){
                        return blocks.indexOf(node);
                    }
                } 
            }
            // if(flag){
            //     for(T k: ((LeafNode<T>)node).getKeys()){
            //         if(key.equals(k)){
            //             return blocks.indexOf(node);
            //         }
            //         else if(compare(key, k)<0){
            //             return blocks.indexOf(node);
            //         }
            //     } 
            // }
        }
        int next = ((LeafNode<T>)curr).nextLeafId();
        while(next>0 && next < blocks.size()){
            curr = blocks.get(next);
            for(T k: ((LeafNode<T>)curr).getKeys()){
                if(key.equals(k)){
                    return blocks.indexOf(curr);
                }
                else if(compare(key, k)<0){
                    return blocks.indexOf(curr);
                }
            }

            next = ((LeafNode<T>)curr).nextLeafId();
        } 


        return -1;
    }

    // returns true if the key was found and deleted, false otherwise
    // (Optional for Assignment 3)
    public boolean delete(T key) {

        /* Write your code here */
        return false;
    }

    // DO NOT CHANGE THIS - will be used for evaluation
    public void print_bfs() {
        int root = getRootId();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(root);
        while(!queue.isEmpty()) {
            int id = queue.remove();
            if(isLeaf(id)) {
                ((LeafNode<T>) blocks.get(id)).print();
            }
            else {
                ((InternalNode<T>) blocks.get(id)).print();
                int[] children = ((InternalNode<T>) blocks.get(id)).getChildren();
                for(int i = 0; i < children.length; i++) {
                    queue.add(children[i]);
                }
            }
        }
        return;
    }

    // DO NOT CHANGE THIS - will be used for evaluation
    public ArrayList<T> return_bfs() {
        int root = getRootId();
        Queue<Integer> queue = new LinkedList<>();
        ArrayList<T> bfs = new ArrayList<>();
        queue.add(root);
        while(!queue.isEmpty()) {
            int id = queue.remove();
            if(isLeaf(id)) {
                T[] keys = ((LeafNode<T>) blocks.get(id)).getKeys();
                for(int i = 0; i < keys.length; i++) {
                    bfs.add((T) keys[i]);
                }
            }
            else {
                T[] keys = ((InternalNode<T>) blocks.get(id)).getKeys();
                for(int i = 0; i < keys.length; i++) {
                    bfs.add((T) keys[i]);
                }
                int[] children = ((InternalNode<T>) blocks.get(id)).getChildren();
                for(int i = 0; i < children.length; i++) {
                    queue.add(children[i]);
                }
            }
        }
        return bfs;
    }

    public void print() {
        print_bfs();
        return;
    }


    // manshi
    public int compare(T key1, T key2){
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
    
    public T convertBytesToT(byte[] bytes, Class<T> typeClass){
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

    public byte[] convertTToBytes(T key, Class<T> typeClass){
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

    public boolean isRoot(BlockNode node){
        return blocks.indexOf(node) == getRootId();
    }

    public void insert_in_leaf( LeafNode node , T key, int block_id){ //node = by ref
        node.insert(key, block_id);
    }


    
public void print_tree() {
    // print level order traversal of tree
    int root = getRootId();
    Queue<SimpleEntry<Integer, Integer>> queue = new LinkedList<>(); // Queue of pairs <nodeID, level>

    queue.add(new SimpleEntry<>(root, 0)); // Add the root node with level 0
    int currentLevel = 0; // Track the current level

    while (!queue.isEmpty()) {
        SimpleEntry<Integer, Integer> entry = queue.poll();
        int id = entry.getKey();
        int level = entry.getValue();

        if (level > currentLevel) {
            System.out.println(); // Move to the next line for a new level
            currentLevel = level;
        }

        if (isLeaf(id)) {
            System.out.print("("+id+":)");
            ((LeafNode<T>) blocks.get(id)).print_node();;
        } else {
            System.out.print("("+id+";)");
            ((InternalNode<T>) blocks.get(id)).print_node();;
            int[] children = ((InternalNode<T>) blocks.get(id)).getChildren();
            for (int i = 0; i < children.length; i++) {
                queue.add(new SimpleEntry<>(children[i], level + 1)); // Add children nodes with increased level
            }
        }
    }
}


}