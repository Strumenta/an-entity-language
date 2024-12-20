package com.strumenta.entity.parser.semantics

import com.strumenta.entity.parser.ast.BooleanLiteralExpression
import com.strumenta.entity.parser.ast.ConstructorExpression
import com.strumenta.entity.parser.ast.Entity
import com.strumenta.entity.parser.ast.Expression
import com.strumenta.entity.parser.ast.Feature
import com.strumenta.entity.parser.ast.Import
import com.strumenta.entity.parser.ast.IntegerLiteralExpression
import com.strumenta.entity.parser.ast.InvocationExpression
import com.strumenta.entity.parser.ast.Module
import com.strumenta.entity.parser.ast.Operation
import com.strumenta.entity.parser.ast.Operator
import com.strumenta.entity.parser.ast.OperatorExpression
import com.strumenta.entity.parser.ast.ReferenceExpression
import com.strumenta.entity.parser.ast.Statement
import com.strumenta.entity.parser.ast.StringLiteralExpression
import com.strumenta.entity.parser.ast.Variable
import com.strumenta.entity.parser.runtime.BooleanType
import com.strumenta.entity.parser.runtime.IntegerType
import com.strumenta.entity.parser.runtime.StringType
import com.strumenta.entity.parser.runtime.builtinTypes
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.previousSamePropertySibling
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import com.strumenta.kolasu.semantics.scope.description.ScopeDescriptionApi
import com.strumenta.kolasu.semantics.scope.provider.declarative.DeclarativeScopeProvider
import com.strumenta.kolasu.semantics.scope.provider.declarative.scopeFor
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver
import com.strumenta.kolasu.traversing.findAncestorOfType
import com.strumenta.kolasu.traversing.walkDescendants
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import kotlin.reflect.KProperty1

interface ModuleFinder {
    fun findModule(moduleName: String): Module?
}

interface TypeCalculator {
    fun getType(node: Node): Type? {
        return setTypeIfNeeded(node)
    }

    fun strictlyGetType(node: Node): Type {
        return setTypeIfNeeded(node) ?: throw IllegalStateException("Cannot get type for node $node")
    }

    fun calculateType(node: Node): Type?

    fun setTypeIfNeeded(node: Node): Type? {
        if (node.type == null) {
            val calculatedType = calculateType(node)
            node.type = calculatedType
        }
        return node.type
    }
}

class SimpleModuleFinder : ModuleFinder {
    private val modules = mutableMapOf<String, Module>()

    fun registerModule(module: Module) {
        modules[module.name] = module
    }

    override fun findModule(moduleName: String): Module? {
        return modules[moduleName]
    }
}

fun symbolResolver(
    moduleFinder: ModuleFinder,
    typeCalculator: TypeCalculator,
): SymbolResolver {
    val dsp = DeclarativeScopeProvider()
    val sr = SymbolResolver(dsp)

    fun moduleLevelTypes(ctx: Node): ScopeDescription =
        ScopeDescription().apply {
            val module = ctx.findAncestorOfType(Module::class.java)
            module?.let { m ->
                define(m.entities)
                m.imports.forEach { import ->
                    sr.resolve(import)
                    import.module.referred?.entities?.forEach {
                        define(it)
                    }
                }
            }
            builtinTypes.forEach(this::define)
        }

    fun entityLevelValues(ctx: Node): ScopeDescription =
        ScopeDescription().apply {
            define(ctx.findAncestorOfType(Entity::class.java)?.features)
        }

    fun entityLevelOperations(ctx: Node): ScopeDescription =
        ScopeDescription().apply {
            define(ctx.findAncestorOfType(Entity::class.java)?.operations)
        }

    dsp.addRule(
        scopeFor(Import::module) {
            moduleFinder.findModule(it.node.module.name)?.let { importedModule ->
                define(importedModule)
            }
        },
    )
    dsp.addRule(
        scopeFor(Operation::type) {
            parent(moduleLevelTypes(it.node))
        },
    )
    dsp.addRule(
        scopeFor(Feature::type) {
            parent(moduleLevelTypes(it.node))
        },
    )
    dsp.addRule(
        scopeFor(ReferenceExpression::target) {
            if (it.node.context == null) {
                parent(entityLevelValues(it.node))
            } else {
                TODO()
            }
        },
    )
    dsp.addRule(
        scopeFor(InvocationExpression::operation) {
            sr.resolve(it.node.context)
            val type =
                typeCalculator.getType(it.node.context)
                    ?: throw IllegalStateException(
                        "Cannot resolve operation as the type of the context is unknown. " +
                            "Context: ${it.node.context}",
                    )
            val entity = type as? Entity ?: throw IllegalStateException("We cannot only invoke operations on entities")
            define(entity.operations)
        },
    )
    dsp.addRule(
        scopeFor(Variable::type) {
        },
    )
    dsp.addRule(
        scopeFor(ConstructorExpression::entity) {
            parent(moduleLevelTypes(it.node))
        },
    )
    return sr
}

