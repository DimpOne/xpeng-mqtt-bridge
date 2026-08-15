package com.schwoi.xpengbridge;

public final class UiNode {
    public final String resourceId;
    public final String text;

    public UiNode(String resourceId, String text) {
        this.resourceId = resourceId == null ? "" : resourceId;
        this.text = text == null ? "" : text.trim();
    }
}
