package com.oceanbase.json_table;

// MySQL get
public class JsonBool extends JsonData {
    private Boolean val = null;

    public JsonBool(boolean val) {
        JsonBool.validate(val);
        this.val = Boolean.valueOf(val);
    }

    public static void validate(boolean val) throws IllegalArgumentException {
        // do nothing
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        if (this.val)
            return "1";
        else
            return "0";
    }
}
