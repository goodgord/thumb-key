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
    private val textServicesManager =
        context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
    private val spellCheckerSession =
        textServicesManager.newSpellCheckerSession(
            null,
            Locale.getDefault(),
            this,
            true
        ).also {
            Log.d("ThumbKey", "SpellCheckerSession created: ${it != null}")
        }

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    private var currentWord = StringBuilder()

    fun onTextInput(text: String) {
        currentWord.append(text)
        Log.d("ThumbKey", "Current word: ${currentWord.toString()}")
        requestSuggestions()
    }

    fun onBackspace() {
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
            Log.d("ThumbKey", "Current word after backspace: ${currentWord.toString()}")
            requestSuggestions()
        }
    }

    fun onWordComplete() {
        currentWord.clear()
        _suggestions.value = emptyList()
        Log.d("ThumbKey", "Word completed, suggestions cleared")
    }

    private fun requestSuggestions() {
        val word = currentWord.toString()
        if (word.isNotEmpty()) {
            Log.d("ThumbKey", "Requesting suggestions for: $word")
            spellCheckerSession?.getSentenceSuggestions(
                arrayOf(TextInfo(word)),
                3, // Suggestion limit
            )
        } else {
            _suggestions.value = emptyList()
            Log.d("ThumbKey", "Empty word, suggestions cleared")
        }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        val suggestions = mutableListOf<String>()
        results?.forEach { result ->
            for (i in 0 until result.suggestionsCount) {
                result.getSuggestionAt(i)?.let { suggestion ->
                    suggestions.add(suggestion)
                }
            }
        }
        Log.d("ThumbKey", "Got suggestions: $suggestions")
        _suggestions.value = suggestions
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        val suggestions = mutableListOf<String>()
        results?.forEach { result ->
            for (i in 0 until result.suggestionsCount) {
                val suggestionsInfo = result.getSuggestionsInfoAt(i)
                for (j in 0 until suggestionsInfo.suggestionsCount) {
                    suggestionsInfo.getSuggestionAt(j)?.let { suggestion ->
                        suggestions.add(suggestion)
                    }
                }
            }
        }
        Log.d("ThumbKey", "Got sentence suggestions: $suggestions")
        _suggestions.value = suggestions
    }
}
