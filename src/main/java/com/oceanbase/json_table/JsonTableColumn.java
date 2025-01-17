package com.oceanbase.json_table;

import java.util.regex.Pattern;
import java.sql.ResultSet;
import java.util.regex.Matcher;

public class JsonTableColumn {
    public int jcol_id;
    public String jcol_name;
    public String jcol_type;
    public boolean jcol_nullable;
    public boolean jcol_has_default;
    public String jcol_default;
    public JsonDataFactory jcol_model;
    public JsonData jdata;

    public static JsonDataFactory getJsonDataFactory(String jcol_type) {
        if (jcol_type.startsWith("TINYINT")) {
            return new JsonDataFactory(JsonDataType.J_BOOL);
        }
        if (jcol_type.startsWith("TIMESTAMP")) {
            return new JsonDataFactory(JsonDataType.J_TIMESTAMP);
        }
        if (jcol_type.startsWith("INT")) {
            return new JsonDataFactory(JsonDataType.J_INT);
        }
        if (jcol_type.startsWith("VARCHAR")) {
            if (jcol_type.equals("VARCHAR")) {
                return new JsonDataFactory(JsonDataType.J_VARCHAR, 255);
            } else {
                Pattern pattern = Pattern.compile("VARCHAR\\s*\\((\\d+)\\)");
                Matcher matcher = pattern.matcher(jcol_type);
                if (matcher.find()) {
                    String varlen = matcher.group(1);
                    int varlen_val = Integer.parseInt(varlen);
                    return new JsonDataFactory(JsonDataType.J_VARCHAR, varlen_val);
                } else {
                    throw new IllegalArgumentException("length is not found in VARCHAR expression");
                }
            }
        }
        if (jcol_type.startsWith("DECIMAL")) {
            if (jcol_type.equals("DECIMAL")) {
                return new JsonDataFactory(JsonDataType.J_DECIMAL, 10, 0);
            } else {
                Pattern pattern = Pattern.compile("DECIMAL\\s*\\((\\d+),\\s*(\\d+)\\)");
                Matcher matcher = pattern.matcher(jcol_type);
                if (matcher.find()) {
                    int decimal_ndigits = Integer.parseInt(matcher.group(1));
                    int decimal_p = Integer.parseInt(matcher.group(2));
                    return new JsonDataFactory(JsonDataType.J_DECIMAL, decimal_ndigits, decimal_p);
                } else {
                    throw new IllegalArgumentException("ndigits and precise is not found in DECIMAL expression");
                }
            }
        }
        throw new IllegalArgumentException("Invalid column type: " + jcol_type);
    }

    public JsonTableColumn(
        int jcol_id,
        String jcol_name,
        String jcol_type,
        boolean jcol_nullable,
        boolean jcol_has_default,
        String jcol_default
    ) {
        if ((!jcol_nullable) && jcol_has_default && (jcol_default == null)) {
            throw new IllegalArgumentException("Invalid default value for " + jcol_name);
        }
        this.jcol_id = jcol_id;
        this.jcol_name = jcol_name;
        this.jcol_type = jcol_type;
        this.jcol_nullable = jcol_nullable;
        this.jcol_has_default = jcol_has_default;
        this.jcol_default = jcol_default;
        this.jcol_model = JsonTableColumn.getJsonDataFactory(jcol_type);
        this.jdata = null;
    }

    public boolean validation(ResultSet res, String col_name) {
        try {
            this.jdata = jcol_model.build(res, col_name);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + //
            "jcol_id:" + jcol_id + ", " + //
            "jcol_name:" + jcol_name + ", " + //
            "jcol_type:" + jcol_type + ", " + //
            "jcol_nullable:" + jcol_nullable + ", " + //
            "jcol_has_default:" + jcol_has_default + ", " + //
            "jcol_default:" + jcol_default + ", " + //
            "}";
    }
}
