package com.strumenta.entity.parser.semantics

import com.strumenta.kolasu.model.Expression
import java.util.IdentityHashMap

interface Type

private object Typing {
    private val memory = IdentityHashMap<Expression, Type>()

    fun setType(
        expression: Expression,
        type: Type?,
    ) {
        if (type == null) {
            memory.remove(expression)
        } else {
            memory[expression] = type
        }
    }

    fun getType(expression: Expression): Type? {
        return memory[expression]
    }
}

var Expression.type: Type?
    get() = Typing.getType(this)
    set(value) {
        Typing.setType(this, value)
    }
