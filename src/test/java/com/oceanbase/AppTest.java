package com.oceanbase;


import java.util.logging.Level;

import com.oceanbase.obvec_jdbc.ObVecJsonClient;

// import java.math.BigDecimal;

// import com.oceanbase.obvec_jdbc.ObVecJsonClient;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        String uri = "jdbc:oceanbase://127.0.0.1:2881/test";
        String user = "root@test";
        String password = "";
        try {
            ObVecJsonClient client = new ObVecJsonClient(uri, user, password, "0", Level.INFO, false);
            client.reset();
            
            String sql = "create table `t2` (c1 int NOT NULL DEFAULT 10, c2 varchar(30) DEFAULT 'ca', c3 varchar not null, c4 decimal(10, 2), c5 timestamp default current_timestamp);";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 CHANGE COLUMN c2 changed_col INT";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 DROP c1";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 MODIFY COLUMN changed_col TIMESTAMP NOT NULL DEFAULT current_timestamp";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 ADD COLUMN email VARCHAR(100) default 'example@example.com'";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 ADD COLUMN email2 VARCHAR(100)";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 RENAME TO alter_test";
            client.parseJsonTableSQL2NormalSQL(sql);

            client.parseJsonTableSQL2NormalSQL(
                "DROP TABLE IF EXISTS t2"
            );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
