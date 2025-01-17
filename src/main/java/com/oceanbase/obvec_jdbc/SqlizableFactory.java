package com.oceanbase.obvec_jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlizableFactory {
    public static Sqlizable build(DataType dataType, ResultSet db_res, String col_name) throws SQLException {
        switch (dataType) {
            case BOOL:
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            {
                return new SqlInteger(db_res.getInt(col_name));
            }
            case FLOAT:
            {
                return new SqlFloat(db_res.getFloat(col_name));
            }
            case DOUBLE:
            {
                return new SqlDouble(db_res.getDouble(col_name));
            }
            case STRING:
            case VARCHAR:
            case JSON: // TODO: use org.json
            {
                return new SqlText(db_res.getString(col_name));
            }
            case FLOAT_VECTOR:
            {
                return new SqlText(db_res.getString(col_name));
            }
            default: return null;
        }
    }
}
