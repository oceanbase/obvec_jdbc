package com.oceanbase.json_table;

import java.sql.ResultSet;

public class JsonDataFactory {
    private JsonDataType dataType;
    private int jsonVarcharLength;
    private int jsonDecimalDigits;
    private int jsonDecimalPrecise;

    public JsonDataFactory(JsonDataType dataType) {
        this.dataType = dataType;
    }

    public JsonDataFactory(JsonDataType dataType, int varchar_length) {
        this.dataType = dataType;
        this.jsonVarcharLength = varchar_length;
    }

    public JsonDataFactory(JsonDataType dataType, int decimal_ndigits, int decimal_p) {
        this.dataType = dataType;
        this.jsonDecimalDigits = decimal_ndigits;
        this.jsonDecimalPrecise = decimal_p;
    }

    public JsonData build(ResultSet db_res, String col_name) throws Exception {
        if (!db_res.next()) {
            throw new IllegalArgumentException("Invalid data.");
        }
        switch (this.dataType) {
            case J_BOOL:
                {
                    if (col_name != null) {
                        return new JsonBool(db_res.getBoolean(col_name));
                    } else {
                        return new JsonBool(db_res.getBoolean(1));
                    }
                }
            case J_INT:
                {
                    if (col_name != null) {
                        return new JsonInt(db_res.getInt(col_name));
                    } else {
                        return new JsonInt(db_res.getInt(1));
                    }
                }
            case J_TIMESTAMP:
                {
                    if (col_name != null) {
                        return new JsonTimestamp(db_res.getTimestamp(col_name));
                    } else {
                        return new JsonTimestamp(db_res.getTimestamp(1));
                    }
                }            
            case J_DECIMAL:
                {
                    if (col_name != null) {
                        return new JsonDecimal(
                            this.jsonDecimalDigits,
                            this.jsonDecimalPrecise,
                            db_res.getBigDecimal(col_name)
                        );
                    } else {
                        return new JsonDecimal(
                            this.jsonDecimalDigits,
                            this.jsonDecimalPrecise, 
                            db_res.getBigDecimal(1)
                        );
                    }
                }
            case J_VARCHAR:
                {
                    if (col_name != null) {
                        return new JsonVarchar(
                            this.jsonVarcharLength,
                            db_res.getString(col_name)
                        );
                    } else {
                        return new JsonVarchar(
                            this.jsonVarcharLength,
                            db_res.getString(1)
                        );
                    }
                }
            default: {
                throw new IllegalArgumentException("Unsupported data type: " + this.dataType);
            }
        }
    }
}
