package index.bplusTree;

/*
    * Internal Node - num Keys | ptr to next free offset | P_1 | len(K_1) | K_1 | P_2 | len(K_2) | K_2 | ... | P_n
    * Only write code where specified

    * Remember that each Node is a block in the Index file, thus, P_i is the block_id of the child node
 */
public class InternalNode<T> extends BlockNode implements TreeNode<T> {

    // Class of the key
    Class<T> typeClass;

    // Constructor - expects the key, left and right child ids
    public InternalNode(T key, int left_child_id, int right_child_id, Class<T> typeClass) {

        super();
        this.typeClass = typeClass;

        byte[] numKeysBytes = new byte[2];
        numKeysBytes[0] = 0;
        numKeysBytes[1] = 0;

        this.write_data(0, numKeysBytes);

        byte[] child_1 = new byte[2];
        child_1[0] = (byte) ((left_child_id >> 8) & 0xFF);
        child_1[1] = (byte) (left_child_id & 0xFF);

        this.write_data(4, child_1);

        byte[] nextFreeOffsetBytes = new byte[2];
        nextFreeOffsetBytes[0] = 0;
        nextFreeOffsetBytes[1] = 6;

        this.write_data(2, nextFreeOffsetBytes);

        // also calls the insert method
        this.insert(key, right_child_id);
        return;
    }

    // returns the keys in the node - will be evaluated
    @Override
    public T[] getKeys() {

        int numKeys = getNumKeys();
        T[] keys = (T[]) new Object[numKeys];

        /* Write your code here */
        int lenKeyOffset = 6;
        int keyOffset = 8;
       
        for(int i=0; i<numKeys; i++){
            byte[] lenKeyBytes = this.get_data(lenKeyOffset, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);

            byte[] keyBytes = this.get_data(keyOffset, lenKey);
            keys[i] = convertBytesToT(keyBytes, this.typeClass); // check ????????

            lenKeyOffset += 2 + keyBytes.length + 2;
            keyOffset += keyBytes.length + 2 + 2;
        }
        return keys;
    }

    // can be used as helper function - won't be evaluated
    @Override
    public void insert(T key, int right_block_id) {
        /* Write your code here */
        int numKeys = this.getNumKeys();

        byte[] nextFreeOffsetBytes = this.get_data(2, 2);
        int nextFreeOffset = (nextFreeOffsetBytes[0] << 8) | (nextFreeOffsetBytes[1] & 0xFF);
        
        byte[] keyBytes = convertTToBytes(key, this.typeClass); 
        int size = 2+2+ keyBytes.length;
        // -------------------------------------------
        // find where to insert key
        int lenKeyOffset = 6; // len key | key | right id
        int insertOffset = -1;
        for(int i=0; i<numKeys; i++){
            byte[] xlenKeyBytes = this.get_data(lenKeyOffset, 2);
            int xlenKey = (xlenKeyBytes[0] << 8) | (xlenKeyBytes[1] & 0xFF);
            byte[] xKeyBytes = this.get_data(lenKeyOffset+2, xlenKey);
            T xKey = convertBytesToT(xKeyBytes, this.typeClass);
            if(compare(key, xKey) < 0){
                insertOffset = lenKeyOffset;
                break;
            }
            lenKeyOffset += 2 + xlenKey + 2;
        }
        if(insertOffset==-1){
            insertOffset = nextFreeOffset;
        }
        // System.out.println("Insert offset: " + insertOffset);
        byte[] tmp = new byte[size];
        this.write_data(nextFreeOffset, tmp); //increase size of this.data
        // shift array 
        for(int i= nextFreeOffset-1 ; i>=insertOffset; i--){
            this.data[i+size] = this.data[i];
        }
        // System.out.println("numkeys = " + numKeys);
        
        // add len key, new key
        int lenKey = keyBytes.length;
        byte[] lenKeyBytes = new byte[2];
        lenKeyBytes[0] = (byte) ((lenKey >> 8) & 0xFF);
        lenKeyBytes[1] = (byte) (lenKey & 0xFF);
        this.write_data(insertOffset, lenKeyBytes);
        this.write_data(insertOffset+2, keyBytes);

        // add right block id
        byte[] rightBlockIdBytes = new byte[2];
        rightBlockIdBytes[0] = (byte) ((right_block_id >> 8) & 0xFF);
        rightBlockIdBytes[1] = (byte) (right_block_id & 0xFF);
        this.write_data(insertOffset+2+keyBytes.length, rightBlockIdBytes);

        // -------------------------------------------

        // update next free offset
        nextFreeOffset += size;
        nextFreeOffsetBytes[0] = (byte) ((nextFreeOffset >> 8) & 0xFF);
        nextFreeOffsetBytes[1] = (byte) (nextFreeOffset & 0xFF);
        this.write_data(2, nextFreeOffsetBytes); 
        
        // update num keys
        numKeys++;
        byte[] numKeysBytes = new byte[2];
        numKeysBytes[0] = (byte) ((numKeys >> 8) & 0xFF);
        numKeysBytes[1] = (byte) (numKeys & 0xFF);
        this.write_data(0, numKeysBytes);

    }

    // can be used as helper function - won't be evaluated
    @Override
    public int search(T key) {
        /* Write your code here */
        int numKeys = this.getNumKeys();
        T[] keys   = this.getKeys();
        int keyIndex = -1;
        for(int i=0; i<keys.length; i++){
            if(keys[i].equals(key)){
                keyIndex = i;
                break;
            }
        }

        if(keyIndex==-1) return -1;
        
        int lenKeyOffset = 6;
        for(int i=0; i<numKeys; i++){
            byte[] lenKeyBytes = this.get_data(lenKeyOffset, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);

            if(i==keyIndex){
                return lenKeyOffset + 2;
            }
            lenKeyOffset += 2 + lenKey + 2;
        }
        return -1;
    }

    // should return the block_ids of the children - will be evaluated
    public int[] getChildren() {

        byte[] numKeysBytes = this.get_data(0, 2);
        int numKeys = (numKeysBytes[0] << 8) | (numKeysBytes[1] & 0xFF);

        int[] children = new int[numKeys + 1];

        /* Write your code here */
        int nextChildOffset = 4;
        for(int i=0; i<numKeys+1; i++){
            byte[] childIdBytes = this.get_data(nextChildOffset, 2);
            children[i] = (childIdBytes[0] << 8) | (childIdBytes[1] & 0xFF);
            
            byte[] lenKeyBytes = this.get_data(nextChildOffset+2, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);

            nextChildOffset += 2 + 2 + lenKey;
        }
        return children;
    }


    public void print_node() {
        // System.out.println("Internal Nodes:");
        int numKeys = this.getNumKeys();
        System.out.print("(" + numKeys+")" );
        T[] keys = this.getKeys();
        System.out.print("[");
        for(int i=0; i<numKeys; i++){
            System.out.print(keys[i] + " ");
        }
        System.out.print("] ");
        // System.out.println();
        // int[] children = this.getChildren();
        // System.out.println("Children: ");
        // for(int i=0; i<numKeys+1; i++){
        //     System.out.print(children[i] + " ");
        // }
        // System.out.println();

        // for(int i=4;i<30;i++){
        //     System.out.println(this.data[i]);
        // }
    }

}
