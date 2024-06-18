package manager;

import storage.DB;
import storage.File;
import storage.AbstractFile;
import storage.Block;
import Utils.CsvRowConverter;
import index.bplusTree.BPlusTreeIndexFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// import org.apache.calcite.adapter.clone.ArrayTable.Column;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.util.Sources;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.omg.PortableInterceptor.INACTIVE;

import java.util.Iterator;

public class StorageManager {

    private HashMap<String, Integer> file_to_fileid;
    private DB db;

    enum ColumnType {
        VARCHAR, INTEGER, BOOLEAN, FLOAT, DOUBLE
    };

    public StorageManager() {
        file_to_fileid = new HashMap<>();
        db = new DB();
    }

    // loads CSV files into DB362
    public void loadFile(String csvFile, List<RelDataType> typeList) {

        System.out.println("Loading file: " + csvFile);

        String table_name = csvFile;

        if(csvFile.endsWith(".csv")) {
            table_name = table_name.substring(0, table_name.length() - 4);
        }

        // check if file already exists
        assert(file_to_fileid.get(table_name) == null);

        File f = new File();
        try{
            csvFile = getFsPath() + "/" + csvFile;
            BufferedReader br = new BufferedReader(new FileReader(csvFile));
            String line = "";
            int lineNum = 0;

            while ((line = br.readLine()) != null) {

                // csv header line
                if(lineNum == 0){

                    String[] columnNames = CsvRowConverter.parseLine(line);
                    List<String> columnNamesList = new ArrayList<>();

                    for(String columnName : columnNames) {
                        // if columnName contains ":", then take part before ":"
                        String c = columnName;
                        if(c.contains(":")) {
                            c = c.split(":")[0];
                        }
                        columnNamesList.add(c);
                    }

                    Block schemaBlock = createSchemaBlock(columnNamesList, typeList);
                    f.add_block(schemaBlock);
                    lineNum++;
                    continue;
                }

                String[] parsedLine = CsvRowConverter.parseLine(line);
                Object[] row = new Object[parsedLine.length];

                for(int i = 0; i < parsedLine.length; i++) {
                    row[i] = CsvRowConverter.convert(typeList.get(i), parsedLine[i]);
                }

                // convert row to byte array
                byte[] record = convertToByteArray(row, typeList);

                boolean added = f.add_record_to_last_block(record);
                if(!added) {
                    f.add_record_to_new_block(record);
                }
                lineNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        System.out.println("Done writing file\n");
        int counter = db.addFile(f);
        file_to_fileid.put(table_name, counter);
        return;
    }

    // converts a row to byte array to write to relational file
    private byte[] convertToByteArray(Object[] row, List<RelDataType> typeList) {

        List<Byte> fixed_length_Bytes = new ArrayList<>();
        List<Byte> variable_length_Bytes = new ArrayList<>();
        List<Integer> variable_length = new ArrayList<>();
        List<Boolean> fixed_length_nullBitmap = new ArrayList<>();
        List<Boolean> variable_length_nullBitmap = new ArrayList<>();

        for(int i = 0; i < row.length; i++) {

            if(typeList.get(i).getSqlTypeName().getName().equals("INTEGER")) {
                if(row[i] == null){
                    fixed_length_nullBitmap.add(true);
                    for(int j = 0; j < 4; j++) {
                        fixed_length_Bytes.add((byte) 0);
                    }
                } else {
                    fixed_length_nullBitmap.add(false);
                    int val = (int) row[i];
                    byte[] intBytes = new byte[4];
                    intBytes[0] = (byte) (val & 0xFF);
                    intBytes[1] = (byte) ((val >> 8) & 0xFF);
                    intBytes[2] = (byte) ((val >> 16) & 0xFF);
                    intBytes[3] = (byte) ((val >> 24) & 0xFF);
                    for(int j = 0; j < 4; j++) {
                        fixed_length_Bytes.add(intBytes[j]);
                    }
                }
            } else if(typeList.get(i).getSqlTypeName().getName().equals("VARCHAR")) {
                if(row[i] == null){
                    variable_length_nullBitmap.add(true);
                    for(int j = 0; j < 1; j++) {
                        variable_length_Bytes.add((byte) 0);
                    }
                } else {
                    variable_length_nullBitmap.add(false);
                    String val = (String) row[i];
                    byte[] strBytes = val.getBytes();
                    for(int j = 0; j < strBytes.length; j++) {
                        variable_length_Bytes.add(strBytes[j]);
                    }
                    variable_length.add(strBytes.length);
                }
            } else if (typeList.get(i).getSqlTypeName().getName().equals("BOOLEAN")) {         
                if(row[i] == null){
                    fixed_length_nullBitmap.add(true);
                    fixed_length_Bytes.add((byte) 0);
                } else {
                    fixed_length_nullBitmap.add(false);
                    boolean val = (boolean) row[i];
                    fixed_length_Bytes.add((byte) (val ? 1 : 0));
                }
            } else if (typeList.get(i).getSqlTypeName().getName().equals("FLOAT")) {
                
                if(row[i] == null){
                    fixed_length_nullBitmap.add(true);
                    for(int j = 0; j < 4; j++) {
                        fixed_length_Bytes.add((byte) 0);
                    }
                } else {
                    fixed_length_nullBitmap.add(false);
                    float val = (float) row[i];
                    byte[] floatBytes = new byte[4];
                    int intBits = Float.floatToIntBits(val);
                    floatBytes[0] = (byte) (intBits & 0xFF);
                    floatBytes[1] = (byte) ((intBits >> 8) & 0xFF);
                    floatBytes[2] = (byte) ((intBits >> 16) & 0xFF);
                    floatBytes[3] = (byte) ((intBits >> 24) & 0xFF);
                    for(int j = 0; j < 4; j++) {
                        fixed_length_Bytes.add(floatBytes[j]);
                    }
                }
            } else if (typeList.get(i).getSqlTypeName().getName().equals("DOUBLE")) {
                
                if(row[i] == null){
                    fixed_length_nullBitmap.add(true);
                    for(int j = 0; j < 8; j++) {
                        fixed_length_Bytes.add((byte) 0);
                    }
                } else {
                    fixed_length_nullBitmap.add(false);
                    double val = (double) row[i];
                    byte[] doubleBytes = new byte[8];
                    long longBits = Double.doubleToLongBits(val);
                    doubleBytes[0] = (byte) (longBits & 0xFF);
                    doubleBytes[1] = (byte) ((longBits >> 8) & 0xFF);
                    doubleBytes[2] = (byte) ((longBits >> 16) & 0xFF);
                    doubleBytes[3] = (byte) ((longBits >> 24) & 0xFF);
                    doubleBytes[4] = (byte) ((longBits >> 32) & 0xFF);
                    doubleBytes[5] = (byte) ((longBits >> 40) & 0xFF);
                    doubleBytes[6] = (byte) ((longBits >> 48) & 0xFF);
                    doubleBytes[7] = (byte) ((longBits >> 56) & 0xFF);
                    for(int j = 0; j < 8; j++) {
                        fixed_length_Bytes.add(doubleBytes[j]);
                    }
                }
            } else {
                System.out.println("Unsupported type");
                throw new RuntimeException("Unsupported type");
            }
        }

        short num_bytes_for_bitmap = (short) ((fixed_length_nullBitmap.size() + variable_length_nullBitmap.size() + 7) / 8); // should be in multiples of bytes

        //                       bytes for fixed length and variable length fields          offset & length of var fields
        byte[] result = new byte[fixed_length_Bytes.size() + variable_length_Bytes.size() + 4 * variable_length.size() + num_bytes_for_bitmap];
        int variable_length_offset = 4 * variable_length.size() + fixed_length_Bytes.size() + num_bytes_for_bitmap;

        int idx = 0;
        for(; idx < variable_length.size() ; idx ++){
            // first 2 bytes should be offset
            result[idx * 4] = (byte) (variable_length_offset & 0xFF);
            result[idx * 4 + 1] = (byte) ((variable_length_offset >> 8) & 0xFF);

            // next 2 bytes should be length
            result[idx * 4 + 2] = (byte) (variable_length.get(idx) & 0xFF);
            result[idx * 4 + 3] = (byte) ((variable_length.get(idx) >> 8) & 0xFF);

            variable_length_offset += variable_length.get(idx);
        }

        idx = idx * 4;

        // write fixed length fields
        for(int i = 0; i < fixed_length_Bytes.size(); i++, idx++) {
            result[idx] = fixed_length_Bytes.get(i);
        }

        // write null bitmap
        int bitmap_idx = 0;
        for(int i = 0; i < fixed_length_nullBitmap.size(); i++) {
            if(fixed_length_nullBitmap.get(i)) {
                result[idx] |= (1 << (7 - bitmap_idx));
            }
            bitmap_idx++;
            if(bitmap_idx == 8) {
                bitmap_idx = 0;
                idx++;
            }
        }
        for(int i = 0; i < variable_length_nullBitmap.size(); i++) {
            if(variable_length_nullBitmap.get(i)) {
                result[idx] |= (1 << (7 - bitmap_idx));
            }
            bitmap_idx++;
            if(bitmap_idx == 8) {
                bitmap_idx = 0;
                idx++;
            }
        }

        if(bitmap_idx != 0) {
            idx++;
        }

        // write variable length fields
        for(int i = 0; i < variable_length_Bytes.size(); i++, idx++) {
            result[idx] = variable_length_Bytes.get(i);
        }

        return result;
    }

    // helper function for loadFile
    private String getFsPath() throws IOException, ParseException {

        String modelPath = Sources.of(CsvRowConverter.class.getResource("/" + "model.json")).file().getAbsolutePath();
        JSONObject json = (JSONObject) new JSONParser().parse(new FileReader(modelPath));
        JSONArray schemas = (JSONArray) json.get("schemas");

        Iterator itr = schemas.iterator();

        while (itr.hasNext()) {
            JSONObject next = (JSONObject) itr.next();
            if (next.get("name").equals("FILM_DB")) {
                JSONObject operand = (JSONObject) next.get("operand");
                String directory = operand.get("directory").toString();
                return Sources.of(CsvRowConverter.class.getResource("/" + directory)).file().getAbsolutePath();
            }
        }
        return null;
    }

    // write schema block for a relational file
    private Block createSchemaBlock(List<String> columnNames, List<RelDataType> typeList) {

        Block schema = new Block();

        // write number of columns
        byte[] num_columns = new byte[2];
        num_columns[0] = (byte) (columnNames.size() & 0xFF);
        num_columns[1] = (byte) ((columnNames.size() >> 8) & 0xFF);

        schema.write_data(0, num_columns);

        int idx = 0, curr_offset = schema.get_block_capacity();
        for(int i = 0 ; i < columnNames.size() ; i ++){
            // if column type is fixed, then write it
            if(!typeList.get(i).getSqlTypeName().getName().equals("VARCHAR")) {
                
                // write offset
                curr_offset = curr_offset - (columnNames.get(i).length() + 2);
                byte[] offset = new byte[2];
                offset[0] = (byte) (curr_offset & 0xFF);
                offset[1] = (byte) ((curr_offset >> 8) & 0xFF);
                schema.write_data(2 + 2 * idx, offset);
                
                // convert column name to bytes
                byte[] column_name_type = new byte[columnNames.get(i).length() + 2];
                // first byte will tell datatype, 2nd byte will tell length of column name
                // Thus, assert that column name length is less than 256
                assert(columnNames.get(i).length() < 256);

                column_name_type[0] = (byte) (ColumnType.valueOf(typeList.get(i).getSqlTypeName().getName()).ordinal() & 0xFF);
                column_name_type[1] = (byte) (columnNames.get(i).length() & 0xFF);
                for(int j = 0; j < columnNames.get(i).length(); j++) {
                    column_name_type[2 + j] = (byte) columnNames.get(i).charAt(j);
                }

                schema.write_data(curr_offset, column_name_type);
                idx++;
            }
        }

        // write variable length fields
        for(int i = 0; i < columnNames.size(); i++) {
            if(typeList.get(i).getSqlTypeName().getName().equals("VARCHAR")) {
                
                // write offset
                curr_offset = curr_offset - (columnNames.get(i).length() + 2);
                byte[] offset = new byte[2];
                offset[0] = (byte) (curr_offset & 0xFF);
                offset[1] = (byte) ((curr_offset >> 8) & 0xFF); 
                // IMPORTANT: Take care of endianness
                schema.write_data(2 + 2 * idx, offset);
                
                // convert column name to bytes
                byte[] column_name_type = new byte[columnNames.get(i).length() + 2];
                // first byte will tell datatype, 2nd byte will tell length of column name
                // Thus, assert that column name length is less than 256
                assert(columnNames.get(i).length() < 256);

                column_name_type[0] = (byte) (ColumnType.valueOf(typeList.get(i).getSqlTypeName().getName()).ordinal() & 0xFF);
                column_name_type[1] = (byte) (columnNames.get(i).length() & 0xFF);
                for(int j = 0; j < columnNames.get(i).length(); j++) {
                    column_name_type[2 + j] = (byte) columnNames.get(i).charAt(j);
                }

                schema.write_data(curr_offset, column_name_type);
                idx++;
            }
        }

        return schema;
    }

    // should only read one block at a time
    public byte[] get_data_block(String table_name, int block_id){
        int file_id = file_to_fileid.get(table_name);
        return db.get_data(file_id, block_id);
    }

    public boolean check_file_exists(String table_name) {
        return file_to_fileid.get(table_name) != null;
    }

    public boolean check_index_exists(String table_name, String column_name) {
        String index_file_name = table_name + "_" + column_name + "_index";
        return file_to_fileid.get(index_file_name) != null;
    }

    // the order of returned columns should be same as the order in schema
    // i.e., first all fixed length columns, then all variable length columns
    public List<Object[]> get_records_from_block(String table_name, int block_id){
        System.out.println("get_records_from_block.........");
        System.out.println("table_name: " + table_name + " block_id: " + block_id);
        /* Write your code here */
        // return null if file does not exist, or block_id is invalid
        if(!check_file_exists(table_name)){
            return null;
        }
        int file_id = file_to_fileid.get(table_name);
        byte[] block = db.get_data(file_id, block_id);
        
        if(block == null){
            return null;
        }
        // return list of records otherwise
        // block -> records(rows of csv) -> fields(columns of csv)
        // list -> of records
        // object[] -> of fields in a record
        List<Object[]> allRecords = new ArrayList<>();
        List<byte[]> allRecordsBytes = new ArrayList<>();
        // first 2 bytes are for number of records in that block
        int numRecords = (block[1] & 0xFF) | (block[0]<<8); // big endian
        System.out.println("numRecords: " + numRecords);
        int offset = 2;
        for(int i=0; i<numRecords; i++){
            byte[] recordBytes;
            if(i==0){
                byte[] recordOffsetBytes = new byte[2];
                recordOffsetBytes[0] = block[offset];
                recordOffsetBytes[1] = block[offset+1];
                int recordOffset = (recordOffsetBytes[1]  & 0xFF) | (recordOffsetBytes[0] << 8);
                recordBytes = new byte[4096-recordOffset];
                for(int k=0;k<4096-recordOffset;k++){
                    recordBytes[k] = block[recordOffset+k];
                }

            }
            else{
                byte[] prevOffsetBytes = new byte[2];
                prevOffsetBytes[0] = block[offset-2];
                prevOffsetBytes[1] = block[offset-1];
                byte[] recordOffsetBytes = new byte[2];
                recordOffsetBytes[0] = block[offset];
                recordOffsetBytes[1] = block[offset+1];
                int prevOffset = (prevOffsetBytes[1]  & 0xFF) | (prevOffsetBytes[0] <<8);
                int recordOffset = (recordOffsetBytes[1] & 0xFF) | (recordOffsetBytes[0] << 8);
                recordBytes = new byte[prevOffset - recordOffset];
                for(int j=0;j<prevOffset - recordOffset;j++){
                    recordBytes[j] = block[recordOffset+j];
                }                
            }
            offset += 2;
            allRecordsBytes.add(recordBytes);
        }
        // map to store(columnNumber, columnType)
        HashMap<Integer, ColumnType> columnTypeMap = new HashMap<>();
        // read metadata block to get column types
        byte[] metadataBlock = db.get_data(file_id, 0);
        int numColumns = (metadataBlock[0] & 0xFF) | (metadataBlock[1] << 8); // little endian
        for(int i=2;i<= numColumns*2 ;i+=2){
            int colOffset = (metadataBlock[i] & 0xFF) | (metadataBlock[i+1] << 8); // little endian
            if(colOffset == 0){
                break;
            }
            byte colType = metadataBlock[colOffset];
            columnTypeMap.put(i/2 - 1, ColumnType.values()[colType]);
        }

        int varLenFields = 0;
        for(int i=0;i<numColumns;i++){
            if(columnTypeMap.get(i) == ColumnType.VARCHAR){
                varLenFields++;
            }
        }
        System.out.println("varLenFields: " + varLenFields);
        // convert each byte[] to object[]
        for(int i=0; i<numRecords; i++){
            // System.out.println("i th record============================= " + i);
            byte[] recordBytes = allRecordsBytes.get(i);
            Object[] record = new Object[numColumns];
            int offsetf = 0;
            //read fixed length fields
            for(int j= 0; j<numColumns - varLenFields; j++){
                int offsetj = 4*varLenFields  + offsetf;
                if(columnTypeMap.get(j)== ColumnType.INTEGER ){
                    // System.out.println("int");
                    byte[] fieldBytes = new byte[4];
                    for(int k=0;k<4;k++){
                        fieldBytes[k] = recordBytes[offsetj+k];
                    }
                    record[j] = convertBytesToObject(fieldBytes, Integer.class);
                    offsetf += 4;
                }
                else if(columnTypeMap.get(j)== ColumnType.BOOLEAN ){
                    // System.err.println("bool");
                    byte[] fieldBytes = new byte[1];
                    for(int k=0;k<1;k++){
                        fieldBytes[k] = recordBytes[offsetj+k];
                    }
                    record[j] = convertBytesToObject(fieldBytes, Boolean.class);
                    offsetf += 1;
                }
                else if(columnTypeMap.get(j)== ColumnType.FLOAT ){
                    // System.out.println("float");
                    byte[] fieldBytes = new byte[4];
                    for(int k=0;k<4;k++){
                        fieldBytes[k] = recordBytes[offsetj+k];
                    }
                    record[j] = convertBytesToObject(fieldBytes, Float.class);
                    offsetf += 4;
                }
                else if(columnTypeMap.get(j)== ColumnType.DOUBLE ){
                    // System.out.println("double");
                    byte[] fieldBytes = new byte[8];
                    for(int k=0;k<8;k++){
                        fieldBytes[k] = recordBytes[offsetj+k];
                    }
                    record[j] = convertBytesToObject(fieldBytes, Double.class);
                    offsetf += 8;
                }
            }
            
            //read variable length fields
            for(int j=0;j<varLenFields; j++){
                // System.out.println("varchar");
                int offsetj = (recordBytes[j*4+1] << 8) | (recordBytes[j*4] & 0xFF); 
                int lengthj = (recordBytes[j*4+3] << 8) | (recordBytes[j*4+2] & 0xFF); 
                // System.out.println("offsetj: " + offsetj + " lengthj: " + lengthj);
                byte[] fieldBytes = new byte[lengthj];
                for(int k=0;k<lengthj;k++){
                    fieldBytes[k] = recordBytes[offsetj+k];
                }
                // convert fieldBytes to varchar object
                record[j + (numColumns - varLenFields)] = convertBytesToObject(fieldBytes, String.class);
            }
            // null bit map
            int tmp = 4*varLenFields;
            // traverse in columntypemap
            for(int j=0;j<numColumns;j++){
                // if integer type add 4 in tmp
                if(columnTypeMap.get(j) == ColumnType.INTEGER){
                    tmp += 4;
                }
                else if(columnTypeMap.get(j) == ColumnType.BOOLEAN){
                    tmp += 1;
                }
                else if(columnTypeMap.get(j) == ColumnType.FLOAT){
                    tmp += 4;
                }
                else if(columnTypeMap.get(j) == ColumnType.DOUBLE){
                    tmp += 8;
                }
            }
            byte[] nullBitmap = new byte[(numColumns + 7) / 8];
            for(int j=0;j<(numColumns + 7) / 8;j++){
                nullBitmap[j] = recordBytes[tmp+j];
            }
            // read bitmap
            for(int x=0; x<numColumns; x++){
                // read ith bit
                int byteIndex = x / 8;
                int bitIndex = x % 8;
                if((nullBitmap[byteIndex] & (1 << (7 - bitIndex))) != 0){
                    record[x] = null;
                }
            }
            allRecords.add(record);
        }
        System.out.println("get records from block complete--- " + allRecords.size());
        return allRecords;
        // return null;
    }

    public boolean create_index(String table_name, String column_name, int order) {
        /* Write your code here */
        int file_id = file_to_fileid.get(table_name);
        System.out.println("file_id: " + file_id);
        if(file_id == -1){
            System.out.println("File not found in database");
            return false;
        }
        // create index file
        
        // get columnID from column_name using metadata block
        int columnID = -1;
        byte[] metadataBlock = db.get_data(file_id, 0);
        int numColumns = (metadataBlock[0] & 0xFF) | (metadataBlock[1] << 8); // little endian
        // System.out.println("numColumns: " + numColumns);
        // read metadata
        byte columnTypeByte = 0;
        Object typeObj = null ;
        for(int i=2;i< numColumns*2 ;i+=2){
            int colOffset = (metadataBlock[i] & 0xFF) | (metadataBlock[i+1] << 8); // little endian
            System.out.println("colOffset: " + colOffset);
            int colNameLength = metadataBlock[colOffset+1];
            System.out.println("colNameLength: " + colNameLength);
            byte[] colNameBytes = new byte[colNameLength];
            for(int j=0;j<colNameLength;j++){
                colNameBytes[j] = metadataBlock[colOffset+2+j];
            }
            String colName = new String(colNameBytes);
            System.out.println("colName: " + colName + "colid = " + (i/2-1)); ;
            if(colName.equals(column_name)){
                columnID = i/2 - 1;
                columnTypeByte = metadataBlock[colOffset];
                typeObj = getColumnType(columnTypeByte);
                // columnType = ColumnType.values()[colType];
                break;
            }
        }
        // System.out.println("columnID: " + columnID);
        if(columnID == -1){
            System.out.println("Column not found in metadata block");
            return false;
        }
        // create tree = index file
        
        //enum ????????? columntype
        BPlusTreeIndexFile<Object> indexTree = new BPlusTreeIndexFile(order, typeObj.getClass());
        int counter = db.addFile(indexTree);
        file_to_fileid.put(table_name + "_" + column_name + "_index", counter);

        // int numBlocks ;
        // if(db.get_num_records(file_id) % 4096 == 0){
        //     numBlocks = db.get_num_records(file_id) / 4096;
        // }
        // else{
        //     numBlocks = db.get_num_records(file_id) / 4096 + 1;
        // }

        // System.out.println("numBlocks: " + numBlocks);

        // for(int i=1; i<=numBlocks; i++){
        int b=1;
        while(get_records_from_block(table_name, b)!= null){
            List<Object[]> records = get_records_from_block(table_name, b);
            System.out.println("records_size: " + records.size());
            for(int j=0; j<(int)records.size(); j++){
                Object[] record = records.get(j);
                Object key = record[columnID];
                indexTree.insert((Integer)key, b);
            }
            b++;
        }
        // print tree 
        indexTree.print_tree();
        System.out.println("create index done...");
        return true;
    }

    // returns the block_id of the leaf node where the key is present
    public int search(String table_name, String column_name, RexLiteral value) {
        /* Write your code here */
        System.out.println("search..." + value);
        int file_id = file_to_fileid.get(table_name + "_" + column_name + "_index");
        if(file_id == -1){
            System.out.println("Index file not found");
            return -1;
        }
        System.out.println("file_id: " + file_id);
        int val = value.getValueAs(Integer.class);
        System.out.println("val: " + val);
        int block_id = db.search_index(file_id, val);
        System.out.println("block_id of leaf ( leafid ): " + block_id);
        return block_id;
    }

    public boolean delete(String table_name, String column_name, RexLiteral value) {
        /* Write your code here */
        // Hint: You need to delete from both - the file and the index

        return false;
    }

    // will be used for evaluation - DO NOT modify
    public DB getDb() {
        return db;
    }

    public <T> ArrayList<T> return_bfs_index(String table_name, String column_name) {
        if(check_index_exists(table_name, column_name)) {
            int file_id = file_to_fileid.get(table_name + "_" + column_name + "_index");
            return db.return_bfs_index(file_id);
        } else {
            System.out.println("Index does not exist");
        }
        return null;
    }

    // manshi
    public int getFileId(String table_name){
        return file_to_fileid.get(table_name);
    }
    public Object convertBytesToObject(byte[] bytes, Class<?> typeClass){
        if(typeClass.equals(String.class)){
            return new String(bytes);
        }
        else if(typeClass.equals(Integer.class)){
            int value = 0;
            for(int i=3; i>=0; i--){
                value = (value << 8) | (bytes[i] & 0xFF);
            }
            return new Integer(value);
        }
        else if(typeClass.equals(Boolean.class)){
            return new Boolean(bytes[0] == 1);
        }
        else if(typeClass.equals(Float.class)){
            float floatValue = ByteBuffer.wrap(bytes).getFloat();
            return new Float(floatValue);
        }
        else if(typeClass.equals(Double.class)){
            double doubleValue = ByteBuffer.wrap(bytes).getDouble();
            return new Double(doubleValue);
        }

        System.out.println("Invalid typeClass");
        return null;
    }
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

}
