package com.clinic.auth.common;

// Đánh dấu chỗ trong khung còn chờ người phụ trách service viết tiếp; API trả 501
public class NotImplementedYetException extends RuntimeException {

    public NotImplementedYetException(String task) {
        super("Chưa làm: " + task);
    }
}
