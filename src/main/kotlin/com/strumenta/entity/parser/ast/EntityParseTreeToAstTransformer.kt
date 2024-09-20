package com.strumenta.entity.parser.ast

import com.strumenta.entity.parser.AntlrEntityLexer
import com.strumenta.entity.parser.AntlrEntityParser
import com.strumenta.entity.parser.AntlrEntityParser.Module_declarationContext
import com.strumenta.kolasu.mapping.ParseTreeToASTTransformer
import com.strumenta.kolasu.mapping.translateCasted
import com.strumenta.kolasu.mapping.translateList
import com.strumenta.kolasu.mapping.translateOnlyChild
import com.strumenta.kolasu.mapping.translateOptional
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.validation.Issue

fun entityParseTreeToAst(
    root: Module_declarationContext,
    issues: MutableList<Issue>,
) = EntityParseTreeToAstTransformer(issues).transform(root)

class EntityParseTreeToAstTransformer(issues: MutableList<Issue> = mutableListOf()) :
    ParseTreeToASTTransformer(issues) {
    init {
        // module
        this.registerNodeFactory(AntlrEntityParser.Module_declarationContext::class) { ctx ->
            Module(
                name = ctx.name.text,
                imports = translateList(ctx.imports),
                entities = translateList(ctx.entities),
            )
        }
        // import
        this.registerNodeFactory(AntlrEntityParser.Module_importContext::class) { ctx ->
            Import(
                module = ReferenceByName(name = ctx.name.text),
            )
        }
        // entity
        this.registerNodeFactory(AntlrEntityParser.Entity_declarationContext::class) { ctx ->
            Entity(
                name = ctx.name.text,
                features = translateList(ctx.features),
                operations = translateList(ctx.operations),
            )
        }
        // feature
        this.registerNodeFactory(AntlrEntityParser.Feature_declarationContext::class) { ctx ->
            Feature(
                name = ctx.name.text,
                type = ReferenceByName(name = ctx.type.text),
            )
        }
        // operation
        this.registerNodeFactory(AntlrEntityParser.Operation_declarationContext::class) { ctx ->
            Operation(
                name = ctx.name.text,
                type = ctx.type?.let { ReferenceByName(name = it.text) },
                parameters = translateList(ctx.parameters),
                statements = translateList(ctx.statements),
            )
        }
        // parameter
        this.registerNodeFactory(AntlrEntityParser.Parameter_declarationContext::class) { ctx ->
            Parameter(
                name = ctx.name.text,
                type = ReferenceByName(name = ctx.type.text),
            )
        }
        // statement
        this.registerNodeFactory(AntlrEntityParser.StatementContext::class) { ctx ->
            require(ctx.childCount == 1)
            translateOnlyChild(ctx)
        }
        // binding statement
        this.registerNodeFactory(AntlrEntityParser.Binding_statementContext::class) { ctx ->
            BindingStatement(
                variable = translateCasted(ctx.variable),
                value = translateCasted(ctx.value),
            )
        }
        // variable
        this.registerNodeFactory(AntlrEntityParser.Variable_declarationContext::class) { ctx ->
            Variable(
                name = ctx.name.text,
                type = ReferenceByName(ctx.type.text),
            )
        }
        // return statement
        this.registerNodeFactory(AntlrEntityParser.Return_statementContext::class) { ctx ->
            ReturnStatement(
                value = translateCasted(ctx.value),
            )
        }
        // expression
        this.registerNodeFactory(AntlrEntityParser.ExpressionContext::class) { ctx ->
            require(ctx.childCount == 1)
            translateOnlyChild(ctx)
        }
        // operator expression
        this.registerNodeFactory(AntlrEntityParser.Operator_expressionContext::class) { ctx ->
            OperatorExpression(
                left = translateCasted(ctx.left),
                right = translateCasted(ctx.right),
                operator =
                    when (ctx.op.type) {
                        AntlrEntityLexer.ADD -> Operator.ADDITION
                        AntlrEntityLexer.SUB -> Operator.SUBTRACTION
                        AntlrEntityLexer.MUL -> Operator.MULTIPLICATION
                        AntlrEntityLexer.DIV -> Operator.DIVISION
                        else -> error("Unknown operator...")
                    },
            )
        }
        // invocation expression
        this.registerNodeFactory(AntlrEntityParser.Invocation_expressionContext::class) { ctx ->
            InvocationExpression(
                context = translateCasted(ctx.context),
                operation = ReferenceByName(name = ctx.target.text),
                arguments = translateList(ctx.argument_list().arguments),
            )
        }
        // reference expression
        this.registerNodeFactory(AntlrEntityParser.Reference_expressionContext::class) { ctx ->
            ReferenceExpression(
                context = translateOptional(ctx.context),
                target = ReferenceByName(name = ctx.target.text),
            )
        }
        // constructor expression
        this.registerNodeFactory(AntlrEntityParser.Constructor_expressionContext::class) { ctx ->
            ConstructorExpression(
                entity = ReferenceByName(name = ctx.name.text),
                arguments = translateList(ctx.argument_list().arguments),
            )
        }
        // literal expression
        this.registerNodeFactory(AntlrEntityParser.Literal_expressionContext::class) { ctx ->
            when (ctx.value.type) {
                AntlrEntityLexer.STRING -> StringLiteralExpression(ctx.value.text)
                AntlrEntityLexer.INTEGER -> IntegerLiteralExpression(ctx.value.text.toLong())
                AntlrEntityLexer.BOOLEAN -> BooleanLiteralExpression(ctx.value.text.toBoolean())
                else -> error("Unknown literal type...")
            }
        }
    }
}
