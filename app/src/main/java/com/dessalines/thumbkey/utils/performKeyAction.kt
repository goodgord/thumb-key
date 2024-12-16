package com.dessalines.thumbkey.utils

import androidx.compose.runtime.MutableState
import com.dessalines.thumbkey.IMEService
import com.dessalines.thumbkey.keyboards.KeyboardDefinitionSettings
import com.dessalines.thumbkey.keyboards.KeyboardMode
import com.dessalines.thumbkey.keyboards.KeyboardPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun performKeyAction(
    action: KeyAction,
    ime: IMEService,
    autoCapitalize: Boolean,
    keyboardSettings: KeyboardDefinitionSettings,
    onToggleShiftMode: (enable: Boolean) -> Unit,
    onToggleNumericMode: (enable: Boolean) -> Unit,
    onToggleEmojiMode: (enable: Boolean) -> Unit,
    onToggleCapsLock: () -> Unit,
    onAutoCapitalize: (enable: Boolean) -> Unit,
    onSwitchLanguage: () -> Unit,
    onChangePosition: ((old: KeyboardPosition) -> KeyboardPosition) -> Unit,
) {
    when (action) {
        is KeyAction.CommitText -> {
            ime.commitText(action.text, 1)
            if (autoCapitalize) {
                val mode = getKeyboardMode(ime, autoCapitalize)
                onAutoCapitalize(mode == KeyboardMode.SHIFTED)
            }
        }
        is KeyAction.SendEvent -> {
            ime.currentInputConnection?.sendKeyEvent(action.event)
        }
        is KeyAction.DeleteLastWord -> {
            ime.currentInputConnection?.let { ic ->
                // Get text before cursor
                var beforeCursor = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
                // Find the last word boundary
                val lastSpace = beforeCursor.lastIndexOf(' ')
                if (lastSpace != -1) {
                    beforeCursor = beforeCursor.substring(lastSpace + 1)
                }
                // Delete the characters
                for (i in beforeCursor.indices) {
                    ic.deleteSurroundingText(1, 0)
                }
                // Reset prediction after word deletion
                ime.predictionManager.onWordComplete()
            }
        }
        is KeyAction.ReplaceLastWord -> {
            ime.currentInputConnection?.let { ic ->
                // Delete the last word
                var beforeCursor = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
                val lastSpace = beforeCursor.lastIndexOf(' ')
                if (lastSpace != -1) {
                    beforeCursor = beforeCursor.substring(lastSpace + 1)
                }
                for (i in beforeCursor.indices) {
                    ic.deleteSurroundingText(1, 0)
                }
                // Insert the new word
                ic.commitText(action.replacement, 1)
                // Reset prediction after word replacement
                ime.predictionManager.onWordComplete()
            }
        }
        is KeyAction.ToggleShiftMode -> onToggleShiftMode(action.enable)
        is KeyAction.ToggleNumericMode -> onToggleNumericMode(action.enable)
        is KeyAction.ToggleEmojiMode -> onToggleEmojiMode(action.enable)
        is KeyAction.ToggleCapsLock -> onToggleCapsLock()
        is KeyAction.SwitchLanguage -> onSwitchLanguage()
        is KeyAction.ChangePosition -> onChangePosition(action.transform)
    }
}

fun doneKeyAction(
    scope: CoroutineScope,
    action: KeyAction,
    isDragged: MutableState<Boolean>,
    releasedKey: MutableState<String?>,
    animationHelperSpeed: Int,
) {
    when (action) {
        is KeyAction.CommitText -> {
            isDragged.value = false
            releasedKey.value = action.text
            scope.launch {
                delay(animationHelperSpeed.toLong())
                releasedKey.value = null
            }
        }
        else -> {
            isDragged.value = false
            releasedKey.value = null
        }
    }
}
