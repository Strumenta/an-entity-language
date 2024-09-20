package com.strumenta.entity.parser.ast

import com.strumenta.entity.parser.semantics.Type
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName

// workspace

// data class Workspace(var modules: MutableList<Module> = mutableListOf()) : Node()

// modules

data class Module(
    override val name: String,
    var imports: MutableList<Import> = mutableListOf(),
    var entities: MutableList<Entity> = mutableListOf(),
) : Node(), Named

// imports

data class Import(
    var module: ReferenceByName<Module>,
) : Node()

// entities

data class Entity(
    override val name: String,
    var features: MutableList<Feature> = mutableListOf(),
    var operations: MutableList<Operation> = mutableListOf(),
) : Node(), Named, Type

// value

sealed class Symbol(
    override val name: String,
    open var type: ReferenceByName<Entity>,
) : Node(), Named

// features

data class Feature(
    override val name: String,
    override var type: ReferenceByName<Entity>,
) : Symbol(name, type)

// operations

data class Operation(
    override val name: String,
    var type: ReferenceByName<Entity>?,
    var parameters: MutableList<Parameter> = mutableListOf(),
    var statements: MutableList<Statement> = mutableListOf(),
) : Node(), Named

// parameters

data class Parameter(
    override val name: String,
    override var type: ReferenceByName<Entity>,
) : Symbol(name, type)

// statements

sealed class Statement : Node()

data class BindingStatement(
    var variable: Variable,
    var value: Expression,
) : Statement()

data class Variable(
    override val name: String,
    override var type: ReferenceByName<Entity>,
) : Symbol(name, type)

data class ReturnStatement(
    var value: Expression,
) : Statement()

// expressions

sealed class Expression : Node(), com.strumenta.kolasu.model.Expression

data class OperatorExpression(
    var left: Expression,
    var right: Expression,
    var operator: Operator,
) : Expression()

enum class Operator {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION,
}

data class InvocationExpression(
    var context: Expression,
    var operation: ReferenceByName<Operation>,
    var arguments: MutableList<Expression> = mutableListOf(),
) : Expression()

data class ReferenceExpression(
    var context: Expression? = null,
    var target: ReferenceByName<Symbol>,
) : Expression()

data class ConstructorExpression(
    var entity: ReferenceByName<Entity>,
    var arguments: MutableList<Expression> = mutableListOf(),
) : Expression()

sealed class LiteralExpression : Expression()

data class StringLiteralExpression(var value: String) : LiteralExpression()

data class BooleanLiteralExpression(var value: Boolean) : LiteralExpression()

data class IntegerLiteralExpression(var value: Long) : LiteralExpression()
