package com.oceanbase.obvec_jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlText extends Sqlizable {
    private String str_val;

    public SqlText(String text) {
        str_val = text;
    }

    @Override
    public void toDB(int param_idx, PreparedStatement ps) throws SQLException {
        ps.setString(param_idx, str_val);
    }

    @Override
    public String toString() {
        return str_val;
    }
}
