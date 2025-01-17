package com.oceanbase.obvec_jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class SqlVector extends Sqlizable {
    private float[] vector_val;

    public SqlVector(float[] vector) {
        vector_val = vector;
    }

    @Override
    public void toDB(int param_idx, PreparedStatement ps) throws SQLException {
        ArrayList<String> float_str = new ArrayList<>();
        for (int i = 0; i < vector_val.length; i++) {
            float_str.add(String.valueOf(vector_val[i]));
        }
        String vector_str = "[" + String.join(", ", float_str) + "]";
        ps.setString(param_idx, vector_str);
    }

    @Override
    public String toString() {
        ArrayList<String> float_str = new ArrayList<>();
        for (int i = 0; i < vector_val.length; i++) {
            float_str.add(String.valueOf(vector_val[i]));
        }
        return "[" + String.join(", ", float_str) + "]";
    }
}
