package ru.senin.kotlin.wiki.parser

import ru.senin.kotlin.wiki.data.Page

interface Parser {
    fun parse()
    fun onPage(callback: (Page) -> Unit)
}