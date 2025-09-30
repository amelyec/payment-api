package tz.co.werelay.payment_api.model;

public class PaymentResponse {
    private boolean success;
    private String transactionId;
    private String note;

    public PaymentResponse() {
    }

    public PaymentResponse(boolean success, String transactionId, String note) {
        this.success = success;
        this.transactionId = transactionId;
        this.note = note;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}