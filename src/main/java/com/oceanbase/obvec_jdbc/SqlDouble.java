package com.oceanbase.obvec_jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlDouble extends Sqlizable {
    private double double_val;

    public SqlDouble(double val) {
        double_val = val;
    }

    @Override
    public void toDB(int param_idx, PreparedStatement ps) throws SQLException {
        ps.setDouble(param_idx, double_val);
    }

    @Override
    public String toString() {
        return String.valueOf(double_val);
    }
    
}
