package com.strumenta.entity.parser.runtime

import com.strumenta.entity.parser.ast.Entity
import com.strumenta.entity.parser.ast.Module

val StringType = Entity(name = "String")
val IntegerType = Entity("Integer")
val BooleanType = Entity("Boolean")

val StandardModule =
    Module(
        name = "Standard",
        entities =
            mutableListOf(
                StringType,
                IntegerType,
                BooleanType,
            ),
    )

// typeFor(StringLiteral::class) { StringType }
