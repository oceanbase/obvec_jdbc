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

// import java.math.BigDecimal;

// import com.oceanbase.obvec_jdbc.ObVecJsonClient;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class VecClientTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public VecClientTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( VecClientTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
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
                
                tb_name = "JAVA_TEST2";
                ob.dropCollection(tb_name);
                ObCollectionSchema collectionSchema2 = new ObCollectionSchema();
                ObFieldSchema c1_field2 = new ObFieldSchema("id", DataType.INT32);
                c1_field2.IsAutoInc(true).IsPrimary(true);
                collectionSchema2.addField(c1_field2);
                ObFieldSchema c2_field2 = new ObFieldSchema("vec", DataType.FLOAT_VECTOR);
                c2_field2.Dim(3);
                collectionSchema2.addField(c2_field2);
                ob.createCollection(tb_name, collectionSchema2);

                ArrayList<Sqlizable[]> insert_rows2 = new ArrayList<>();
                Sqlizable[] ir21 = { new SqlVector(new float[] {1.0f, 2.0f, 3.0f}) };
                insert_rows2.add(ir21);
                Sqlizable[] ir22 = { new SqlVector(new float[] {1.1f, 2.2f, 3.3f}) };
                insert_rows2.add(ir22);
                Sqlizable[] ir23 = { new SqlVector(new float[] {0f, 0f, 0f}) };
                insert_rows2.add(ir23);
                ob.insert(tb_name, new String[] {"vec"}, insert_rows2);

                IndexParam index_param2 = new IndexParam("vidx2", "vec");
                index_param2.MetricType("inner_product");
                ob.createIndex(tb_name, index_param2);

                ArrayList<HashMap<String, Sqlizable>> res2 = ob.query(tb_name, "vec", "ip", 
                        new float[] {1f, 1f, 1f}, 10,
                        new String[] {"*"},
                        new DataType[] {
                            DataType.INT32,
                            DataType.FLOAT_VECTOR
                        }, null);
                if (res2 != null) {
                    for (int i = 0; i < res2.size(); i++) {
                        for (HashMap.Entry<String, Sqlizable> entry : res2.get(i).entrySet()) {
                            System.out.printf("%s : %s, ", entry.getKey(), entry.getValue().toString());
                        }
                        System.out.print("\n");
                    }
                } else {
                    System.out.println("res is null");
                }
                
                int ef_search_before = ob.getHNSWEfSearch();
                System.out.println("before set ef_search: " + ef_search_before);
                ob.setHNSWEfSearch(200);
                int ef_search = ob.getHNSWEfSearch();
                System.out.println("ef_search: " + ef_search);
                
                ObVecClient client2 = new ObVecClient(uri, user, password);
                int client2_efsearch = client2.getHNSWEfSearch();
                System.out.println("client2 ef_search: " + client2_efsearch);
                client2.setHNSWEfSearch(100);

                ef_search = ob.getHNSWEfSearch();
                client2_efsearch = client2.getHNSWEfSearch();
                System.out.println("ef_search: " + ef_search + " client2 ef_search: " + client2_efsearch);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
