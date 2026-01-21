package com.example.xtreamplayer.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.xtreamplayer.R

enum class AppFont(val label: String, val fontFamily: FontFamily) {
    DEFAULT("Default", FontFamily.Serif),
    ROBOTO("Roboto", FontFamily(Font(R.font.roboto))),
    LATO("Lato", FontFamily(Font(R.font.lato))),
    MONTSERRAT("Montserrat", FontFamily(Font(R.font.montserrat))),
    OPEN_SANS("Open Sans", FontFamily(Font(R.font.opensans))),
    PRODUCT_SANS("Product Sans", FontFamily(Font(R.font.productsans))),
    INTER_24("Inter", FontFamily(Font(R.font.inter24))),
}
