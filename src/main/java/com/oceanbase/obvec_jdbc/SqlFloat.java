package com.oceanbase.obvec_jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlFloat extends Sqlizable {
    private float float_val;

    public SqlFloat(float val) {
        float_val = val;
    }

    @Override
    public void toDB(int param_idx, PreparedStatement ps) throws SQLException {
        ps.setFloat(param_idx, float_val);
    }

    @Override
    public String toString() {
        return String.valueOf(float_val);
    }
}
