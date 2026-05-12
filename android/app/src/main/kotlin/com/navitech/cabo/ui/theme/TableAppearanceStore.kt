package com.navitech.cabo.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TableAppearanceStore {
    private const val PREFS = "cabogame_appearance"
    private const val KEY_BOARD = "cabogame.boardTheme"
    private const val KEY_DECK = "cabogame.cardDeckStyle"

    private lateinit var app: Context

    private val _boardTheme = MutableStateFlow(BoardTheme.FOREST)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    private val _cardDeck = MutableStateFlow(CardDeckStyle.CLASSIC)
    val cardDeck: StateFlow<CardDeckStyle> = _cardDeck.asStateFlow()

    fun load(context: Context) {
        app = context.applicationContext
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rawBoard = p.getString(KEY_BOARD, null)
        _boardTheme.value = BoardTheme.fromRaw(rawBoard)
        if (rawBoard == "midnight") {
            p.edit().putString(KEY_BOARD, BoardTheme.AMETHYST.raw).apply()
        }
        val rawDeck = p.getString(KEY_DECK, null)
        _cardDeck.value = CardDeckStyle.fromRaw(rawDeck)
        if (rawDeck == "amethyst") {
            p.edit().putString(KEY_DECK, CardDeckStyle.VELVET.raw).apply()
        }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BOARD, theme.raw)
            .apply()
    }

    fun setCardDeck(style: CardDeckStyle) {
        _cardDeck.value = style
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DECK, style.raw)
            .apply()
    }
}
