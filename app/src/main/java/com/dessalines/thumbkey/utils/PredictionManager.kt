package com.dessalines.thumbkey.utils

import android.content.Context
import android.util.Log
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class PredictionManager(
    context: Context,
) : SpellCheckerSession.SpellCheckerSessionListener {
    private var textServicesManager: TextServicesManager? = null
    private var spellCheckerSession: SpellCheckerSession? = null
    private var initialized = false

    init {
        Log.d(TAG, "Initializing PredictionManager")
        try {
            textServicesManager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
            Log.d(TAG, "TextServicesManager obtained: ${textServicesManager != null}")
            
            if (textServicesManager?.isSpellCheckerEnabled == true) {
                Log.d(TAG, "Spell checker is enabled, creating session")
                try {
                    spellCheckerSession = textServicesManager?.newSpellCheckerSession(
                        null,
                        Locale.getDefault(),
                        this,
                        true
                    )
                    Log.d(TAG, "SpellCheckerSession created: ${spellCheckerSession != null}")
                    initialized = spellCheckerSession != null
                    Log.d(TAG, "Initialization complete, success: $initialized")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create SpellCheckerSession", e)
                }
            } else {
                Log.e(TAG, "Spell checker is not enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextServicesManager", e)
        }
    }

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    private var currentWord = StringBuilder()

    fun onTextInput(text: String) {
        if (!initialized) {
            Log.e(TAG, "onTextInput called but PredictionManager not initialized")
            return
        }
        currentWord.append(text)
        Log.d(TAG, "onTextInput: Current word is now: ${currentWord.toString()}")
        requestSuggestions()
    }

    fun onBackspace() {
        if (!initialized) {
            Log.e(TAG, "onBackspace called but PredictionManager not initialized")
            return
        }
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
            Log.d(TAG, "onBackspace: Current word is now: ${currentWord.toString()}")
            requestSuggestions()
        }
    }

    fun onWordComplete() {
        if (!initialized) {
            Log.e(TAG, "onWordComplete called but PredictionManager not initialized")
            return
        }
        currentWord.clear()
        _suggestions.value = emptyList()
        Log.d(TAG, "onWordComplete: Word completed, suggestions cleared")
    }

    private fun requestSuggestions() {
        if (!initialized) {
            Log.e(TAG, "requestSuggestions called but PredictionManager not initialized")
            return
        }
        val word = currentWord.toString()
        if (word.isNotEmpty()) {
            Log.d(TAG, "requestSuggestions: Requesting suggestions for word: $word")
            try {
                val textInfo = TextInfo(word)
                spellCheckerSession?.getSuggestions(
                    arrayOf(textInfo),
                    5,
                    true
                )
                Log.d(TAG, "requestSuggestions: Successfully requested suggestions")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request suggestions", e)
                e.printStackTrace()
            }
        } else {
            _suggestions.value = emptyList()
            Log.d(TAG, "requestSuggestions: Empty word, suggestions cleared")
        }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        Log.d(TAG, "onGetSuggestions callback received with results: ${results?.size}")
        if (results == null) {
            Log.d(TAG, "onGetSuggestions: Results array is null")
            return
        }
        
        val suggestions = mutableListOf<String>()
        results.forEach { result ->
            Log.d(TAG, "Processing result with suggestions count: ${result.suggestionsCount}")
            for (i in 0 until result.suggestionsCount) {
                result.getSuggestionAt(i)?.let { suggestion ->
                    Log.d(TAG, "Found suggestion: $suggestion")
                    suggestions.add(suggestion)
                }
            }
        }
        Log.d(TAG, "Final suggestions list: $suggestions")
        if (suggestions.isNotEmpty()) {
            _suggestions.value = suggestions
        }
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        Log.d(TAG, "onGetSentenceSuggestions callback received with results: ${results?.size}")
        if (results == null) {
            Log.d(TAG, "onGetSentenceSuggestions: Results array is null")
            return
        }
        
        val suggestions = mutableListOf<String>()
        results.forEach { result ->
            Log.d(TAG, "Processing sentence result with suggestions count: ${result.suggestionsCount}")
            for (i in 0 until result.suggestionsCount) {
                val suggestionsInfo = result.getSuggestionsInfoAt(i)
                for (j in 0 until suggestionsInfo.suggestionsCount) {
                    suggestionsInfo.getSuggestionAt(j)?.let { suggestion ->
                        Log.d(TAG, "Found sentence suggestion: $suggestion")
                        suggestions.add(suggestion)
                    }
                }
            }
        }
        Log.d(TAG, "Final sentence suggestions list: $suggestions")
        if (suggestions.isNotEmpty()) {
            _suggestions.value = suggestions
        }
    }
}
