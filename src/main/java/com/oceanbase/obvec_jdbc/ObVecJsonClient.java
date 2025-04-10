package com.oceanbase.obvec_jdbc;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;

import org.json.simple.JSONObject;

import com.oceanbase.json_table.JsonTableColumn;
import com.oceanbase.json_table.JsonTableMetadata;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterExpression.ColumnDataType;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.drop.Drop;

import java.sql.SQLException;

public class ObVecJsonClient extends ObVecClient {
    public static String DATA_JSON_TABLE_NAME = "_data_json_t";
    public static String META_JSON_TABLE_NAME = "_meta_json_t";
    private final Logger logger = Logger.getLogger(ObVecJsonClient.class.getName());
    private JsonTableMetadata metadata;
    private String user_id;

    public ObVecJsonClient(String uri, String user, String password, String user_id, Level log_level, boolean skip_create) throws Throwable
    {
        super(uri, user, password);
        this.logger.setLevel(log_level);
        this.metadata = new JsonTableMetadata(user_id);
        this.user_id = user_id;
        
        String createDataJsonTableSQL = "CREATE TABLE IF NOT EXISTS `" + DATA_JSON_TABLE_NAME + "` (\n" + //
                "  `user_id` varchar(128) NOT NULL,\n" + //
                "  `admin_id` varchar(128) NOT NULL,\n" + //
                "  `jtable_name` varchar(512) NOT NULL,\n" + //
                "  `jdata_id` int(11) NOT NULL AUTO_INCREMENT,\n" + //
                "  `jdata` json DEFAULT NULL,\n" + //
                "  PRIMARY KEY (`jdata_id`, `user_id`, `admin_id`, `jtable_name`)\n" + //
                ")";
        String createMetaJsonTableSQL = "CREATE TABLE IF NOT EXISTS `" + META_JSON_TABLE_NAME + "` (\n" + //
                "  `user_id` varchar(128) NOT NULL,\n" + //
                "  `jtable_name` varchar(512) NOT NULL,\n" + //
                "  `jcol_id` int(11) NOT NULL,\n" + //
                "  `jcol_name` varchar(512) NOT NULL,\n" + //
                "  `jcol_type` varchar(128) NOT NULL,\n" + //
                "  `jcol_nullable` tinyint(4) NOT NULL,\n" + //
                "  `jcol_has_default` tinyint(4) NOT NULL,\n" + //
                "  `jcol_default` json DEFAULT NULL,\n" + //
                "  PRIMARY KEY (`user_id`, `jtable_name`, `jcol_id`, `jcol_name`)\n" + //
                ")";
        
        if (!skip_create) {
            try (Statement stmt = this.conn.createStatement()) {
                this.conn.setAutoCommit(false);
                stmt.execute(createDataJsonTableSQL);
                stmt.execute(createMetaJsonTableSQL);
                this.conn.commit();
            } catch (SQLException e) {
                e.printStackTrace();
                if (this.conn != null) {
                    try {
                        this.conn.rollback();
                    } catch (SQLException se) {
                        se.printStackTrace();
                        throw se;
                    }
                }
                throw e;
            }
        }

        try {
            metadata.reflect(this.conn);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void reset() throws Throwable {
        try (Statement stmt = this.conn.createStatement()) {
            this.conn.setAutoCommit(false);
            stmt.execute("TRUNCATE TABLE " + DATA_JSON_TABLE_NAME);
            stmt.execute("TRUNCATE TABLE " + META_JSON_TABLE_NAME);
            this.conn.commit();
            metadata.clearMeta();
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }
    }

    public void refreshMetadata() throws Throwable {
        metadata.reflect(this.conn);
    }

    private String getRealName(String name) {
        if (name.charAt(0) == '`' && name.charAt(name.length() - 1) == '`') {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    private void handleCreateJsonTable(CreateTable stmt) throws Throwable {
        net.sf.jsqlparser.schema.Table table = stmt.getTable();
        if (table == null) {
            throw new IllegalArgumentException("Invalid create table statement: " + stmt.toString());
        }
        
        String table_name = getRealName(table.getName());
        if (table_name == DATA_JSON_TABLE_NAME || table_name == META_JSON_TABLE_NAME) {
            throw new IllegalArgumentException("Invalid table name: " + table_name);
        }
        if (metadata.checkTableExists(table_name)) {
            throw new IllegalArgumentException("Table name duplicated: " + table_name);
        }

        ArrayList<JsonTableColumn> new_meta_cache_items = new ArrayList<>();
        int col_id = 16;
        List<ColumnDefinition> col_defs = stmt.getColumnDefinitions();
        if (col_defs == null) {
            throw new IllegalArgumentException("Create Json Table without column definitions: " + stmt.toString());
        }
        String insert_sql_str = "INSERT INTO " + META_JSON_TABLE_NAME + //
            " (user_id, jtable_name, jcol_id, jcol_name, " + //
            "jcol_type, jcol_nullable, jcol_has_default, jcol_default)" + // 
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insert_schema_stmt = this.conn.prepareStatement(insert_sql_str)) {
            this.conn.setAutoCommit(false);
            for (ColumnDefinition col_def : col_defs) {
                String col_name = getRealName(col_def.getColumnName());
                ColDataType col_data_type = col_def.getColDataType();
                if (col_data_type == null) {
                    throw new IllegalArgumentException("Column data type is not defined: " + col_def.toString());
                }
                String col_type_str = col_data_type.toString().toUpperCase();
                List<String> col_specs = col_def.getColumnSpecs();

                // System.out.println("Column info [ " + col_id + " ]: ");
                // System.out.println(col_name);
                // System.out.println(col_type_str);
                // if (col_specs != null) {
                //     for (String spec : col_specs) {
                //         System.out.println(spec);
                //     }
                // }

                int spec_idx = 0;
                boolean col_has_default = false;
                boolean col_nullable = true;
                String col_default_val = null;
                if (col_specs != null) {
                    while (spec_idx < col_specs.size()) {
                        String col_spec = col_specs.get(spec_idx).toUpperCase();
                        if (col_spec.equals("DEFAULT")) {
                            col_has_default = true;
                            spec_idx += 1;
                            if (spec_idx >= col_specs.size()) {
                                throw new IllegalArgumentException("Invalid column spec: " + col_def.toString());
                            }
                            col_default_val = col_specs.get(spec_idx);
                        } else if (col_spec.equals("NOT")) {
                            spec_idx += 1;
                            if (spec_idx >= col_specs.size()) {
                                throw new IllegalArgumentException("Invalid column spec: " + col_def.toString());
                            }
                            String next_spec = col_specs.get(spec_idx);
                            if (!next_spec.toUpperCase().equals("NULL")) {
                                throw new IllegalArgumentException("Invalid column spec: " + col_def.toString());
                            } else {
                                col_nullable = false;
                            }
                        } else {
                            throw new IllegalArgumentException("Unsupported column spec: " + col_def.toString() + ", " + col_spec);
                        }
                        spec_idx += 1;
                    }
                }

                JsonTableColumn jtable_column = new JsonTableColumn(
                    col_id, col_name, col_type_str, 
                    col_nullable, col_has_default, col_default_val
                );

                // validation
                if (col_has_default && (col_default_val != null)) {
                    ResultSet res = null;
                    Statement sql_stmt = null;
                    try {
                        sql_stmt = this.conn.createStatement();
                        res = sql_stmt.executeQuery("SELECT " + col_default_val);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException("Invalid default value: " + col_def.toString());
                    }
                    if (!jtable_column.validation(res, null)) {
                        throw new IllegalArgumentException("Invalid default value: " + col_def.toString());
                    }
                    try {
                        if (res != null) res.close();
                        if (sql_stmt != null) sql_stmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                new_meta_cache_items.add(jtable_column);
                
                JSONObject json_default = new JSONObject();
                json_default.put("default", col_default_val);
                insert_schema_stmt.setString(1, this.user_id);
                insert_schema_stmt.setString(2, table_name);
                insert_schema_stmt.setInt(3, col_id);
                insert_schema_stmt.setString(4, col_name);
                insert_schema_stmt.setString(5, col_type_str);
                insert_schema_stmt.setInt(6, (col_nullable ? 1 : 0));
                insert_schema_stmt.setInt(7, (col_has_default ? 1 : 0));
                insert_schema_stmt.setString(8, json_default.toJSONString());
                insert_schema_stmt.executeUpdate();
                col_id += 1;
            }
            
            this.conn.commit();
            this.metadata.addMeta(table_name, new_meta_cache_items);
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }

        logger.info("CREATE NEW JSON TABLE: " + table_name + " with columns: " + new_meta_cache_items.toString());
    }

    private String getJsonValueReturningType(String col_type_str) {
        if (col_type_str.startsWith("TINYINT")) {
            return "SIGNED";
        } else if (col_type_str.startsWith("TIMESTAMP")) {
            return "DATETIME";
        } else if (col_type_str.startsWith("INT")) {
            return "SIGNED";
        } else if (col_type_str.startsWith("VARCHAR")) {
            if (col_type_str.equals("VARCHAR")) {
                return "CHAR(255)";
            } else {
                Pattern pattern = Pattern.compile("VARCHAR\\s*\\((\\d+)\\)");
                Matcher matcher = pattern.matcher(col_type_str);
                if (matcher.find()) {
                    String varlen = matcher.group(1);
                    int varlen_val = Integer.parseInt(varlen);
                    return String.format("CHAR(%d)", varlen_val);
                } else {
                    throw new IllegalArgumentException("length is not found in VARCHAR expression");
                }
            }
        } else if (col_type_str.startsWith("DECIMAL")) {
            if (col_type_str.equals("DECIMAL")) {
                return "DECIMAL(10, 0)";
            } else {
                Pattern pattern = Pattern.compile("DECIMAL\\s*\\((\\d+),\\s*(\\d+)\\)");
                Matcher matcher = pattern.matcher(col_type_str);
                if (matcher.find()) {
                    int decimal_ndigits = Integer.parseInt(matcher.group(1));
                    int decimal_p = Integer.parseInt(matcher.group(2));
                    return String.format("DECIMAL(%d, %d)", decimal_ndigits, decimal_p);
                } else {
                    throw new IllegalArgumentException("ndigits and precise is not found in DECIMAL expression");
                }
            }
        }
        throw new IllegalArgumentException("Unsupported column type str: " + col_type_str);
    }

    @SuppressWarnings("unchecked")
    private void handleAlterJTableChangeColumn(String table_name, AlterExpression alter_expr) throws Throwable {
        logger.info("HANDLE ALTER CHANGE COLUMN");

        String col_spec = alter_expr.getOptionalSpecifier();
        if (col_spec == null || !col_spec.toUpperCase().equals("COLUMN")) {
            throw new IllegalArgumentException("Unsupported Alter change stmt: " + alter_expr.toString());
        }

        String old_col_name = alter_expr.getColumnOldName();
        if (old_col_name == null) {
            throw new IllegalArgumentException("Column old name is null");
        }
        List<ColumnDataType> col_types = alter_expr.getColDataTypeList();
        if (col_types == null || col_types.isEmpty()) {
            throw new IllegalArgumentException("Column data type is null");
        } else if (col_types.size() > 1) {
            throw new UnsupportedOperationException("Multiple column data types is not supported");
        }
        
        ColumnDataType data_type = col_types.get(0);
        String new_col_name = data_type.getColumnName();
        ColDataType new_col_data_type = data_type.getColDataType();
        String new_col_type_str = new_col_data_type.toString().toUpperCase();

        String update_meta_sql_str = "UPDATE " + META_JSON_TABLE_NAME + //
            " SET jcol_name = ?, jcol_type = ?, jcol_nullable = ?, jcol_has_default = ?, jcol_default = ?" + //
            " WHERE user_id = ? AND jtable_name = ? AND jcol_name = ?";
        String rename_col_sql_str = String.format("UPDATE " + DATA_JSON_TABLE_NAME + //
            " SET jdata = json_insert(json_remove(jdata, '$.%s'), '$.%s', json_value(jdata, '$.%s'))" + //
            " WHERE admin_id = ? AND jtable_name = ?",
            old_col_name,
            new_col_name,
            old_col_name
        );
        String cast_col_sql_str = String.format("UPDATE " + DATA_JSON_TABLE_NAME + //
            " SET jdata = json_replace(jdata, '$.%s', json_value(jdata, '$.%s' RETURNING %s))" + //
            " WHERE admin_id = ? AND jtable_name = ?",
            new_col_name,
            new_col_name,
            getJsonValueReturningType(new_col_type_str)
        );
        try (PreparedStatement update_meta_sql = this.conn.prepareStatement(update_meta_sql_str);
             PreparedStatement rename_col_sql = this.conn.prepareStatement(rename_col_sql_str);
             PreparedStatement cast_col_sql = this.conn.prepareStatement(cast_col_sql_str)) {
            this.conn.setAutoCommit(false);
            update_meta_sql.setString(1, new_col_name);
            update_meta_sql.setString(2, new_col_type_str);
            update_meta_sql.setInt(3, 1);
            update_meta_sql.setInt(4, 1);
            JSONObject json_default = new JSONObject();
            json_default.put("default", null);
            update_meta_sql.setString(5, json_default.toJSONString());
            update_meta_sql.setString(6, this.user_id);
            update_meta_sql.setString(7, table_name);
            update_meta_sql.setString(8, old_col_name);
            update_meta_sql.executeUpdate();

            rename_col_sql.setString(1, this.user_id);
            rename_col_sql.setString(2, table_name);
            rename_col_sql.executeUpdate();

            cast_col_sql.setString(1, this.user_id);
            cast_col_sql.setString(2, table_name);
            cast_col_sql.executeUpdate();
            this.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }
    }

    private void handleAlterJTableDropColumn(String table_name, AlterExpression alter_expr) throws Throwable {
        logger.info("HANDLE ALTER DROP COLUMN");

        String col_name = alter_expr.getColumnName();
        if (col_name == null) {
            throw new IllegalArgumentException("Column name is null");
        }
        JsonTableColumn col = metadata.checkColumnExists(table_name, col_name);
        if (col == null) {
            throw new IllegalArgumentException("Column does not exist: " + col_name);
        }

        String update_meta_sql_str = "DELETE FROM " + META_JSON_TABLE_NAME + //
            " WHERE user_id = ? AND jtable_name = ? AND jcol_name = ?";
        String delete_col_sql_str = String.format(
            "UPDATE " + DATA_JSON_TABLE_NAME + //
            " SET jdata = json_remove(jdata, '$.%s')" + //
            " WHERE admin_id = ? AND jtable_name = ?",
            col_name
        );
        try (PreparedStatement update_meta_sql = this.conn.prepareStatement(update_meta_sql_str);
             PreparedStatement delete_col_sql = this.conn.prepareStatement(delete_col_sql_str)) {
            this.conn.setAutoCommit(false);
            update_meta_sql.setString(1, this.user_id);
            update_meta_sql.setString(2, table_name);
            update_meta_sql.setString(3, col_name);
            update_meta_sql.executeUpdate();

            delete_col_sql.setString(1, this.user_id);
            delete_col_sql.setString(2, table_name);
            delete_col_sql.executeUpdate();
            this.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }        
    }

    private boolean checkDefaultValue(JsonTableColumn jtable_column) {
        if (jtable_column.jcol_has_default && (jtable_column.jcol_default != null)) {
            ResultSet res = null;
            Statement sql_stmt = null;
            try {
                sql_stmt = this.conn.createStatement();
                res = sql_stmt.executeQuery("SELECT " + jtable_column.jcol_default);
            } catch (SQLException e) {
                return false;
            }
            if (!jtable_column.validation(res, null)) {
                return false;
            }
            try {
                if (res != null) res.close();
                if (sql_stmt != null) sql_stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private JsonTableColumn parseColumnConstraint(List<String> col_specs, String col_type_str) {
        int spec_idx = 0;
        boolean col_has_default = false;
        boolean col_nullable = true;
        String col_default_val = null;
        if (col_specs != null) {
            while (spec_idx < col_specs.size()) {
                String col_spec = col_specs.get(spec_idx).toUpperCase();
                if (col_spec.equals("DEFAULT")) {
                    col_has_default = true;
                    spec_idx += 1;
                    if (spec_idx >= col_specs.size()) {
                        throw new IllegalArgumentException("Invalid column spec: " + col_specs.toString());
                    }
                    col_default_val = col_specs.get(spec_idx);
                } else if (col_spec.equals("NOT")) {
                    spec_idx += 1;
                    if (spec_idx >= col_specs.size()) {
                        throw new IllegalArgumentException("Invalid column spec: " + col_specs.toString());
                    }
                    String next_spec = col_specs.get(spec_idx);
                    if (!next_spec.toUpperCase().equals("NULL")) {
                        throw new IllegalArgumentException("Invalid column spec: " + col_specs.toString());
                    } else {
                        col_nullable = false;
                    }
                } else {
                    throw new IllegalArgumentException(
                        "Unsupported column spec: " + col_specs.toString() + ", " + col_spec
                    );
                }
                spec_idx += 1;
            }
            return new JsonTableColumn(
                0, null, col_type_str, col_nullable, col_has_default, col_default_val
            );
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public void handleAlterJTableModifyColumn(String table_name, AlterExpression alter_expr) throws Throwable {
        logger.info("HANDLE ALTER MODIFY COLUMN");

        List<ColumnDataType> col_types = alter_expr.getColDataTypeList();
        if (col_types == null || col_types.isEmpty()) {
            throw new IllegalArgumentException("Column data type is null");
        } else if (col_types.size() > 1) {
            throw new UnsupportedOperationException("Multiple column data types is not supported");
        }
        
        ColumnDataType data_type = col_types.get(0);
        String col_name = data_type.getColumnName();
        if (col_name == null) {
            throw new IllegalArgumentException("Column name is null");
        }
        JsonTableColumn col = metadata.checkColumnExists(table_name, col_name);
        if (col == null) {
            throw new IllegalArgumentException("Column does not exist: " + col_name);
        }

        List<String> new_col_specs = data_type.getColumnSpecs();
        ColDataType new_col_data_type = data_type.getColDataType();
        String new_col_type_str = new_col_data_type.toString().toUpperCase();
        JsonTableColumn col_constraints = parseColumnConstraint(new_col_specs, new_col_type_str);
        if (!checkDefaultValue(col_constraints)) {
            throw new IllegalArgumentException("Default value check failed: " + col_constraints.jcol_default);
        }

        String update_meta_sql_str = "UPDATE " + META_JSON_TABLE_NAME + //
            " SET jcol_name = ?, jcol_type = ?, jcol_nullable = ?, jcol_has_default = ?, jcol_default = ?" + //
            " WHERE user_id = ? AND jtable_name = ? AND jcol_name = ?";
        String cast_col_sql_str_without_default = String.format("UPDATE " + DATA_JSON_TABLE_NAME + //
            " SET jdata = json_replace(jdata, '$.%s', json_value(jdata, '$.%s' RETURNING %s))" + //
            " WHERE admin_id = ? AND jtable_name = ?",
            col_name,
            col_name,
            getJsonValueReturningType(new_col_type_str)
        );
        String cast_col_sql_str = String.format("UPDATE " + DATA_JSON_TABLE_NAME + //
            " SET jdata = json_replace(jdata, '$.%s', IFNULL(json_value(jdata, '$.%s' RETURNING %s), %s))" + //
            " WHERE admin_id = ? AND jtable_name = ?",
            col_name,
            col_name,
            getJsonValueReturningType(new_col_type_str),
            col_constraints.jdata != null ? col_constraints.jdata.toJson() : "null"
        );
        try (PreparedStatement update_meta_sql = this.conn.prepareStatement(update_meta_sql_str);
             PreparedStatement cast_col_sql_without_default = this.conn.prepareStatement(cast_col_sql_str_without_default);
             PreparedStatement cast_col_sql = this.conn.prepareStatement(cast_col_sql_str)) {
            this.conn.setAutoCommit(false);
            update_meta_sql.setString(1, col_name);
            update_meta_sql.setString(2, new_col_type_str);
            update_meta_sql.setInt(3, col_constraints.jcol_nullable ? 1 : 0);
            update_meta_sql.setInt(4, col_constraints.jcol_has_default ? 1 : 0);
            JSONObject json_default = new JSONObject();
            json_default.put("default", col_constraints.jcol_default);
            update_meta_sql.setString(5, json_default.toJSONString());
            update_meta_sql.setString(6, this.user_id);
            update_meta_sql.setString(7, table_name);
            update_meta_sql.setString(8, col_name);
            update_meta_sql.executeUpdate();
            
            if (col_constraints.jcol_default == null) {
                cast_col_sql_without_default.setString(1, this.user_id);
                cast_col_sql_without_default.setString(2, table_name);
                cast_col_sql_without_default.executeUpdate();
            } else {
                cast_col_sql.setString(1, this.user_id);
                cast_col_sql.setString(2, table_name);
                cast_col_sql.executeUpdate();
            }

            this.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleAlterJTableAddColumn(String table_name, AlterExpression alter_expr) throws Throwable {
        logger.info("HANDLE ALTER ADD COLUMN");

        List<ColumnDataType> col_types = alter_expr.getColDataTypeList();
        if (col_types == null || col_types.isEmpty()) {
            throw new IllegalArgumentException("Column data type is null");
        } else if (col_types.size() > 1) {
            throw new UnsupportedOperationException("Multiple column data types is not supported");
        }

        ColumnDataType data_type = col_types.get(0);
        String col_name = data_type.getColumnName();
        if (col_name == null) {
            throw new IllegalArgumentException("Column name is null");
        }
        JsonTableColumn col = metadata.checkColumnExists(table_name, col_name);
        if (col != null) {
            throw new IllegalArgumentException("Column exists: " + col_name);
        }
        ArrayList<JsonTableColumn> cols = metadata.getMeta(table_name);
        int cur_col_id = 0;
        for (JsonTableColumn jcol : cols) {
            cur_col_id = Integer.max(cur_col_id, jcol.jcol_id);
        }
        cur_col_id += 1;

        List<String> new_col_specs = data_type.getColumnSpecs();
        ColDataType new_col_data_type = data_type.getColDataType();
        String new_col_type_str = new_col_data_type.toString().toUpperCase();
        JsonTableColumn col_constraints = parseColumnConstraint(new_col_specs, new_col_type_str);
        if (!checkDefaultValue(col_constraints)) {
            throw new IllegalArgumentException("Default value check failed: " + col_constraints.jcol_default);
        }

        String insert_sql_str = "INSERT INTO " + META_JSON_TABLE_NAME + //
            " (user_id, jtable_name, jcol_id, jcol_name, " + //
            "jcol_type, jcol_nullable, jcol_has_default, jcol_default)" + // 
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String insert_col_sql_str_without_default = String.format("UPDATE " + DATA_JSON_TABLE_NAME + //
            " SET jdata = json_insert(jdata, '$.%s', null)" + //
            " WHERE admin_id = ? AND jtable_name = ?",
            col_name
        );
        String insert_col_sql_str = String.format("UPDATE " + DATA_JSON_TABLE_NAME + //
            " SET jdata = json_insert(jdata, '$.%s', %s)" + //
            " WHERE admin_id = ? AND jtable_name = ?",
            col_name,
            col_constraints.jdata != null ? col_constraints.jdata.toJson() : "null"
        );
        try (PreparedStatement insert_sql = this.conn.prepareStatement(insert_sql_str);
             PreparedStatement insert_col_sql_without_default = this.conn.prepareStatement(insert_col_sql_str_without_default);
             PreparedStatement insert_col_sql = this.conn.prepareStatement(insert_col_sql_str)) {
            this.conn.setAutoCommit(false);
            insert_sql.setString(1, this.user_id);
            insert_sql.setString(2, table_name);
            insert_sql.setInt(3, cur_col_id);
            insert_sql.setString(4, col_name);
            insert_sql.setString(5, new_col_type_str);
            insert_sql.setInt(6, col_constraints.jcol_nullable ? 1 : 0);
            insert_sql.setInt(7, col_constraints.jcol_has_default ? 1 : 0);
            JSONObject json_default = new JSONObject();
            json_default.put("default", col_constraints.jcol_default);
            insert_sql.setString(8, json_default.toJSONString());
            insert_sql.executeUpdate();
            
            if (col_constraints.jcol_default == null) {
                insert_col_sql_without_default.setString(1, this.user_id);
                insert_col_sql_without_default.setString(2, table_name);
                insert_col_sql_without_default.executeUpdate();
            } else {
                insert_col_sql.setString(1, this.user_id);
                insert_col_sql.setString(2, table_name);
                insert_col_sql.executeUpdate();
            }

            this.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }
    }

    private void handleAlterJTableRenameTable(String table_name, AlterExpression alter_expr) throws Throwable {
        logger.info("HANDLE ALTER RENAME TABLE");

        String new_table_name = alter_expr.getNewTableName();
        if (new_table_name == null) {
            throw new IllegalArgumentException("New table name does not exist");
        }

        if (metadata.checkTableExists(new_table_name)) {
            throw new IllegalArgumentException("Table exists: " + new_table_name);
        }

        String update_name_sql_str = "UPDATE " + META_JSON_TABLE_NAME + //
            " SET jtable_name = ?" + //
            " WHERE user_id = ? AND jtable_name = ?";
        
        try (PreparedStatement update_name_sql = this.conn.prepareStatement(update_name_sql_str)) {
            this.conn.setAutoCommit(false);
            update_name_sql.setString(1, new_table_name);
            update_name_sql.setString(2, this.user_id);
            update_name_sql.setString(3, table_name);
            update_name_sql.executeUpdate();
            this.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }
    }

    private void handleAlterJsonTable(Alter stmt) throws Throwable {
        net.sf.jsqlparser.schema.Table table = stmt.getTable();
        if (table == null) {
            throw new IllegalArgumentException("Invalid create table statement: " + stmt.toString());
        }

        String table_name = getRealName(table.getName());
        if (!metadata.checkTableExists(table_name)) {
            throw new IllegalArgumentException("Table does not exist: " + table_name);
        }

        List<AlterExpression> alter_exprs = stmt.getAlterExpressions();
        if (alter_exprs == null) {
            throw new IllegalArgumentException("Alter Expression is null");
        }

        for (AlterExpression alter_expr : alter_exprs) {
            AlterOperation op = alter_expr.getOperation();
            if (op == AlterOperation.CHANGE) {
                handleAlterJTableChangeColumn(table_name, alter_expr);
            } else if (op == AlterOperation.DROP) {
                handleAlterJTableDropColumn(table_name, alter_expr);
            } else if (op == AlterOperation.ADD) {
                handleAlterJTableAddColumn(table_name, alter_expr);
            } else if (op == AlterOperation.MODIFY) {
                handleAlterJTableModifyColumn(table_name, alter_expr);
            } else if (op == AlterOperation.RENAME_TABLE) {
                handleAlterJTableRenameTable(table_name, alter_expr);
            } else {
                throw new IllegalArgumentException("Invalid alter operation: " + op);
            }
        }

        try {
            metadata.reflect(this.conn);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void handleDropTable(Drop stmt) throws Throwable {
        String type = stmt.getType();
        if (!type.toUpperCase().equals("TABLE")) {
            throw new IllegalArgumentException("DROP " + type + " is not supported");
        }
        
        net.sf.jsqlparser.schema.Table table = stmt.getName();
        if (table == null) {
            throw new IllegalArgumentException("Invalid drop table statement: " + stmt.toString());
        }

        String table_name = getRealName(table.getName());
        boolean table_exists = metadata.checkTableExists(table_name);
        if (!table_exists) {
            if (stmt.isIfExists()) {
                return;
            }
            throw new IllegalArgumentException("Table " + table_name + " does not exists");
        }

        String delete_meta_sql_str = "DELETE FROM " + META_JSON_TABLE_NAME + //
            " WHERE user_id = ? AND jtable_name = ?";
        String delete_data_sql_str = "DELETE FROM " + DATA_JSON_TABLE_NAME + //
            " WHERE admin_id = ? AND jtable_name = ?";
        
        try (PreparedStatement delete_meta_sql = this.conn.prepareStatement(delete_meta_sql_str);
            PreparedStatement delete_data_sql = this.conn.prepareStatement(delete_data_sql_str)) {
           this.conn.setAutoCommit(false);
           delete_meta_sql.setString(1, this.user_id);
           delete_meta_sql.setString(2, table_name);
           delete_meta_sql.executeUpdate();

           delete_data_sql.setString(1, this.user_id);
           delete_data_sql.setString(2, table_name);
           delete_data_sql.executeUpdate();
           this.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException se) {
                    se.printStackTrace();
                    throw se;
                }
            }
            throw e;
        }

        try {
            metadata.reflect(this.conn);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public String parseJsonTableSQL2NormalSQL(String json_table_sql) throws Throwable {
        net.sf.jsqlparser.statement.Statement stmt = CCJSqlParserUtil.parse(json_table_sql);
        if (stmt instanceof CreateTable) {
            handleCreateJsonTable((CreateTable)stmt);
        } else if (stmt instanceof Alter) {
            handleAlterJsonTable((Alter)stmt);
        } else if (stmt instanceof Drop) {
            handleDropTable((Drop)stmt);
        } else {
            throw new UnsupportedOperationException("Unsupported JSON Table SQL: " + json_table_sql);
        }
        return null;
    }
}
