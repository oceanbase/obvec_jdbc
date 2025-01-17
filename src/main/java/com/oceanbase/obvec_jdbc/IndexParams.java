package com.oceanbase.obvec_jdbc;

import java.util.ArrayList;

public class IndexParams extends Visitable {
    ArrayList<IndexParam> index_params;

    public IndexParams() {
        index_params = new ArrayList<>();
    }

    public void addIndex(IndexParam index_param) {
        index_params.add(index_param);
    }

    @Override
    public String visit() {
        String[] vidx_defs = new String[index_params.size()];
        for (int i = 0; i < index_params.size(); i++) {
            IndexParam index_param = index_params.get(i);
            vidx_defs[i] = String.format("VECTOR INDEX %s(%s) %s", 
                            index_param.getVidxName(),
                            index_param.getFieldName(),
                            index_params.get(i).visit());
        }
        return String.join(", ", vidx_defs);
    }
}
