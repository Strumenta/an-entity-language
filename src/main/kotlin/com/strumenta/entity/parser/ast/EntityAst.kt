package com.strumenta.entity.parser.ast

import com.strumenta.kolasu.model.*

// workspace

data class Workspace(var modules: MutableList<Module> = mutableListOf()) : Node()

// modules

data class Module(
    override val name: String,
    var imports: MutableList<Import> = mutableListOf(),
    var entities: MutableList<Entity> = mutableListOf()
) : Node(), Named

// imports

data class Import(
    var module: ReferenceByName<Module>
) : Node()

// entities

data class Entity(
    override val name: String,
    var features: MutableList<Feature> = mutableListOf(),
    var operations: MutableList<Operation> = mutableListOf(),
) : Node(), Named

// value

sealed class Symbol(
    override val name: String,
    open var type: ReferenceByName<Entity>
) : Node(), Named

// features

data class Feature(
    override val name: String,
    override var type: ReferenceByName<Entity>
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
    override var type: ReferenceByName<Entity>
) : Symbol(name, type)

// statements

sealed class Statement : Node()

data class BindingStatement(
    var variable: Variable,
    var value: Expression,
) : Statement()

data class Variable(
    override val name: String,
    override var type: ReferenceByName<Entity>
) : Symbol(name, type)

data class ReturnStatement(
    var value: Expression,
) : Statement()

// expressions

sealed class Expression : Node()

data class OperatorExpression(
    var left: Expression,
    var right: Expression,
    var operator: Operator
) : Expression()

enum class Operator {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION
}

data class InvocationExpression(
    var operation: OperationReference,
    var arguments: MutableList<Expression> = mutableListOf()
) : Expression()

data class OperationReference(
    var context: Expression? = null,
    var operation: ReferenceByName<Operation>
) : Node()

data class ReferenceExpression(
    var context: Expression? = null,
    var target: ReferenceByName<Symbol>
) : Expression()

data class ConstructorExpression(
    var entity: ReferenceByName<Entity>,
    var arguments: MutableList<Expression> = mutableListOf(),
) : Expression()

data class LiteralExpression(
    var value: String,
    var type: ReferenceByName<Entity>
) : Expression()
