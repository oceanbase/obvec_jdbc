package com.oceanbase.json_table;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class JsonDecimal extends JsonData {
    private BigDecimal val = null;

    public JsonDecimal(int ndigit, int precise, BigDecimal val) {
        this.val = JsonDecimal.validate(ndigit, precise, val);
    }

    public static BigDecimal validate(int ndigit, int precise, BigDecimal val) throws IllegalArgumentException {
        if (val == null) {
            return null;
        }
        
        String decimalStr = val.stripTrailingZeros().toPlainString();
        String[] parts = decimalStr.split("\\.");
        
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "";
        
        int integerCount = integerPart.replace("-", "").length(); // Exclude the negative sign
        int decimalCount = decimalPart.length();

        if (integerCount + Math.min(decimalCount, precise) > ndigit) {
            throw new IllegalArgumentException("'" + val + "' Range out of Decimal(" + ndigit + ", " + precise + ")");
        }
        
        if (decimalCount > precise) {
            return val.setScale(precise, RoundingMode.DOWN);
        }
        return val;
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        return val.toString();
    }
    
}
