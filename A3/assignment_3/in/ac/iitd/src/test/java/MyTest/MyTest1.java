package MyTest;
import index.bplusTree.InternalNode;
import index.bplusTree.LeafNode;
import index.bplusTree.BlockNode;
import index.bplusTree.BPlusTreeIndexFile;
import org.junit.Test;

public class MyTest1 {
    // @Test
    public void get_keys_from_internal_node() {
        // InternalNode<Integer> internalNode = new InternalNode<>(1000, 2, 3, Integer.class);
        // internalNode.insert(2000, 2001);
        // internalNode.insert(1500, 1001);
        // internalNode.print_node();


        // LeafNode<Integer> leafNode = new LeafNode<>( Integer.class);
        // leafNode.insert(20000, 9130);
        // leafNode.insert(10000, 6592);
        // leafNode.print_node();

        BPlusTreeIndexFile<Integer> bPlusTreeIndexFile = new BPlusTreeIndexFile<>(3, Integer.class); //m+1 = 4
        for(int i=1; i<5; i++){
            bPlusTreeIndexFile.insert(i, i);
        }
        for(int i=12; i<18; i++){
            bPlusTreeIndexFile.insert(5, i+5);
        }
        for(int i=8; i<12; i++){
            bPlusTreeIndexFile.insert(i, i);
        }
        bPlusTreeIndexFile.print_tree();
        System.out.println();
        System.out.println(1 + ": "+  bPlusTreeIndexFile.search(1));
        System.out.println(11 + ": "+  bPlusTreeIndexFile.search(11));
        System.out.println(17 + ": "+  bPlusTreeIndexFile.search(17)); 
        System.out.println(5 + ": "+  bPlusTreeIndexFile.search(5));
        System.out.println(6 + ": "+  bPlusTreeIndexFile.search(6));
        System.out.println(7 + ": "+  bPlusTreeIndexFile.search(7));
    

    }

  
}
