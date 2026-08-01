package com.chatbox.chatbox.model;

public enum ProductSaleMode {
    COMBO,
    RETAIL,
    B2B,
    COMBO_AND_B2B,
    RETAIL_AND_B2B;

    public boolean includesCombo() {
        return this == COMBO || this == COMBO_AND_B2B;
    }
}
