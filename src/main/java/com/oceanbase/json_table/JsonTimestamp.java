package com.oceanbase.json_table;

import java.sql.Timestamp;

public class JsonTimestamp extends JsonData {
    private Timestamp val = null;

    public JsonTimestamp(Timestamp val) {
        JsonTimestamp.validate(val);
        this.val = val;
    }

    public static void validate(Timestamp val) throws IllegalArgumentException {
        // do nothing
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        return "'" + val.toString() + "'";
    }
    
}
