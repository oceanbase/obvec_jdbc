package com.oceanbase.obvec_jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class Sqlizable {
    public abstract void toDB(int param_idx, PreparedStatement ps) throws SQLException;
    public abstract String toString();
}
