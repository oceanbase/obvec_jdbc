package com.oceanbase.obvec_jdbc;

public class ObFieldSchema extends Visitable {
    private String name;
    private DataType dataType;
    private boolean isPrimary = false;
    private boolean isAutoInc = false;
    private boolean isNullable = false;
    private Integer maxLength = null;
    private Integer dim = null;

    public ObFieldSchema(String name, DataType dataType) {
        this.name = name;
        this.dataType = dataType;
        isPrimary = false;
        isAutoInc = false;
        isNullable = false;
        maxLength = null;
        dim = null;
    }

    public String getName() {
        return name;
    }

    public ObFieldSchema Name(String name) {
        this.name = name;
        return this;
    }

    public ObFieldSchema DataType(DataType dataType) {
        this.dataType = dataType;
        return this;
    }

    public boolean getIsPrimary() {
        return isPrimary;
    }

    public ObFieldSchema IsPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
        return this;
    }

    public ObFieldSchema IsAutoInc(boolean isAutoInc) {
        this.isAutoInc = isAutoInc;
        return this;
    }

    public ObFieldSchema IsNullable(boolean isNullable) {
        this.isNullable = isNullable;
        return this;
    }

    public ObFieldSchema MaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public ObFieldSchema Dim(int dim) {
        this.dim = dim;
        return this;
    }

    private boolean checkIsValid() {
        if (dataType == DataType.VARCHAR && maxLength == null) {
            return false;
        }
        if (dataType == DataType.FLOAT_VECTOR && dim == null) {
            return false;
        }
        return true;
    }

    private String visitDataType() {
        if (dataType == DataType.VARCHAR) {
            return String.format("VARCHAR(%d)", this.maxLength);
        }
        if (dataType == DataType.FLOAT_VECTOR) {
            return String.format("VECTOR(%d)", this.dim);
        }
        return this.dataType.Convert2MySQL();
    }

    @Override
    public String visit() {
        if (!checkIsValid()) {
            throw new UnsupportedOperationException("Necessary arguments are not exist");
        }
        String nullable = "NOT NULL";
        if (this.isNullable) {
            nullable = "NULL";
        }
        String auto_inc = "";
        if (this.isPrimary && this.isAutoInc) {
            auto_inc = "AUTO_INCREMENT";
        }
        String visited = String.format("%s %s %s %s", this.name, visitDataType(), auto_inc, nullable);
        return visited;
    }
}
