package com.example.visverbum.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

public class TextAccessibilityService extends AccessibilityService {

    private static final String TAG = "AggressiveTextSvc";
    public static final String ACTION_TEXT_SELECTED = "com.example.visverbum.TEXT_SELECTED";
    public static final String EXTRA_SELECTED_TEXT = "selected_text";

    private String lastBroadcastedText = "";
    private ClipboardManager clipboardManager;
    private long lastCopyAttemptTime = 0;
    private static final long MIN_INTERVAL_BETWEEN_COPY_ATTEMPTS = 2000;

    @Override
    public void onCreate() {
        super.onCreate();
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        Log.d(TAG, "Service onCreate");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        String foundText = null;

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            AccessibilityNodeInfo sourceNode = event.getSource();
            if (sourceNode != null) {
                foundText = getSelectedTextFromNode(sourceNode);
                if (TextUtils.isEmpty(foundText) && canTryCopy()) {
                    foundText = performCopyAndGetText(sourceNode);
                }
                sourceNode.recycle();
            }
        }

        if (TextUtils.isEmpty(foundText)) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                AccessibilityNodeInfo focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focusedNode != null) {
                    foundText = getSelectedTextFromNode(focusedNode);
                    if (TextUtils.isEmpty(foundText) && canTryCopy()) {
                        foundText = performCopyAndGetText(focusedNode);
                    }
                    focusedNode.recycle();
                }

                if (TextUtils.isEmpty(foundText)) {
                    foundText = findSelectedTextRecursive(rootNode, true);
                }
                rootNode.recycle();
            }
        }

        if (!TextUtils.isEmpty(foundText)) {
            if (!foundText.equals(lastBroadcastedText)) {
                lastBroadcastedText = foundText;
                sendSelectedTextBroadcast(foundText);
            }
        } else {
            if (!lastBroadcastedText.isEmpty()) {
                lastBroadcastedText = "";
                sendSelectedTextBroadcast("");
            }
        }
    }

    private boolean canTryCopy() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCopyAttemptTime > MIN_INTERVAL_BETWEEN_COPY_ATTEMPTS) {
            return true;
        }
        return false;
    }

    private boolean nodeSupportsCopyAction(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null || !nodeInfo.isVisibleToUser() || !nodeInfo.isEnabled()) {
            return false;
        }
        List<AccessibilityNodeInfo.AccessibilityAction> actions = nodeInfo.getActionList();
        if (actions != null) {
            for (AccessibilityNodeInfo.AccessibilityAction action : actions) {
                if (action.getId() == AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    private String performCopyAndGetText(AccessibilityNodeInfo nodeInfo) {
        if (clipboardManager == null || nodeInfo == null || !nodeSupportsCopyAction(nodeInfo)) {
            if (nodeInfo != null && !nodeSupportsCopyAction(nodeInfo)) {
                Log.d(TAG, "Node " + nodeInfo.getClassName() + " does not support COPY action or is not suitable for copy.");
            }
            return null;
        }
        lastCopyAttemptTime = System.currentTimeMillis();

        ClipData originalClip = clipboardManager.hasPrimaryClip() ? clipboardManager.getPrimaryClip() : null;
        String copiedText = null;
        boolean copyActionSuccess;

        Log.d(TAG, "Attempting ACTION_COPY on node: " + nodeInfo.getClassName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AccessibilityNodeInfo.AccessibilityAction actionCopy = AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY;
            if (actionCopy != null) {
                copyActionSuccess = nodeInfo.performAction(actionCopy.getId(), null);
            } else {
                Log.e(TAG, "AccessibilityAction.ACTION_COPY is null! Cannot perform action.");
                copyActionSuccess = false;
            }
        } else {
            copyActionSuccess = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_COPY, null);
        }

        if (copyActionSuccess) {
            ClipData newClip = clipboardManager.getPrimaryClip();
            if (newClip != null && newClip.getItemCount() > 0) {
                CharSequence textFromClip = newClip.getItemAt(0).getText();
                if (textFromClip != null) {
                    copiedText = textFromClip.toString().trim();
                    Log.i(TAG, "ACTION_COPY success. Text from clipboard: [" + copiedText + "]");
                } else {
                    Log.w(TAG, "ACTION_COPY success, but clipboard text is null.");
                }
            } else {
                Log.w(TAG, "ACTION_COPY success, but primary clip is null or empty.");
            }
        } else {
            Log.w(TAG, "ACTION_COPY failed on node: " + nodeInfo.getClassName());
        }

        try {
            if (originalClip != null) {
                clipboardManager.setPrimaryClip(originalClip);
            } else {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, ""));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring/clearing clipboard: " + e.getMessage());
        }

        return copiedText;
    }

    private String getSelectedTextFromNode(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null || !nodeInfo.isVisibleToUser()) {
            return null;
        }

        CharSequence nodeText = nodeInfo.getText();
        if (nodeText == null || nodeText.length() == 0) {
            nodeText = nodeInfo.getContentDescription();
        }

        if (nodeText != null && nodeText.length() > 0) {
            int selectionStart = nodeInfo.getTextSelectionStart();
            int selectionEnd = nodeInfo.getTextSelectionEnd();

            if (isValidSelection(selectionStart, selectionEnd, nodeText.length())) {
                return nodeText.subSequence(selectionStart, selectionEnd).toString().trim();
            }
        }
        return null;
    }

    private String findSelectedTextRecursive(AccessibilityNodeInfo parentNode, boolean tryCopyAsFallback) {
        if (parentNode == null || !parentNode.isVisibleToUser()) {
            return null;
        }

        String selectedTextInNode = getSelectedTextFromNode(parentNode);
        if (!TextUtils.isEmpty(selectedTextInNode)) {
            return selectedTextInNode;
        }

        if (tryCopyAsFallback && canTryCopy() && nodeSupportsCopyAction(parentNode)) {
            String copiedText = performCopyAndGetText(parentNode);
            if (!TextUtils.isEmpty(copiedText)) {
                return copiedText;
            }
        }

        for (int i = 0; i < parentNode.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = parentNode.getChild(i);
            if (childNode != null) {
                String foundInChildren = findSelectedTextRecursive(childNode, tryCopyAsFallback);
                childNode.recycle();
                if (!TextUtils.isEmpty(foundInChildren)) {
                    return foundInChildren;
                }
            }
        }
        return null;
    }

    private boolean isValidSelection(int start, int end, int textLength) {
        return start >= 0 && end >= 0 && start < end && end <= textLength;
    }

    private void sendSelectedTextBroadcast(String text) {
        if (text == null) return;
        Intent intent = new Intent(ACTION_TEXT_SELECTED);
        intent.putExtra(EXTRA_SELECTED_TEXT, text.trim());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo serviceInfo = getServiceInfo();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}