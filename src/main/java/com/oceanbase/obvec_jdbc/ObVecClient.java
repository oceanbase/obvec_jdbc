package com.oceanbase.obvec_jdbc;

import java.rmi.UnexpectedException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ObVecClient {
    protected Connection conn = null;

    public ObVecClient(String uri, String user, String password) throws Throwable
    {
        try {
            conn = DriverManager.getConnection(uri, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void setHNSWEfSearch(int val) throws Throwable {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("SET @@ob_hnsw_ef_search = %d", val);
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public int getHNSWEfSearch() throws Throwable {
        Statement statement = null;
        ResultSet resultSet = null;
        ArrayList<Integer> res = new ArrayList<>();

        try {
            statement = conn.createStatement();
            String sql = String.format("show variables like 'ob_hnsw_ef_search'");
            resultSet = statement.executeQuery(sql);
            
            while (resultSet.next()) {
                res.add(resultSet.getInt("Value"));
            }

            if (res.size() != 1) {
                throw new UnexpectedException("ob_hnsw_ef_search is a single variable");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return res.get(0);
    }

    public void dropCollection(String table_name) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("DROP TABLE IF EXISTS %s", table_name);
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public boolean hasCollection(String table_name) throws Throwable
    {
        boolean exists = false;
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, table_name, null);
            if (rs.next()) {
                exists = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return exists;
    }

    public void createCollection(String table_name, ObCollectionSchema collection) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("CREATE TABLE %s (%s)", table_name, collection.visit());
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void createIndex(String table_name, IndexParam index_param) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("CREATE VECTOR INDEX %s on %s(%s) %s", 
                            index_param.getVidxName(),
                            table_name,
                            index_param.getFieldName(),
                            index_param.visit());
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void insert(String table_name, String[] column_names, ArrayList<Sqlizable[]> rows) throws Throwable
    {
        if (rows.isEmpty()) {
            return;
        }

        try {
            conn.setAutoCommit(false);
            // set prepared statement
            ArrayList<String> param_str_list = new ArrayList<String>(Collections.nCopies(column_names.length, "?"));
            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                            table_name,
                            String.join(", ", column_names),
                            String.join(", ", param_str_list));
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            // do insertion
            for (int i = 0; i < rows.size(); i++) {
                Sqlizable[] cols = rows.get(i);
                if (cols.length != column_names.length) {
                    throw new UnsupportedOperationException("column size missmatch");
                }
                for (int col_id = 0; col_id < cols.length; col_id++) {
                    cols[col_id].toDB(col_id + 1, preparedStatement);
                }
                preparedStatement.executeUpdate();
            }
            // commit
            conn.commit();
        } catch (Throwable e) {
            // rollback
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transaction rolled back");
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                    throw rollbackEx;
                }
            }
            e.printStackTrace();
            throw e;
        } finally {
            // reset autocommit
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }

    public void delete(String table_name, String primary_key_name, ArrayList<Sqlizable> primary_keys) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            ArrayList<String> param_str_list = new ArrayList<String>(Collections.nCopies(primary_keys.size(), "?"));
            String sql = String.format("DELETE FROM %s WHERE %s in (%s)", 
                            table_name,
                            primary_key_name,
                            String.join(", ", param_str_list));
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for (int i = 0; i < primary_keys.size(); i++) {
                primary_keys.get(i).toDB(i + 1, preparedStatement);
            }
            preparedStatement.executeUpdate();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public ArrayList<HashMap<String, Sqlizable>> query(String table_name, 
                      String vec_col_name,
                      String metric_type,
                      float[] qv,
                      int topk,
                      String[] output_fields,
                      DataType[] output_datatypes,
                      String where_expr) throws Throwable
    {
        ArrayList<HashMap<String, Sqlizable>> res = new ArrayList<>();

        Statement statement = null;
        ResultSet resultSet = null;

        String metric_type_lower = metric_type.toLowerCase();
        if (!metric_type_lower.equals("l2") &&
            !metric_type_lower.equals("ip") &&
            !metric_type_lower.equals("cosine")) {
            throw new UnsupportedOperationException("Metric Type is not supported.");
        }
        String dist_func = "l2_distance";
        if (metric_type_lower.equals("ip")) {
            dist_func = "negative_inner_product";
        } else if (metric_type_lower.equals("cosine")) {
            dist_func = "cosine_distance";
        }
        
        String[] vec_str = new String[qv.length];
        for (int i = 0; i < qv.length; i++) {
            vec_str[i] = String.valueOf(qv[i]);
        }

        try {
            statement = conn.createStatement();            
            
            String sql = String.format("SELECT %s FROM %s WHERE %s ORDER BY %s(%s, '[%s]') APPROXIMATE LIMIT %d",
                            String.join(", ", output_fields),
                            table_name,
                            (where_expr == null) ? "1" : where_expr,
                            dist_func,
                            vec_col_name,
                            String.join(", ", vec_str),
                            topk);
            resultSet = statement.executeQuery(sql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int project_count = metaData.getColumnCount();
            
            while (resultSet.next()) {
                HashMap<String, Sqlizable> row = new HashMap<>();
                
                for (int i = 1; i <= project_count; i++) {
                    String columnName = metaData.getColumnName(i);
                    Sqlizable sqlizable = SqlizableFactory.build(output_datatypes[i - 1], resultSet, columnName);
                    row.put(columnName, sqlizable);
                }

                res.add(row);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return res;
    }
}
