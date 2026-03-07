package com.chatbox.chatbox.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private Long bagTemplateId;
    private String designSnapshot;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String customerEmail;
    private Integer quantity;
}
