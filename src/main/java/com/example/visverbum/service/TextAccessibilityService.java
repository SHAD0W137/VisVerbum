package com.example.visverbum.service; // Убедитесь, что пакет правильный

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
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

public class TextAccessibilityService extends AccessibilityService {

    private static final String TAG = "AggressiveTextSvc";
    public static final String ACTION_TEXT_SELECTED = "com.example.visverbum.TEXT_SELECTED";
    public static final String EXTRA_SELECTED_TEXT = "selected_text";

    private String lastBroadcastedText = "";
    private ClipboardManager clipboardManager;
    private long lastCopyAttemptTime = 0;
    private static final long MIN_INTERVAL_BETWEEN_COPY_ATTEMPTS = 2000; // 2 секунды

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

        // Log.d(TAG, "Event: " + AccessibilityEvent.eventTypeToString(event.getEventType()) +
        // " from " + event.getPackageName() + ", Class: " + event.getClassName());

        String foundText = null;

        // 1. Приоритет - событие изменения выделения
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            AccessibilityNodeInfo sourceNode = event.getSource();
            if (sourceNode != null) {
                foundText = getSelectedTextFromNode(sourceNode);
                // Log.d(TAG, "SELECTION_CHANGED: Direct find: [" + foundText + "] from " + sourceNode.getClassName());
                if (TextUtils.isEmpty(foundText) && canTryCopy()) {
                    // Log.d(TAG, "SELECTION_CHANGED: Direct find failed, trying ACTION_COPY on source.");
                    foundText = performCopyAndGetText(sourceNode);
                }
                sourceNode.recycle();
            }
        }

        // 2. Если текст не найден или событие другое, пытаемся найти в активном/фокусированном узле
        if (TextUtils.isEmpty(foundText)) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                AccessibilityNodeInfo focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focusedNode != null) {
                    // Log.d(TAG, "Trying focused node: " + focusedNode.getClassName());
                    foundText = getSelectedTextFromNode(focusedNode);
                    if (TextUtils.isEmpty(foundText) && canTryCopy()) {
                        // Log.d(TAG, "Focused node find failed, trying ACTION_COPY.");
                        foundText = performCopyAndGetText(focusedNode);
                    }
                    focusedNode.recycle();
                }

                // 3. Если и в фокусированном нет, ищем рекурсивно
                if (TextUtils.isEmpty(foundText)) {
                    // Log.d(TAG, "Focused node failed, trying recursive search in root (with copy attempt if allowed).");
                    foundText = findSelectedTextRecursive(rootNode, true); // true - разрешить попытку копирования в рекурсии
                }
                rootNode.recycle();
            }
        }

        // Обработка найденного текста
        if (!TextUtils.isEmpty(foundText)) {
            if (!foundText.equals(lastBroadcastedText)) {
                lastBroadcastedText = foundText;
                sendSelectedTextBroadcast(foundText);
            }
        } else {
            // Если текст не найден, и ранее что-то было, отправляем пустую строку
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
        // Log.d(TAG, "Skipping ACTION_COPY attempt due to rate limit.");
        return false;
    }

    /**
     * Проверяет, поддерживает ли узел действие копирования и является ли он подходящим для этого.
     */
    private boolean nodeSupportsCopyAction(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null || !nodeInfo.isVisibleToUser() || !nodeInfo.isEnabled()) {
            return false;
        }
        List<AccessibilityNodeInfo.AccessibilityAction> actions = nodeInfo.getActionList();
        if (actions != null) {
            for (AccessibilityNodeInfo.AccessibilityAction action : actions) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (action.getId() == AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY.getId()) {
                        return true;
                    }
                } else {
                    // Для API < 21, AccessibilityAction.ACTION_COPY является int
                    if (action.getId() == AccessibilityNodeInfo.ACTION_COPY) {
                        return true;
                    }
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
            if (actionCopy != null) { // Проверка на случай, если система вернет null для стандартного действия
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
            // Log.d(TAG, "Clipboard restored/cleared after copy attempt.");
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
            nodeText = nodeInfo.getContentDescription(); // Запасной вариант
        }

        if (nodeText != null && nodeText.length() > 0) {
            int selectionStart = nodeInfo.getTextSelectionStart();
            int selectionEnd = nodeInfo.getTextSelectionEnd();

            // Log.d(TAG, "Node: " + nodeInfo.getClassName() + ", Text: [" + nodeText + "], SelStart: " + selectionStart + ", SelEnd: " + selectionEnd);

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
            // Log.d(TAG, "Recursive find, regular get failed. Attempting ACTION_COPY on node: " + parentNode.getClassName());
            String copiedText = performCopyAndGetText(parentNode);
            if (!TextUtils.isEmpty(copiedText)) {
                return copiedText;
            }
        }

        for (int i = 0; i < parentNode.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = parentNode.getChild(i);
            if (childNode != null) {
                String foundInChildren = findSelectedTextRecursive(childNode, tryCopyAsFallback); // Передаем флаг дальше
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
        if (text == null) return; // Не отправляем null, можем отправить пустую строку
        // Log.d(TAG, "Broadcasting text: [" + text + "]");
        Intent intent = new Intent(ACTION_TEXT_SELECTED);
        intent.putExtra(EXTRA_SELECTED_TEXT, text.trim());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "onInterrupt: Accessibility service interrupted.");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo != null) {
            Log.i(TAG, "Service configured with event types: " + AccessibilityEvent.eventTypeToString(serviceInfo.eventTypes));
            Log.i(TAG, "Service flags:");
        }
        Log.i(TAG, "VisVerbum Aggressive Text Service: Connected.");
        Toast.makeText(this, "VisVerbum Service (Aggressive): Подключен", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "VisVerbum Aggressive Text Service: Unbound.");
        Toast.makeText(this, "VisVerbum Service (Aggressive): Отключен", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }
}