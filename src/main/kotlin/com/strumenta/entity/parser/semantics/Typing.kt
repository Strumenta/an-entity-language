package com.strumenta.entity.parser.semantics

import com.strumenta.kolasu.model.Node
import java.util.IdentityHashMap

interface Type

private object Typing {
    private val memory = IdentityHashMap<Node, Type>()

    fun setType(
        node: Node,
        type: Type?,
    ) {
        if (type == null) {
            memory.remove(node)
        } else {
            memory[node] = type
        }
    }

    fun getType(node: Node): Type? {
        return memory[node]
    }
}

object UnitType : Type, Node()

var Node.type: Type?
    get() = Typing.getType(this)
    set(value) {
        Typing.setType(this, value)
    }
