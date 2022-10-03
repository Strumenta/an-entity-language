package com.strumenta.entity.parser

import com.andreapivetta.kolor.green
import com.andreapivetta.kolor.yellow
import com.strumenta.kolasu.emf.saveMetamodel
import com.strumenta.kolasu.emf.saveModel
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.pos
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.serialization.JsonGenerator
import com.strumenta.kolasu.serialization.computeIdsForReferencedNodes
import com.strumenta.kolasu.testing.IgnoreChildren
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.kolasu.testing.assertParsingResultsAreEqual
import com.strumenta.kolasu.traversing.findAncestorOfType
import com.strumenta.kolasu.traversing.findByPosition
import com.strumenta.kolasu.traversing.searchByPosition
import com.strumenta.kolasu.traversing.searchByType
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.traversing.walkAncestors
import com.strumenta.kolasu.traversing.walkChildren
import com.strumenta.kolasu.traversing.walkDescendants
import com.strumenta.kolasu.traversing.walkLeavesFirst
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import org.eclipse.emf.common.util.URI
import org.junit.Test
import java.io.File
import kotlin.io.path.Path

internal class EntityParserTest {

    private val parser: EntityParser = EntityParser()

    @Test
    fun nodeProperties() {
        module().walk().forEach { node ->
            println()
            // what type of node is this?
            println("NodeType: ".yellow() + node.nodeType)
            // what are the properties of this node?
            println("Properties: ".yellow() + node.properties.joinToString { it.name })
            // where does this node come from?
            println("Origin: ".yellow() + node.origin)
            // who is the parent of this node, if any?
            println("Parent: ".yellow() + node.parent)
            // what is the exact position of this node?
            println("Position: ".yellow() + node.position)
            // what text does this node correspond to?
            println("Source Text: ".yellow() + node.sourceText)
            // what node has been created started from this one?
            println("Destination: ".yellow() + node.destination)
            // type-specific information
            when (node) {
                is Module -> {
                    println("Name: ".green() + node.name)
                    println("Entities: ".green() + node.entities)
                }

                is Entity -> {
                    println("Name: ".green() + node.name)
                    println("Features: ".green() + node.features)
                }

                is Feature -> {
                    println("Name: ".green() + node.name)
                    println("Type: ".green() + node.type)
                }

                is EntityRefType -> {
                    println("Target: ".green() + node.target)
                }
            }
            println()
        }
    }

    @Test
    fun traversalWalk() {
        // Traverse the entire tree, depth first, starting from Demo
        module().walk().forEachIndexed { index, node ->
            println("$index: $node".yellow())
        }
    }

    @Test
    fun traversalWalkLeavesFirst() {
        // Perform a post-order (or leaves-first) node traversal starting from Demo
        module().walkLeavesFirst().forEachIndexed { index, node ->
            println("$index: $node".yellow())
        }
    }

    @Test
    fun traversalWalkAncestors() {
        // Traverse all ancestors from Demo::FirstEntity::name all the way up the root.
        module().entities[0].features[0].walkAncestors().forEachIndexed { index, node ->
            println("$index: $node".yellow())
        }
    }

    @Test
    fun traversalWalkChildren() {
        // Traverse all direct children from Demo
        module().walkChildren().forEachIndexed { index, node ->
            println("$index: $node".yellow())
        }
    }

    @Test
    fun traversalWalkDescendants() {
        // Traverse all nodes from Demo except this same node
        module().walkDescendants(walker = Node::walk).forEachIndexed { index, node ->
            println("$index: $node".yellow())
        }
    }

    @Test
    fun traversalFindAncestorOfType() {
        val secondEntityNameFeature = module().entities[1].features[0]
        // Retrieve the nearest Module ancestor from Demo::SecondEntity::name -> Demo
        println("${secondEntityNameFeature.findAncestorOfType(klass = Module::class.java)}".yellow())
        // Retrieve the nearest Entity ancestor from Demo::SecondEntity::name -> SecondEntity
        println("${secondEntityNameFeature.findAncestorOfType(klass = Entity::class.java)}".yellow())
        // Retrieve the nearest Type ancestor from Demo::SecondEntity::name -> null
        println("${secondEntityNameFeature.findAncestorOfType(klass = Type::class.java)}".yellow())
    }

    @Test
    fun traversalSearchByType() {
        val module = module()
        // Retrieve all Module instances from Demo
        module().searchByType(klass = Module::class.java, walker = Node::walk)
            .forEachIndexed { index, node -> println("$index: $node".yellow()) }
        // Retrieve all Entity instances from Demo
        module().searchByType(klass = Entity::class.java, walker = Node::walk)
            .forEachIndexed { index, node -> println("$index: $node".yellow()) }
        // Retrieve all Feature instances from Demo
        module().searchByType(klass = Feature::class.java, walker = Node::walk)
            .forEachIndexed { index, node -> println("$index: $node".yellow()) }
        // Retrieve all Type instances from Demo
        module().searchByType(klass = Type::class.java, walker = Node::walk)
            .forEachIndexed { index, node -> println("$index: $node".yellow()) }
    }

