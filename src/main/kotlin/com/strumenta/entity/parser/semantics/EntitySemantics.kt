package com.strumenta.entity.parser.semantics

import com.strumenta.entity.parser.ast.ConstructorExpression
import com.strumenta.entity.parser.ast.Entity
import com.strumenta.entity.parser.ast.Feature
import com.strumenta.entity.parser.ast.Import
import com.strumenta.entity.parser.ast.LiteralExpression
import com.strumenta.entity.parser.ast.Module
import com.strumenta.entity.parser.ast.Operation
import com.strumenta.entity.parser.ast.OperationReference
import com.strumenta.entity.parser.ast.OperatorExpression
import com.strumenta.entity.parser.ast.ReferenceExpression
import com.strumenta.entity.parser.ast.Statement
import com.strumenta.entity.parser.ast.Variable
import com.strumenta.entity.parser.runtime.builtinTypes
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.previousSamePropertySibling
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import com.strumenta.kolasu.semantics.scope.description.ScopeDescriptionApi
import com.strumenta.kolasu.semantics.scope.provider.declarative.DeclarativeScopeProvider
import com.strumenta.kolasu.semantics.scope.provider.declarative.scopeFor
import com.strumenta.kolasu.semantics.semantics
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver
import com.strumenta.kolasu.traversing.findAncestorOfType
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity

interface ModuleFinder {
    fun findModule(moduleName: String): Module?
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

val scopeProvider =
    DeclarativeScopeProvider(
//    scopeFor(Todo::prerequisite) {
//        (it.node.parent as TodoProject).todos.forEach {
//            define(it)
//        }
//    }
    )

fun symbolResolver(moduleFinder: ModuleFinder): SymbolResolver {
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
//        scopeFor(Symbol::type) {
//            // all entities from current, imported and standard module
//            symbolResolver.scopeFrom(it.findAncestorOfType(Module::class.java))
//        },
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
            parent(entityLevelValues(it.node))
        },
    )
    dsp.addRule(
        scopeFor(LiteralExpression::type) {
        },
    )
    dsp.addRule(
        scopeFor(OperationReference::operation) {
            parent(entityLevelOperations(it.node))
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

fun Module.semanticEnrichment(moduleFinder: ModuleFinder): List<Issue> {
    val issues = mutableListOf<Issue>()
    symbolResolver(moduleFinder).resolve(this, entireTree = true)
    return issues
}

fun ScopeDescriptionApi.define(values: List<PossiblyNamed>?) {
    values?.forEach { define(it) }
}
