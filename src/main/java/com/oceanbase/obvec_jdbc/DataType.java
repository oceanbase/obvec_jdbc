package com.oceanbase.obvec_jdbc;

public enum DataType {
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,

    FLOAT,
    DOUBLE,

    STRING,
    VARCHAR,
    JSON,

    FLOAT_VECTOR;

    public String Convert2MySQL() {
        switch(this) {
            case BOOL: return "TINYINT";
            case INT8: return "TINYINT";
            case INT16: return "SMALLINT";
            case INT32: return "INT";
            case INT64: return "BIGINT";
            case FLOAT: return "FLOAT";
            case DOUBLE: return "DOUBLE";
            case STRING: return "LONGTEXT";
            case VARCHAR: return "VARCHAR";
            case JSON: return "JSON";
            case FLOAT_VECTOR: return "VECTOR";
            default: return null;
        }
    }
}
