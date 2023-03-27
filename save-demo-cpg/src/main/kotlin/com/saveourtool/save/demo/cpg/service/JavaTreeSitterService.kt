package com.saveourtool.save.demo.cpg.service

import ai.serenade.treesitter.Languages
import ai.serenade.treesitter.Node
import ai.serenade.treesitter.Parser
import ai.serenade.treesitter.TreeCursor
import com.saveourtool.save.demo.cpg.entity.TreeSitterLocation
import com.saveourtool.save.demo.cpg.entity.TreeSitterNode
import org.springframework.stereotype.Service
import java.nio.file.Path
import javax.annotation.PostConstruct
import kotlin.io.path.*

/**
 * A service for [tree-sitter](https://tree-sitter.github.io/tree-sitter/) using [java-tree-sitter](https://github.com/serenadeai/java-tree-sitter)
 */
@Service
class JavaTreeSitterService {
    @PostConstruct
    fun init() {
        System.load("/mnt/d/projects/java-tree-sitter/libjava-tree-sitter.so")
    }

    /**
     * Translate all code in provided folder
     *
     * @param folder
     * @return result from CPG with logs
     */
    @OptIn(ExperimentalPathApi::class)
    fun translate(folder: Path): Map<String, List<TreeSitterNode>> {
        return Parser().use { parser ->
            parser.setLanguage(Languages.java())
            folder.walk()
                .map { path ->
                    val fileName = path.relativize(folder).pathString
                    parser.parseString(path.readText())
                        .use { tree ->
                            tree.rootNode.walk()
                                .use { cursor ->
                                    fileName to cursor.getNodes(fileName)
                                }
                        }
                }
                .toMap()
        }
    }

    companion object {
        private fun TreeCursor.doGenerateNodes(fileName: String): Sequence<Pair<TreeCursor, TreeSitterNode>> =
            generateSequence(this to this.toTreeSitterNode(fileName)) { (cursor, currentNode) ->
                if (cursor.gotoFirstChild()) {
                    return@generateSequence cursor to cursor.toTreeSitterNode(
                        fileName = fileName,
                        parent = currentNode
                    )
                }
                if (cursor.gotoNextSibling()) {
                    return@generateSequence cursor to cursor.toTreeSitterNode(
                        fileName = fileName,
                        parent = currentNode.parent,
                        prev = currentNode,
                    )
                }
                if (cursor.gotoParent() && cursor.gotoNextSibling()) {
                    return@generateSequence cursor to cursor.toTreeSitterNode(
                        fileName = fileName,
                        parent = currentNode.parent?.parent,
                        prev = currentNode.parent,
                    )
                }
                null
            }

        private fun TreeCursor.generateNodes(fileName: String): Sequence<TreeSitterNode> =
                doGenerateNodes(fileName).map { (_, currentNode) -> currentNode }

        private fun TreeCursor.getNodes(fileName: String): List<TreeSitterNode> = generateNodes(fileName)
            .toList()
            .asReversed()
            .onEach { node ->
                node.parent?.child?.apply { this.add(node) }
                node.prev?.apply {
                    this.next = node
                }
            }

        private fun TreeCursor.toTreeSitterNode(
            fileName: String,
            parent: TreeSitterNode? = null,
            prev: TreeSitterNode? = null,
        ): TreeSitterNode = currentNode.toTreeSitterNode(fileName, parent, prev)

        private fun Node.toTreeSitterNode(
            fileName: String,
            parent: TreeSitterNode? = null,
            prev: TreeSitterNode? = null,
        ): TreeSitterNode = TreeSitterNode(
            prev = prev,
            next = null,
            parent = parent,
            child = mutableListOf(),
            location = TreeSitterLocation(
                fileName = fileName,
                startBytes = this.startByte,
                endBytes = this.endByte,
            ),
            localName = this.type,
            code = this.nodeString,
        )
    }
}