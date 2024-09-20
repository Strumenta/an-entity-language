package com.strumenta.entity.parser.runtime

import com.strumenta.entity.parser.ast.Entity
import com.strumenta.entity.parser.ast.Module

val StringType = Entity(name = "String")
val IntegerType = Entity("Integer")
val BooleanType = Entity("Boolean")

val builtinTypes = listOf(StringType, IntegerType, BooleanType)
