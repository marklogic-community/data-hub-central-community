package com.marklogic.dhf.config;

public class FixedFieldProp {

    private String fieldName;
    private int from;
    private int thru;
    private int len;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getThru() {
        return thru;
    }

    public void setThru(int thru) {
        this.thru = thru;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }
}
