package com.github.maratmingazov.spring_boot_starter_markov_chain_model

import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import kotlin.math.roundToInt

/**
 * A generic Markov Chain model that supports variable-length contexts.
 *
 * <p>This model works with arbitrary token types via generics (`T`).
 * It uses an n-order Markov chain where {@code order} defines the maximum
 * context size used during training and prediction.</p>
 *
 * <h2>How training works</h2>
 * <p>During training, the model iterates through the input sequence and builds
 * transition statistics for every possible context length from 1 up to {@code order}.
 * For example, with order = 3 and sequence [A, B, C, D], the following contexts are learned:</p>
 *
 * <pre>
 * Len=1: [A] → B,  [B] → C,  [C] → D
 * Len=2: [A, B] → C,  [B, C] → D
 * Len=3: [A, B, C] → D
 * </pre>
 *
 * <h2>Transitions representation</h2>
 * <p>The internal {@code transitions} map stores frequencies in the form:</p>
 *
 * <pre>
 * {
 *   ["A"] -> { B: 5, C: 2 },
 *   ["A", "B"] -> { C: 3 },
 *   ["B", "C"] -> { D: 1 }
 * }
 * </pre>
 *
 * <h2>Prediction (Backoff Strategy)</h2>
 * <p>Prediction uses a backoff strategy:
 * it first attempts to match the longest possible context (length == order),
 * and if none is found, reduces context size step-by-step until it reaches 1.</p>
 *
 * <p>The returned predictions contain the top N most likely next tokens along with
 * their probabilities expressed as percentages.</p>
 *
 * @param T type of tokens used by the model
 * @param order maximum Markov chain order (maximum context length)
 */
class MarkovModel<T>(private val order: Int) {

    private val transitions = mutableMapOf<List<T>, MutableMap<T, Int>>()
    private val mapper = jacksonObjectMapper()

    /**
     * Trains the Markov model on a sequence of tokens.
     *
     * <p>The model builds transition statistics for all context lengths
     * from 1 up to {@code order}. For each position {@code i}, all subsequences
     * {@code sequence[i .. i+len)} are collected, and the next token
     * {@code sequence[i + len]} increments its frequency.</p>
     *
     * @param sequence the list of tokens used to train the model
     */
    fun train(sequence: List<T>) {
        if (sequence.size <= 1) return

        for (i in 0 until sequence.size) {
            // длины контекста от 1 до order
            for (len in 1..minOf(order, sequence.size - i - 1)) {
                val context = sequence.subList(i, i + len) // immutable!
                val next = sequence[i + len]

                val nextMap = transitions.getOrPut(context) { mutableMapOf() }
                nextMap[next] = nextMap.getOrDefault(next, 0) + 1
            }
        }
    }

    /**
     * Predicts the next most likely tokens given a context.
     *
     * <p>The model first tries to use the longest possible context (up to {@code order}).
     * If no transition is found, it falls back to shorter contexts (order - 1, order - 2, ...).</p>
     *
     * @param context the recent sequence of tokens (the last items matter most)
     * @param topTokens the maximum number of predictions to return
     * @return a list of pairs (token, probability%), sorted by probability descending
     */
    fun predict(context: List<T>, topTokens: Int): List<Pair<T, Int>> {

        var currentOrder = minOf(context.size, order)
        while (currentOrder > 0) {
            val subContext = context.takeLast(currentOrder).toList()
            val nextMap = transitions[subContext]

            if (nextMap != null && nextMap.isNotEmpty()) {
                val total = nextMap.values.sum().toDouble()

                val topNext = nextMap.entries
                    .sortedByDescending { it.value }
                    .take(topTokens)
                    .map { it.key to (it.value / total * 100).roundToInt() } // округляем

                return topNext
            }

            currentOrder--
        }

        return emptyList()
    }

    /**
     * Saves the Markov model (order and transitions) to a JSON file.
     * Like "/Users/username/Desktop/model.json"
     *
     * @param path filesystem path to write the JSON file
     */
    fun saveToFile(path: String) {

        if (transitions.isEmpty())  return

        val data = mapOf(
            "order" to order,
            "transitions" to transitions
        )

        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(File(path), data)
    }

    /**
     * Loads the Markov model state (order and transitions) from a JSON file.
     *
     * <p>Reconstructs the internal transitions map by parsing keys such as:
     * {@code "[A, B, C]"} back into context lists.</p>
     *
     * @param path filesystem path to the JSON model file
     */
    @Suppress("UNCHECKED_CAST")
    fun loadFromFile(path: String) {

        val rootNode = mapper.readTree(File(path))
        transitions.clear()

        val transitionsNode = rootNode["transitions"]

        transitionsNode.properties().forEach { (key, valueNode) ->
            // key — это строка вида "[A, B, C]"
            val context: List<T> = key
                .removePrefix("[")
                .removeSuffix("]")
                .split(", ")
                .filter { it.isNotBlank() }
                .map { stringValue ->  stringValue as T}

            val nextMap: MutableMap<T, Int> = mutableMapOf()
            valueNode.properties().forEach { (next, countNode) -> nextMap[next as T] = countNode.asInt() }

            transitions[context] = nextMap
        }
    }

}