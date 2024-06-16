package index.bplusTree;

/*
    * A LeafNode contains keys and block ids.
    * Looks Like -
    * # entries | prev leafnode | next leafnode | ptr to next free offset | blockid_1 | len(key_1) | key_1 ...
    *
    * Note: Only write code where specified!
 */
public class LeafNode<T> extends BlockNode implements TreeNode<T>{

    Class<T> typeClass;

    public LeafNode(Class<T> typeClass) {
        
        super();
        this.typeClass = typeClass;

        // set numEntries to 0
        byte[] numEntriesBytes = new byte[2];
        numEntriesBytes[0] = 0;
        numEntriesBytes[1] = 0;
        this.write_data(0, numEntriesBytes);

        // set ptr to next free offset to 8
        byte[] nextFreeOffsetBytes = new byte[2];
        nextFreeOffsetBytes[0] = 0;
        nextFreeOffsetBytes[1] = 8;
        this.write_data(6, nextFreeOffsetBytes);

        return;
    }

    // returns the keys in the node - will be evaluated
    @Override
    public T[] getKeys() {

        int numKeys = getNumKeys();
        T[] keys = (T[]) new Object[numKeys];

        /* Write your code here */
        int lenKeyOffset = 10;
        int keyOffset = 12;
        for(int i=0;i < numKeys; i++){
            byte[] lenKeyBytes = this.get_data(lenKeyOffset, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);

            byte[] keyBytes = this.get_data(keyOffset, lenKey);
            keys[i] = convertBytesToT(keyBytes, this.typeClass);

            lenKeyOffset += 2 + lenKey + 2;
            keyOffset += lenKey + 2 + 2;
        }
        return keys;
    }

    // returns the block ids in the node - will be evaluated
    public int[] getBlockIds() {
        int numKeys = getNumKeys();
        int[] block_ids = new int[numKeys];

        /* Write your code here */
        int blockIdOffset = 8;
        int lenKeyOffset = 10;
        for(int i=0; i<numKeys; i++){
            byte[] blockIdBytes = this.get_data(blockIdOffset, 2);
            int blockId = (blockIdBytes[0] << 8) | (blockIdBytes[1] & 0xFF);
            block_ids[i] = blockId;

            byte[] lenKeyByte = this.get_data(lenKeyOffset, 2);
            int lenKey = (lenKeyByte[0] << 8) | (lenKeyByte[1] & 0xFF);
            
            blockIdOffset += 2 + 2 + lenKey;
            lenKeyOffset += 2 + lenKey + 2;
        }

        return block_ids;
    }

    // can be used as helper function - won't be evaluated
    @Override
    public void insert(T key, int block_id) {
        /* Write your code here */
        int numKeys = this.getNumKeys();
        byte[] keyBytes = convertTToBytes(key, this.typeClass);
        byte[] blockIdBytes = new byte[2];
        byte[] nextFreeOffsetBytes = this.get_data(6, 2);
        int nextFreeOffset = (nextFreeOffsetBytes[0] << 8) | (nextFreeOffsetBytes[1] & 0xFF);
        // insert key in sorted order
        // System.out.println("Inserting leaf key: " + key + " with block id: " + block_id);

        // find where to insert key
        int blockIdOffset = 8;
        int insertOffset = -1;
        for(int i=0;i<numKeys;i++){
            byte[] xlenKeyBytes = this.get_data(blockIdOffset+2, 2);
            int xlenKey = (xlenKeyBytes[0] << 8) | (xlenKeyBytes[1] & 0xFF);
            byte[] xKeyBytes= this.get_data(blockIdOffset+4, xlenKey);
            T xKey = convertBytesToT(xKeyBytes, this.typeClass);
            if(compare(key, xKey) < 0){ //existingKey > key 
                insertOffset = blockIdOffset;
                break; 
            }
            blockIdOffset += 2 + 2 + xlenKey;
        }
        if(insertOffset == -1){// insert at the end
            insertOffset = nextFreeOffset;
        }
        
        // shift all values after insertOffset by size  
        int size = 2 + 2 + keyBytes.length ;
        byte[] tmp = new byte[size];
        this.write_data(nextFreeOffset, tmp);
        for(int i=nextFreeOffset-1; i>=insertOffset; i--){
            this.data[i+size] = this.data[i];
        }
        blockIdBytes[0] = (byte) (block_id >> 8);
        blockIdBytes[1] = (byte) block_id;

        int lenKey = keyBytes.length;
        byte[] lenKeyBytes = new byte[2];
        lenKeyBytes[0] = (byte) (lenKey >> 8);
        lenKeyBytes[1] = (byte) lenKey;

        // insert blockId, lenKey, key
        this.write_data(insertOffset, blockIdBytes);
        this.write_data(insertOffset+2, lenKeyBytes);
        this.write_data(insertOffset+4, keyBytes);

        // update numkeys
        numKeys++;
        byte[] numKeysBytes = new byte[2];
        numKeysBytes[0] = (byte) (numKeys >> 8);
        numKeysBytes[1] = (byte) numKeys;
        this.write_data(0, numKeysBytes);

        // update next free offset
        nextFreeOffset += size;
        nextFreeOffsetBytes[0] = (byte) (nextFreeOffset >> 8);
        nextFreeOffsetBytes[1] = (byte) nextFreeOffset;
        this.write_data(6, nextFreeOffsetBytes);

        return;
    }

    // can be used as helper function - won't be evaluated
    @Override
    public int search(T key) {
        /* Write your code here */
        int numKeys = this.getNumKeys();
        T[] keys = this.getKeys();

        int keyIndex = -1;
        for(int i=0; i<numKeys; i++){
            if(keys[i].equals(key)){
                keyIndex = i;
                break;
            }
        }

        int blockIdOffset = 8;
        int lenKeyOffset = 10;
        for(int i=0; i<numKeys; i++){
            
            if(i==keyIndex){
                return blockIdOffset;
                // byte[] blockIdBytes = this.get_data(blockIdOffset, 2);
                // int blockId = (blockIdBytes[0] << 8) | (blockIdBytes[1] & 0xFF);
                // return blockId;
            }
            
            byte[] lenKeyBytes = this.get_data(lenKeyOffset, 2);
            int lenKey = (lenKeyBytes[0] << 8) | (lenKeyBytes[1] & 0xFF);
            blockIdOffset += 2 + 2 + lenKey;
            lenKeyOffset += 2 + lenKey + 2;

        }
        return -1;
    }


    public void print_node() {
        // System.out.println("Leaf Nodes:");
        int numKeys = this.getNumKeys();
        int id ;
        System.out.print("(" +  numKeys + ")");
        T[] keys = getKeys();
        System.out.print("[");
        for (T key : keys) {
            System.out.print(key + " ");
        }
        System.out.print("] ");
        // int numKeys = getNumKeys();
        // int[] blockIds = getBlockIds();
        // System.out.println("\nBlock Ids: ");
        // for (int blockId : blockIds) {
        //     System.out.print(blockId + " ");
        // }
        // System.out.println();

        // for(int i=0;i<30;i++){
        //     System.out.println(this.data[i]);
        // }
    }
    public int nextLeafId(){
        byte[] nextLeafIdBytes = this.get_data(4, 2);
        return (nextLeafIdBytes[0] << 8) | (nextLeafIdBytes[1] & 0xFF);
    }
    public int prevLeafId(){
        byte[] prevLeafIdBytes = this.get_data(2, 2);
        return (prevLeafIdBytes[0] << 8) | (prevLeafIdBytes[1] & 0xFF);
    }
}
