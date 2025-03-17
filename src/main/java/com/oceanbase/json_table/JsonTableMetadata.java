package com.oceanbase.json_table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JsonTableMetadata {
    private String user_id;
    private HashMap<String, ArrayList<JsonTableColumn>> meta_cache;
    // private static String meta_col_name[] = new String[] {
    //     "user_id",
    //     "jtable_name",
    //     "jcol_id",
    //     "jcol_name",
    //     "jcol_type",
    //     "jcol_nullable",
    //     "jcol_has_default",
    //     "jcol_default",
    // };

    public JsonTableMetadata(String user_id) {
        this.user_id = user_id;
        meta_cache = new HashMap<>();
    }

    public void reflect(Connection conn) {
        meta_cache.clear();

        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = conn.createStatement();
            String sql = String.format(
                "SELECT jtable_name, jcol_id, " + //
                "jcol_name, jcol_type, jcol_nullable, " + //
                "jcol_has_default, jcol_default " + //
                "FROM _meta_json_t " + //
                "WHERE user_id = '%s'",
                user_id
            );

            resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                String table_name = resultSet.getString("jtable_name");
                ArrayList<JsonTableColumn> cols = meta_cache.get(table_name);
                if (cols == null) {
                    cols = new ArrayList<>();
                    meta_cache.put(table_name, cols);
                }
                String jcol_default_str = resultSet.getString("jcol_default");
                JSONParser parser = new JSONParser();
                JSONObject json_obj = null;
                try {
                    json_obj = (JSONObject) parser.parse(jcol_default_str);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Invalid default value: " + jcol_default_str);
                }
                if (json_obj.get("default") == null) {
                    cols.add(new JsonTableColumn(
                        resultSet.getInt("jcol_id"),
                        resultSet.getString("jcol_name"),
                        resultSet.getString("jcol_type"),
                        resultSet.getBoolean("jcol_nullable"),
                        resultSet.getBoolean("jcol_has_default"),
                        null
                    ));
                } else {
                    cols.add(new JsonTableColumn(
                        resultSet.getInt("jcol_id"),
                        resultSet.getString("jcol_name"),
                        resultSet.getString("jcol_type"),
                        resultSet.getBoolean("jcol_nullable"),
                        resultSet.getBoolean("jcol_has_default"),
                        json_obj.get("default").toString()
                    ));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkTableExists(String table_name) {
        return meta_cache.containsKey(table_name);
    }

    public JsonTableColumn checkColumnExists(String table_name, String col_name) {
        if (!checkTableExists(table_name)) return null;

        ArrayList<JsonTableColumn> cols = meta_cache.get(table_name);
        for (JsonTableColumn col : cols) {
            if (col.jcol_name.equals(col_name)) {
                return col;
            }
        }
        return null;
    }

    public void addMeta(String table_name, ArrayList<JsonTableColumn> cols) {
        meta_cache.put(table_name, cols);
    }

    public ArrayList<JsonTableColumn> getMeta(String table_name) {
        return meta_cache.get(table_name);
    }

    public void deleteMeta(String table_name) {
        meta_cache.remove(table_name);
    }

    public void clearMeta() {
        meta_cache.clear();
    }
}
