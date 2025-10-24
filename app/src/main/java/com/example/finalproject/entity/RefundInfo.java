package com.example.finalproject.entity;

public class RefundInfo {

    private String account_name;
    private String account_number;
    private String bank_name;
    private String reason;

    public RefundInfo() {
    }
    public RefundInfo(String account_name, String account_number, String bank_name, String reason) {
        this.account_name = account_name;
        this.account_number = account_number;
        this.bank_name = bank_name;
        this.reason = reason;
    }
    public String getAccount_name() {
        return account_name;
    }
    public void setAccount_name(String account_name) {
        this.account_name = account_name;
    }
    public String getAccount_number() {
        return account_number;
    }
    public void setAccount_number(String account_number) {
        this.account_number = account_number;
    }
    public String getBank_name() {
        return bank_name;
    }
    public void setBank_name(String bank_name) {
        this.bank_name = bank_name;
    }
    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
}