    @Test
    fun traversalFindByPosition() {
        val module = module()
        // Given a position, find the nearest corresponding node, if any.
        // Self-contained -> children position always contained in parent
        // E.g. Demo::SecondEntity::name::type -> StringType
        println(
            "${
            module.findByPosition(
                position = module.entities[1].features[0].type!!.position!!, selfContained = true
            )
            }".yellow()
        )
    }

    @Test
    fun traversalSearchByPosition() {
        val module = module()
        // Search all nodes contained in a given position.
        // Self-contained -> children position always contained in parent
        // E.g. Demo::SecondEntity position
        module.searchByPosition(position = module.entities[1].position!!, selfContained = true)
            .forEachIndexed { index, node -> println("$index: $node".yellow()) }
    }

    @Test
    fun testingSupportAssertASTsAreEqual() {
        assertASTsAreEqual(
            Module(
                name = "Demo",
                entities = listOf(
                    Entity(
                        name = "FirstEntity", features = IgnoreChildren()
                    ),
                    Entity(
                        name = "SecondEntity",
                        features = listOf(
                            Feature(
                                name = "name", type = StringType()
                            )
                        )
                    )
                )
            ),
            module()
        )
    }

    @Test
    fun testingSupportAssertParsingResultsAreEqualSyntactic() {
        val actualParsingResult = parser.parse(
            """
                module ExampleModule {
                    entity ExampleEntity {
                        name: string;
                    }
            """.trimIndent()
        ) as ParsingResult<Module>
        val expectedParsingResult = ParsingResult(
            issues = listOf(
                Issue.syntactic(
                    message = "extraneous input '<EOF>' expecting {'entity', '}'}",
                    severity = IssueSeverity.ERROR,
                    position = pos(4, 5, 4, 5),
                ),
                Issue.syntactic(
                    message = "Error node found (token: <missing '}'>)",
                    severity = IssueSeverity.ERROR,
                    position = pos(4, 4, 4, 17)
                )
            ),
            root = Module(
                name = "ExampleModule",
                entities = listOf(
                    Entity(
                        name = "ExampleEntity",
                        features = listOf(
                            Feature(
                                name = "name", type = StringType()
                            )
                        )
                    )
                )
            )
        )
        assertParsingResultsAreEqual(expectedParsingResult, actualParsingResult)
    }

    @Test
    fun testingSupportAssertParsingResultsAreEqualSemantic() {
        val actualParsingResult = parser.parse(
            """
                module ExampleModule {
                    entity ExampleEntity {
                        target: NotExistingEntity;
                    }
                }
            """.trimIndent()
        ) as ParsingResult<Module>
        val expectedParsingResult = ParsingResult<Module>(
            issues = listOf(
                Issue.semantic(
                    message = "Entity NotExistingEntity not found",
                    severity = IssueSeverity.ERROR,
                    position = pos(3, 16, 3, 33)
                )
            ),
            root = Module(
                name = "ExampleModule",
                entities = listOf(
                    Entity(
                        name = "ExampleEntity",
                        features = listOf(
                            Feature(
                                name = "target",
                                type = EntityRefType(target = ReferenceByName(name = "NotExistingEntity"))
                            )
                        )
                    )
                )
            )
        )
        assertParsingResultsAreEqual(expectedParsingResult, actualParsingResult)
    }

    @Test
    fun exportSupportEcore() {
        val metamodelURI = URI.createFileURI(Path("src/test/resources/metamodel.json").toString())
        val metamodel = parser.saveMetamodel(metamodelURI)
        parser.parse(
            """
            module ExampleModule {
                entity ExampleEntity {
                    name: string;
                }
            }
            """.trimIndent(),
            considerPosition = false
        ).saveModel(
            metamodel, URI.createFileURI(Path("src/test/resources/model.json").toString()), includeMetamodel = false
        )
    }

    @Test
    fun exportSupportNative() {
        val module = module()
        JsonGenerator().generateFile(
            root = module,
            file = File("src/test/resources/model-native.json"),
            withIds = module.computeIdsForReferencedNodes()
        )
    }

    private fun module(): Module {
        val input = """
            module Demo {
                entity FirstEntity {
                    name: string;
                    second: SecondEntity;
                }
                entity SecondEntity {
                    name: string;
                }
            }
        """.trimIndent()
        val ast = this.parser.parse(input) // parse code and build AST
        return ast.root!! as Module // retrieve AST root module
    }
}
