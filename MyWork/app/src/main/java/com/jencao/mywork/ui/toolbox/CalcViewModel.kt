package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.CalcHistoryEntity
import com.jencao.mywork.data.repository.CalcHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalcViewModel @Inject constructor(private val repo: CalcHistoryRepository) : ViewModel() {
    val history: StateFlow<List<CalcHistoryEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expr = MutableStateFlow("")
    val expr = _expr.asStateFlow()

    private val _result = MutableStateFlow("")
    val result = _result.asStateFlow()

    fun input(s: String) {
        _expr.value += s
    }

    fun backspace() {
        if (_expr.value.isNotEmpty()) _expr.value = _expr.value.dropLast(1)
    }

    fun clear() {
        _expr.value = ""
        _result.value = ""
    }

    /** 计算并保存历史；返回是否成功 */
    fun compute(): Boolean {
        val r = eval(_expr.value) ?: return false
        _result.value = r
        val e = _expr.value
        viewModelScope.launch { repo.insert(CalcHistoryEntity(expr = e, result = r)) }
        return true
    }

    fun deleteHistory(id: String) = viewModelScope.launch { repo.softDelete(id) }

    fun clearHistory() = viewModelScope.launch { history.value.forEach { repo.softDelete(it.id) } }

    /** 安全的中缀表达式求值：支持 + - * / % ^ 与括号，不允许其他字符 */
    private fun eval(input: String): String? {
        if (input.isBlank()) return null
        if (!input.all { it.isDigit() || "+-*/%^(). ".contains(it) }) return null
        return try {
            val v = Parser(input.replace(" ", "")).parse()
            if (v.isFinite()) String.format(Locale.US, "%.6g", v) else null
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(private val s: String) {
        private var pos = 0
        fun parse(): Double = expr().also { if (pos != s.length) throw RuntimeException("bad") }
        private fun expr(): Double {
            var v = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]; val r = term()
                v = if (op == '+') v + r else v - r
            }
            return v
        }
        private fun term(): Double {
            var v = factor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/' || s[pos] == '%')) {
                val op = s[pos++]; val r = factor()
            v = when (op) {
                '*' -> v * r
                '/' -> if (r == 0.0) throw RuntimeException("div0") else v / r
                else -> v % r
            }
            }
            return v
        }
        private fun factor(): Double {
            if (pos < s.length && s[pos] == '+') { pos++; return factor() }
            if (pos < s.length && s[pos] == '-') { pos++; return -factor() }
            if (pos < s.length && s[pos] == '(') {
                pos++; val v = expr(); if (pos >= s.length || s[pos] != ')') throw RuntimeException(")"); pos++; return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) throw RuntimeException("num")
            val num = s.substring(start, pos).toDouble()
            if (pos < s.length && s[pos] == '^') { pos++; return Math.pow(num, factor()) }
            return num
        }
    }
}
