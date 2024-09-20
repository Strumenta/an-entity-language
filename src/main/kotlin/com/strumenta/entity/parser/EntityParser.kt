package com.strumenta.entity.parser

import com.strumenta.entity.parser.ast.entityParseTreeToAst
import com.strumenta.entity.parser.metamodel.EntityMetamodelBuilder
import com.strumenta.kolasu.emf.EcoreEnabledParser
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.parsing.ANTLRTokenFactory
import com.strumenta.kolasu.parsing.KolasuANTLRToken
import com.strumenta.kolasu.validation.Issue
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.TokenStream
import org.eclipse.emf.ecore.resource.Resource

class EntityParser : EcoreEnabledParser<Node, AntlrEntityParser, AntlrEntityParser.Module_declarationContext, KolasuANTLRToken>(
    ANTLRTokenFactory(),
) {
    override fun createANTLRLexer(charStream: CharStream): Lexer = AntlrEntityLexer(charStream)

    override fun createANTLRParser(tokenStream: TokenStream): AntlrEntityParser = AntlrEntityParser(tokenStream)

    override fun doGenerateMetamodel(resource: Resource) = EntityMetamodelBuilder(resource).generate().let { }

    override fun parseTreeToAst(
        parseTreeRoot: AntlrEntityParser.Module_declarationContext,
        considerPosition: Boolean,
        issues: MutableList<Issue>,
        source: Source?,
    ): Node? = entityParseTreeToAst(parseTreeRoot, issues)
}
