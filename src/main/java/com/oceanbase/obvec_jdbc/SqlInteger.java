package com.oceanbase.obvec_jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlInteger extends Sqlizable {
    private int int_val;

    public SqlInteger(int val) {
        int_val = val;
    }

    @Override
    public void toDB(int param_idx, PreparedStatement ps) throws SQLException {
        ps.setInt(param_idx, int_val);
    }

    @Override
    public String toString() {
        return String.valueOf(int_val);
    }   
}
