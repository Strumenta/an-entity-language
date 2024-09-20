package com.strumenta.entity.parser.semantics

import com.strumenta.entity.parser.ast.BindingStatement
import com.strumenta.entity.parser.ast.ConstructorExpression
import com.strumenta.entity.parser.ast.Entity
import com.strumenta.entity.parser.ast.Import
import com.strumenta.entity.parser.ast.InvocationExpression
import com.strumenta.entity.parser.ast.LiteralExpression
import com.strumenta.entity.parser.ast.Module
import com.strumenta.entity.parser.ast.Operation
import com.strumenta.entity.parser.ast.OperationReference
import com.strumenta.entity.parser.ast.Operator
import com.strumenta.entity.parser.ast.OperatorExpression
import com.strumenta.entity.parser.ast.ReferenceExpression
import com.strumenta.entity.parser.ast.Statement
import com.strumenta.entity.parser.ast.Symbol
import com.strumenta.entity.parser.ast.Workspace
import com.strumenta.entity.parser.runtime.IntegerType
import com.strumenta.entity.parser.runtime.StandardModule
import com.strumenta.entity.parser.runtime.StringType
import com.strumenta.kolasu.model.previousSamePropertySibling
import com.strumenta.kolasu.semantics.scope
import com.strumenta.kolasu.semantics.semantics
import com.strumenta.kolasu.traversing.findAncestorOfType
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity

fun entitySemantics(issues: MutableList<Issue> = mutableListOf()) =
    semantics(issues) {
        // symbol resolution
        symbolResolver {
            // resolution rules
            scopeFor(Import::module) {
                scope {
                    // all visible modules in workspace
                    it.findAncestorOfType(Module::class.java)
                        ?.visibleModules()
                        ?.forEach(this::define)
                }
            }
            scopeFor(Symbol::type) {
                // all entities from current, imported and standard module
                symbolResolver.scopeFrom(it.findAncestorOfType(Module::class.java))
            }
            scopeFor(Operation::type) {
                // all entities from current, imported and standard modules
                symbolResolver.scopeFrom(it.findAncestorOfType(Module::class.java))
            }
            scopeFor(OperationReference::operation) {
                if (it.context != null) {
                    // all operations from the context type, i.e. entity
                    symbolResolver.scopeFrom(typeComputer.typeFor(it.context))
                } else {
                    scope {
                        // all operations from the containing entity
                        it.findAncestorOfType(Entity::class.java)?.operations?.forEach(this::define)
                    }
                }
            }
            scopeFor(ReferenceExpression::target) {
                if (it.context != null) {
                    // all features from the context type, i.e. entity
                    symbolResolver.scopeFrom(typeComputer.typeFor(it.context))
                } else {
                    scope {
                        // all previously declared variables from the container operation
                        it.findAncestorOfType(Statement::class.java)
                            ?.previousStatements()
                            ?.filterIsInstance<BindingStatement>()
                            ?.map(BindingStatement::variable)
                            ?.forEach(this::define)
                        parent =
                            scope {
                                // all parameters from the container operation
                                it.findAncestorOfType(Operation::class.java)
                                    ?.parameters
                                    ?.forEach(this::define)
                                parent =
                                    scope {
                                        // all features from the container entity
                                        it.findAncestorOfType(Entity::class.java)
                                            ?.features
                                            ?.forEach(this::define)
                                    }
                            }
                    }
                }
            }
            scopeFor(ConstructorExpression::entity) {
                // all entities from current, imported and standard modules
                symbolResolver.scopeFrom(it.findAncestorOfType(Module::class.java))
            }
            scopeFor(LiteralExpression::type) {
                // all entities from current, imported and standard modules
                symbolResolver.scopeFrom(it.findAncestorOfType(Module::class.java))
            }
            // construction rules (scope-wide representations for nodes)
            scopeFrom(Module::class) {
                scope {
                    // entities from current module
                    it.entities.forEach(this::define)
                    parent =
                        scope {
                            // entities from imported modules
                            it.imports.flatMap { import ->
                                // resolve module (if not done already)
                                import.module
                                    .takeUnless { moduleRef -> moduleRef.resolved }
                                    ?.apply { symbolResolver.resolve(Import::module, import) }
                                // retrieve all entities from the referenced module
                                import.module.referred?.entities ?: emptyList()
                            }.forEach(this::define)
                            parent =
                                scope {
                                    // entities from standard module
                                    StandardModule.entities.forEach(this::define)
                                }
                        }
                }
            }
            scopeFrom(Entity::class) {
                scope {
                    // all declared features
                    it.features.forEach(this::define)
                    // all declared operations
                    it.operations.forEach(this::define)
                }
            }
        }
        // type computation
        typeComputer {
            typeFor(Operation::class) {
                // resolve return type reference (if not done already)
                it.type
                    ?.takeUnless { typeRef -> typeRef.resolved }
                    ?.apply { symbolResolver.resolve(Operation::type, it) }
                // return the referenced entity (null means void)
                it.type?.referred
            }
            typeFor(Symbol::class) {
                // resolve type reference (if not done already)
                it.type
                    .takeUnless { typeRef -> typeRef.resolved }
                    ?.apply { symbolResolver.resolve(Symbol::type, it) }
                // return the referenced entity
                it.type.referred
            }
            typeFor(OperatorExpression::class) {
                // compute type of the left operand
                val leftOperandType = typeComputer.typeFor(it.left)
                // compute type of the right operand
                val rightOperandType = typeComputer.typeFor(it.right)
                // handle different operators/operands combinations
                when {
                    // all operations can be used with integer operands
                    leftOperandType == IntegerType &&
                        rightOperandType == IntegerType -> IntegerType
                    // only addition can be used with string operands (for concatenation)
                    leftOperandType == StringType &&
                        rightOperandType == StringType &&
                        it.operator == Operator.ADDITION -> StringType
                    // no operators can be used otherwise (signal error and return null)
                    else -> issues.addIncompatibleTypesError(it).let { null }
                }
            }
            typeFor(InvocationExpression::class) {
                // return the type of the referenced operation (OperationReference)
                typeComputer.typeFor(it.operation)
            }
            typeFor(OperationReference::class) {
                // resolve operation reference (if not done already)
                it.operation
                    .takeUnless { operationRef -> operationRef.resolved }
                    ?.apply { symbolResolver.resolve(OperationReference::operation, it) }
                // return the type of the referenced operation
                typeComputer.typeFor(it.operation.referred)
            }
            typeFor(ReferenceExpression::class) {
                // resolve symbol reference (if not done already)
                it.target
                    .takeUnless { symbolRef -> symbolRef.resolved }
                    ?.apply { symbolResolver.resolve(ReferenceExpression::target, it) }
                // return the type of the referenced symbol
                typeComputer.typeFor(it.target.referred)
            }
            typeFor(ConstructorExpression::class) {
                // resolve entity reference (if not done already)
                it.entity
                    .takeUnless { entityRef -> entityRef.resolved }
                    ?.apply { symbolResolver.resolve(ConstructorExpression::entity, it) }
                // return the referenced entity
                it.entity.referred
            }
            typeFor(LiteralExpression::class) {
                // resolve type reference (if not done already)
                it.type
                    .takeUnless { typeRef -> typeRef.resolved }
                    ?.apply { symbolResolver.resolve(LiteralExpression::type, it) }
                // return the referenced entity
                it.type.referred
            }
        }
    }

internal fun Module.visibleModules() =
    this.findAncestorOfType(Workspace::class.java)
        ?.modules
        ?.filter { it.name != this.name }
        ?: emptyList()

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