internal fun Statement.previousStatements() =
    sequence {
        var precedingStatement = this@previousStatements.previousSamePropertySibling
        while (precedingStatement != null) {
            yield(precedingStatement)
            precedingStatement = precedingStatement.previousSamePropertySibling
        }
    }

internal fun MutableList<Issue>.addIncompatibleTypesError(operatorExpression: OperatorExpression) {
    this.add(
        Issue.semantic(
            message = "Incompatible types in operator expression: $operatorExpression",
            severity = IssueSeverity.ERROR,
            position = operatorExpression.position,
        ),
    )
}

class EntityTypeCalculator : TypeCalculator {
    var sr: SymbolResolver? = null

    private fun <T : Node, S : PossiblyNamed> getTypeOfReference(
        refHolder: T,
        ref: KProperty1<T, ReferenceByName<S>?>,
    ): Type? {
        val refValue = ref.getValue(refHolder, ref) ?: return null
        if (!refValue.resolved) {
            sr?.resolve(refHolder, ref as KProperty1<Node, ReferenceByName<PossiblyNamed>?>)
        }
        return if (refValue.resolved) {
            getType(refValue.referred!! as Node)
        } else {
            null
        }
    }

    override fun calculateType(node: Node): Type? {
        return when (node) {
            is OperatorExpression -> {
                val leftType = getType(node.left)
                val rightType = getType(node.right)
                if (leftType == null || rightType == null) {
                    return null
                }
                when (node.operator) {
                    Operator.ADDITION -> {
                        when (val operandTypes = Pair(leftType, rightType)) {
                            Pair(StringType, StringType) -> StringType
                            Pair(StringType, IntegerType) -> StringType
                            else -> TODO("$operandTypes for expression $node")
                        }
                    }
                    Operator.MULTIPLICATION -> {
                        when (val operandTypes = Pair(leftType, rightType)) {
                            Pair(IntegerType, IntegerType) -> IntegerType
                            else -> TODO("$operandTypes for expression $node")
                        }
                    }
                    else -> TODO(node.operator.toString())
                }
            }
            is ReferenceExpression -> {
                getTypeOfReference(node, ReferenceExpression::target)
            }
            is Feature -> {
                getTypeOfReference(node, Feature::type)
            }
            is Entity -> {
                node
            }
            is StringLiteralExpression -> {
                StringType
            }
            is BooleanLiteralExpression -> {
                BooleanType
            }
            is IntegerLiteralExpression -> {
                IntegerType
            }
            is InvocationExpression -> {
                getTypeOfReference(node, InvocationExpression::operation)
            }
            is Operation -> {
                if (node.type == null) {
                    UnitType
                } else {
                    getTypeOfReference(node, Operation::type)
                }
            }
            is ConstructorExpression -> {
                getTypeOfReference(node, ConstructorExpression::entity)
            }
            else -> TODO("Does not know how to calculate the type of $node")
        }
    }
}

fun Module.semanticEnrichment(moduleFinder: ModuleFinder): List<Issue> {
    val issues = mutableListOf<Issue>()
    val typeCalculator = EntityTypeCalculator()
    val sr = symbolResolver(moduleFinder, typeCalculator)
    typeCalculator.sr = sr
    sr.resolve(this, entireTree = true)
    this.walkDescendants(Expression::class).forEach { expression ->
        typeCalculator.setTypeIfNeeded(expression)
    }
    return issues
}

fun ScopeDescriptionApi.define(values: List<PossiblyNamed>?) {
    values?.forEach { define(it) }
}
