package com.dessalines.thumbkey

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.textservice.TextServicesManager
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
        
        // Initialize prediction manager
        val tsm = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        Log.d(TAG, "Spell checker enabled (onCreate): ${tsm.isSpellCheckerEnabled}")
        if (tsm.isSpellCheckerEnabled) {
            predictionManager = PredictionManager(this)
            Log.d(TAG, "PredictionManager initialized in onCreate")
        } else {
            Log.e(TAG, "Spell checker is not enabled, predictions won't work")
        }
    }

    private fun setupView(): View {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository

        val view = ComposeKeyboardView(
            this, 
            settingsRepo, 
            if (::predictionManager.isInitialized) predictionManager else {
                Log.d(TAG, "Creating new PredictionManager in setupView")
                PredictionManager(this)
            }
        )
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
        Log.d(TAG, "onStartInput called, restarting: $restarting")
        
        // Re-initialize prediction manager if needed
        if (!::predictionManager.isInitialized) {
            val tsm = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
            Log.d(TAG, "Spell checker enabled (onStartInput): ${tsm.isSpellCheckerEnabled}")
            if (tsm.isSpellCheckerEnabled) {
                predictionManager = PredictionManager(this)
                Log.d(TAG, "PredictionManager initialized in onStartInput")
            } else {
                Log.e(TAG, "Spell checker is not enabled, predictions won't work")
            }
        }
        
        val view = this.setupView()
        this.setInputView(view)
    }

    fun commitText(
        text: String,
        newCursorPosition: Int,
    ) {
        Log.d(TAG, "commitText called with text: $text")
        currentInputConnection?.commitText(text, newCursorPosition)

        if (!::predictionManager.isInitialized) {
            Log.e(TAG, "Cannot handle predictions - PredictionManager not initialized")
            return
        }

        // Handle word boundaries for prediction
        if (text == " " || text == "\n") {
            Log.d(TAG, "Word boundary detected, calling onWordComplete")
            predictionManager.onWordComplete()
        } else {
            Log.d(TAG, "Calling onTextInput with: $text")
            predictionManager.onTextInput(text)
        }
    }

    fun handleBackspace() {
        Log.d(TAG, "handleBackspace called")
        currentInputConnection?.deleteSurroundingText(1, 0)
        
        if (!::predictionManager.isInitialized) {
            Log.e(TAG, "Cannot handle predictions - PredictionManager not initialized")
            return
        }
        
        predictionManager.onBackspace()
    }

    fun commitSuggestion(suggestion: String) {
        Log.d(TAG, "commitSuggestion called with: $suggestion")
        if (!::predictionManager.isInitialized) {
            Log.e(TAG, "Cannot handle predictions - PredictionManager not initialized")
            return
        }

        // Get the current word
        val currentWord = getCurrentWord()
        Log.d(TAG, "Current word before suggestion: $currentWord")

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
        Log.d(TAG, "getCurrentWord returning: $beforeCursor")
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
