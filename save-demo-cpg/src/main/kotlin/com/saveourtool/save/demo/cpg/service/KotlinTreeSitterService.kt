package com.saveourtool.save.demo.cpg.service

import com.saveourtool.save.demo.cpg.entity.TreeSitterLocation
import com.saveourtool.save.demo.cpg.entity.TreeSitterNode
import io.github.oxisto.kotlintree.jvm.CppParser
import io.github.oxisto.kotlintree.jvm.Node
import io.github.oxisto.kotlintree.jvm.TreeCursor
import io.github.oxisto.kotlintree.jvm.TreeSitter
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.*

/**
 *
 * A service for [tree-sitter](https://tree-sitter.github.io/tree-sitter/) using [kotlintree](https://github.com/oxisto/kotlintree)
 */
@Service
class KotlinTreeSitterService {
    /**
     * Translate all code in provided folder
     *
     * @param folder
     * @return result from CPG with logs
     */
    @OptIn(ExperimentalPathApi::class)
    fun translate(folder: Path): Map<String, List<TreeSitterNode>> {
        return CppParser().use { parser ->
            folder.walk()
                .map { path ->
                    val fileName = path.relativize(folder).pathString
                    val tree = parser.parse(path.toFile())
                    fileName to tree.rootNode.newCursor()?.getNodes(fileName).orEmpty()
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
                if (cursor.hasNext()) {
                    cursor.gotoNextSibling()
                    return@generateSequence cursor to cursor.toTreeSitterNode(
                        fileName = fileName,
                        parent = currentNode.parent,
                        prev = currentNode,
                    )
                }
                cursor.gotoParent()
                if (cursor.hasNext()) {
                    cursor.gotoNextSibling()
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
            localName = this.type ?: "N/A",
            code = this.string ?: "N/A",
        )

        private fun TreeCursor.gotoParent() {
            TreeSitter.INSTANCE.ts_tree_cursor_goto_parent(pointer)
        }
    }
}