package com.oceanbase;

import java.util.ArrayList;
import java.util.HashMap;

import com.oceanbase.obvec_jdbc.DataType;
import com.oceanbase.obvec_jdbc.IndexParam;
import com.oceanbase.obvec_jdbc.IndexParams;
import com.oceanbase.obvec_jdbc.ObCollectionSchema;
import com.oceanbase.obvec_jdbc.ObFieldSchema;
import com.oceanbase.obvec_jdbc.ObVecClient;
import com.oceanbase.obvec_jdbc.SqlInteger;
import com.oceanbase.obvec_jdbc.SqlText;
import com.oceanbase.obvec_jdbc.SqlVector;
import com.oceanbase.obvec_jdbc.Sqlizable;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            String uri = "jdbc:oceanbase://127.0.0.1:2881/test";
            String user = "root@test";
            String password = "";
            String tb_name = "JAVA_TEST";

            ObVecClient ob = new ObVecClient(uri, user, password);
            ob.dropCollection(tb_name);
            System.out.println(ob.hasCollection(tb_name));

            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            ObFieldSchema c1_field = new ObFieldSchema("c1", DataType.INT32);
            c1_field.IsPrimary(true).IsAutoInc(true);
            ObFieldSchema c2_field = new ObFieldSchema("c2", DataType.FLOAT_VECTOR);
            c2_field.Dim(3).IsNullable(false);
            ObFieldSchema c3_field = new ObFieldSchema("c3", DataType.JSON);
            c3_field.IsNullable(true);
            collectionSchema.addField(c1_field);
            collectionSchema.addField(c2_field);
            collectionSchema.addField(c3_field);

            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx1", "c2");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);

            ob.createCollection(tb_name, collectionSchema);

            // create index
            // IndexParam index_param = new IndexParam("vidx1", "c2");
            // ob.createIndex(tb_name, index_param);
            
            // insert vectors
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            Sqlizable[] ir1 = { new SqlVector(new float[] {1.0f, 2.0f, 3.0f}), new SqlText("{\"doc\": \"oceanbase doc 1\"}") };
            insert_rows.add(ir1);
            Sqlizable[] ir2 = { new SqlVector(new float[] {1.1f, 2.2f, 3.3f}), new SqlText("{\"doc\": \"oceanbase doc 2\"}") };
            insert_rows.add(ir2);
            Sqlizable[] ir3 = { new SqlVector(new float[] {0f, 0f, 0f}), new SqlText("{\"doc\": \"oceanbase doc 3\"}") };
            insert_rows.add(ir3);
            ob.insert(tb_name, new String[] {"c2", "c3"}, insert_rows);
            
            // query
            ArrayList<HashMap<String, Sqlizable>> res = ob.query(tb_name, "c2", "l2", 
                     new float[] {0f, 0f, 0f}, 10,
                     new String[] {"c1", "c3", "c2"},
                     new DataType[] {
                        DataType.INT32,
                        DataType.JSON,
                        DataType.FLOAT_VECTOR
                     }, null);
            if (res != null) {
                for (int i = 0; i < res.size(); i++) {
                    // System.err.printf("%s");
                    for (HashMap.Entry<String, Sqlizable> entry : res.get(i).entrySet()) {
                        System.out.printf("%s : %s, ", entry.getKey(), entry.getValue().toString());
                    }
                    System.out.print("\n");
                }
            } else {
                System.out.println("res is null");
            }

            // delete
            ArrayList<Sqlizable> ids = new ArrayList<>();
            ids.add(new SqlInteger(2));
            ids.add(new SqlInteger(1));
            ob.delete(tb_name, "c1", ids);
        }
    }
}
