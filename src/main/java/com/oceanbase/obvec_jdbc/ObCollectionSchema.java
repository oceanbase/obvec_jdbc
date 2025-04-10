package com.oceanbase.obvec_jdbc;

import java.util.ArrayList;

public class ObCollectionSchema extends Visitable {
    private ArrayList<ObFieldSchema> fields;
    private IndexParams index_params = null;

    public ObCollectionSchema() {
        fields = new ArrayList<>();
        index_params = null;
    }

    public void addField(ObFieldSchema field) {
        fields.add(field);
    }

    public void setIndexParams(IndexParams index_params) {
        this.index_params = index_params;
    }

    @Override
    public String visit() {
        String[] column_defs = new String[fields.size()];
        ArrayList<String> primary_keys = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            ObFieldSchema field = fields.get(i);
            if (field.getIsPrimary()) {
                primary_keys.add(field.getName());
            }
            column_defs[i] = field.visit();
        }
        ArrayList<String> table_schema_strs = new ArrayList<>();
        String joined_column_def = String.join(", ", column_defs);
        table_schema_strs.add(joined_column_def);
        if (!primary_keys.isEmpty()) {
            String primary_key_def = String.format("PRIMARY KEY(%s)", String.join(", ", primary_keys));
            table_schema_strs.add(primary_key_def);
        }
        if (this.index_params != null) {
            table_schema_strs.add(index_params.visit());
        }
        return String.join(", ", table_schema_strs);
    }
    
}
