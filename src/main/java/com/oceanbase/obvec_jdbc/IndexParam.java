package com.oceanbase.obvec_jdbc;

public class IndexParam extends Visitable {
    private String vidx_name;
    private String vector_field_name;
    private int m = 16;
    private int ef_construction = 200;
    private int ef_search = 40;
    private String lib = "vsag";
    private String metric_type = "l2";

    public IndexParam(String vidx_name, String vector_field_name) {
        this.vidx_name = vidx_name;
        this.vector_field_name = vector_field_name;
    }

    public IndexParam M(int m) {
        this.m = m;
        return this;
    }

    public IndexParam EfConstruction(int ef_construction) {
        this.ef_construction = ef_construction;
        return this;
    }

    public IndexParam EfSearch(int ef_search) {
        this.ef_search = ef_search;
        return this;
    }

    public IndexParam Lib(String lib) {
        this.lib = lib;
        return this;
    }

    private boolean checkMetricType(String metric_type) {
        String metric_type_lower = metric_type.toLowerCase();
        return metric_type_lower.equals("l2") || metric_type_lower.equals("inner_product");
    }

    public IndexParam MetricType(String metric_type) {
        if (!checkMetricType(metric_type)) {
            throw new UnsupportedOperationException("Metric Type is not supported.");
        }
        this.metric_type = metric_type;
        return this;
    }

    public String getVidxName() {
        return this.vidx_name;
    }

    public String getFieldName() {
        return this.vector_field_name;
    }

    @Override
    public String visit() {
        return String.format("WITH(m=%d, ef_construction=%d, ef_search=%d, lib=%s, distance=%s, type=hnsw)", 
                      this.m, this.ef_construction, this.ef_search,
                      this.lib, this.metric_type);
    }
}
