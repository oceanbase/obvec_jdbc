package com.oceanbase.json_table;

public class JsonVarchar extends JsonData {
    private String val = null;

    public JsonVarchar(int length, String val) {
        JsonVarchar.valid(length, val);
        this.val = val;
    }

    public static void valid(int length, String val) throws IllegalArgumentException {
        if (val == null) {
            return;
        }
        if (val.length() > length) {
            throw new IllegalArgumentException("value length is longer than " + length);
        }
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        String cp = new String(val);
        return "'" + cp + "'";
    }
}
