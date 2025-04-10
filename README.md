# obvec_jdbc

A JAVA SDK for OceanBase Vector Store and JSON virtual table.

## Installation

- Install with Maven:

1. Add a dependency in your project's `pom.xml` file.

```
<dependency>
  <groupId>com.oceanbase</groupId>
  <artifactId>obvec_jdbc</artifactId>
  <version>1.0.3</version>
</dependency>
```

- Install with source code:

1. Install sdk with maven.  

```
git clone https://github.com/oceanbase/obvec_jdbc.git
cd obvec_jdbc
mvn install
```

2. Add a dependency in your project's `pom.xml` file (make sure the groupId and artifactId match those of the library you cloned).

```
<dependency>
  <groupId>com.oceanbase</groupId>
  <artifactId>obvec_jdbc</artifactId>
  <version>1.0.3</version>
</dependency>
```

## Usage

### SDK for OceanBase Vector Store

- Setup a client

```java
import com.oceanbase.obvec_jdbc.ObVecClient;

String uri = "jdbc:oceanbase://127.0.0.1:2881/test";
String user = "root@test";
String password = "";
String tb_name = "JAVA_TEST";

ObVecClient ob = new ObVecClient(uri, user, password);
```

- Vector table schema definition

```java
import com.oceanbase.obvec_jdbc.DataType;
import com.oceanbase.obvec_jdbc.ObCollectionSchema;
import com.oceanbase.obvec_jdbc.ObFieldSchema;

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
```

- Create the table with a `vector index`

```java
import com.oceanbase.obvec_jdbc.IndexParam;
import com.oceanbase.obvec_jdbc.IndexParams;

IndexParams index_params = new IndexParams();
IndexParam index_param = new IndexParam("vidx1", "c2");
index_params.addIndex(index_param);
collectionSchema.setIndexParams(index_params);

ob.createCollection(tb_name, collectionSchema);
```

- Insert data

```java
import com.oceanbase.obvec_jdbc.SqlInteger;
import com.oceanbase.obvec_jdbc.SqlText;
import com.oceanbase.obvec_jdbc.SqlVector;
import com.oceanbase.obvec_jdbc.Sqlizable;

ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
Sqlizable[] ir1 = { new SqlVector(new float[] {1.0f, 2.0f, 3.0f}), new SqlText("{\"doc\": \"oceanbase doc 1\"}") };
insert_rows.add(ir1);
Sqlizable[] ir2 = { new SqlVector(new float[] {1.1f, 2.2f, 3.3f}), new SqlText("{\"doc\": \"oceanbase doc 2\"}") };
insert_rows.add(ir2);
Sqlizable[] ir3 = { new SqlVector(new float[] {0f, 0f, 0f}), new SqlText("{\"doc\": \"oceanbase doc 3\"}") };
insert_rows.add(ir3);
ob.insert(tb_name, new String[] {"c2", "c3"}, insert_rows);
```

- Perform vector ann search

```java
ArrayList<HashMap<String, Sqlizable>> res = ob.query(tb_name, "c2", "l2", 
            new float[] {0f, 0f, 0f}, 10,
            new String[] {"c1", "c3", "c2"},
            new DataType[] {
            DataType.INT32,
            DataType.JSON,
            DataType.FLOAT_VECTOR
            });
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
```

- Delete data

```java
ArrayList<Sqlizable> ids = new ArrayList<>();
ids.add(new SqlInteger(2));
ids.add(new SqlInteger(1));
ob.delete(tb_name, "c1", ids);
```

### SDK for JSON Virtual Table

- Setup a client

```java
import com.oceanbase.obvec_jdbc.ObVecJsonClient;

String uri = "jdbc:oceanbase://127.0.0.1:2881/test";
String user = "root@test";
String password = "";
ObVecJsonClient client = new ObVecJsonClient(uri, user, password, 0, Level.INFO);
```

- Perform virtual table SQL (Only DDL is supported for JAVA SDK)

```java
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

sql = "ALTER TABLE t2 RENAME TO alter_test";
client.parseJsonTableSQL2NormalSQL(sql);
```
