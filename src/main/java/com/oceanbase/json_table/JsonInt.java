package com.oceanbase.json_table;

public class JsonInt extends JsonData {
    private Integer val = null;

    public JsonInt(int val) {
        JsonInt.validate(val);
        this.val = Integer.valueOf(val);
    }

    public static void validate(int val) throws IllegalArgumentException {
        // do nothing
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        return val.toString();
    }
}
