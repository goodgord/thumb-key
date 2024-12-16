package com.dessalines.thumbkey.utils

import android.content.Context
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PredictionManager(
    context: Context,
) : SpellCheckerSession.SpellCheckerSessionListener {
    private val textServicesManager =
        context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
    private val spellCheckerSession =
        textServicesManager.newSpellCheckerSession(null, null, this, true)

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    private var currentWord = StringBuilder()

    fun onTextInput(text: String) {
        currentWord.append(text)
        requestSuggestions()
    }

    fun onBackspace() {
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
            requestSuggestions()
        }
    }

    fun onWordComplete() {
        currentWord.clear()
        _suggestions.value = emptyList()
    }

    private fun requestSuggestions() {
        val word = currentWord.toString()
        if (word.isNotEmpty()) {
            spellCheckerSession?.getSentenceSuggestions(
                arrayOf(TextInfo(word)),
                3, // Suggestion limit
            )
        } else {
            _suggestions.value = emptyList()
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
        _suggestions.value = suggestions
    }

    override fun onGetSentenceSuggestions(results: Array<out SuggestionsInfo>?) {
        onGetSuggestions(results)
    }
}
