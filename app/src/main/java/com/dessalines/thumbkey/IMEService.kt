package com.dessalines.thumbkey

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dessalines.thumbkey.utils.PredictionManager
import com.dessalines.thumbkey.utils.TAG

class IMEService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    lateinit var predictionManager: PredictionManager

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        predictionManager = PredictionManager(this)
    }

    private fun setupView(): View {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository

        val view = ComposeKeyboardView(this, settingsRepo, predictionManager)
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        view.let {
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }
        return view
    }

    override fun onStartInput(
        attribute: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInput(attribute, restarting)
        val view = this.setupView()
        this.setInputView(view)
    }

    fun commitText(
        text: String,
        newCursorPosition: Int,
    ) {
        currentInputConnection?.commitText(text, newCursorPosition)

        // Handle word boundaries for prediction
        if (text == " " || text == "\n") {
            predictionManager.onWordComplete()
        } else {
            predictionManager.onTextInput(text)
        }
    }

    fun handleBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        predictionManager.onBackspace()
    }

    fun commitSuggestion(suggestion: String) {
        // Get the current word
        val currentWord = getCurrentWord()

        // Delete the current word
        for (i in currentWord.indices) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        // Insert the suggestion
        currentInputConnection?.commitText(suggestion, 1)
        predictionManager.onWordComplete()
    }

    fun getCurrentWord(): String {
        val ic = currentInputConnection ?: return ""
        // Get text before cursor
        var beforeCursor = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
        // Find the last word boundary
        val lastSpace = beforeCursor.lastIndexOf(' ')
        if (lastSpace != -1) {
            beforeCursor = beforeCursor.substring(lastSpace + 1)
        }
        return beforeCursor
    }

    // Lifecycle Methods
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    private fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)

    override val lifecycle = lifecycleRegistry

    override fun onDestroy() {
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    // Cursor update Methods
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)

        cursorMoved =
            if (ignoreCursorMove) {
                ignoreCursorMove = false
                false
            } else {
                Log.d(TAG, "cursor moved")
                cursorAnchorInfo.selectionStart != selectionStart ||
                    cursorAnchorInfo.selectionEnd != selectionEnd
            }

        selectionStart = cursorAnchorInfo.selectionStart
        selectionEnd = cursorAnchorInfo.selectionEnd
    }

    fun didCursorMove(): Boolean = cursorMoved

    fun ignoreNextCursorMove() {
        // This gets reset on the next call to `onUpdateCursorAnchorInfo`
        ignoreCursorMove = true
    }

    private var ignoreCursorMove: Boolean = false
    private var cursorMoved: Boolean = false
    private var selectionStart: Int = 0
    private var selectionEnd: Int = 0

    // ViewModelStore Methods
    override val viewModelStore = ViewModelStore()

    // SaveStateRegistry Methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
}
