package com.vatsalya.founderpocket.data.ml

/**
 * Minimal WordPiece tokenizer for BERT-style models (all-MiniLM-L6-v2).
 * Expects vocab.txt loaded from assets — one token per line, index = line number.
 */
class WordPieceTokenizer(private val vocab: Map<String, Int>) {

    private val clsId = vocab[CLS] ?: 101
    private val sepId = vocab[SEP] ?: 102
    private val unkId = vocab[UNK] ?: 100
    private val padId = vocab[PAD] ?: 0

    data class Encoding(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray
    )

    fun encode(text: String, maxLen: Int = MAX_LEN): Encoding {
        val words = text.lowercase().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val tokenIds = mutableListOf(clsId.toLong())

        for (word in words) {
            val pieces = wordpiece(word)
            if (tokenIds.size + pieces.size >= maxLen - 1) break
            tokenIds.addAll(pieces)
        }
        tokenIds.add(sepId.toLong())

        val len = tokenIds.size.coerceAtMost(maxLen)
        return Encoding(
            inputIds    = LongArray(len) { tokenIds[it] },
            attentionMask = LongArray(len) { 1L },
            tokenTypeIds  = LongArray(len) { 0L }
        )
    }

    private fun wordpiece(word: String): List<Long> {
        if (word.isEmpty()) return emptyList()
        val result = mutableListOf<Long>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var found = false
            while (start < end) {
                val sub = if (start == 0) word.substring(start, end) else "##${word.substring(start, end)}"
                val id = vocab[sub]
                if (id != null) {
                    result.add(id.toLong())
                    start = end
                    found = true
                    break
                }
                end--
            }
            if (!found) { result.add(unkId.toLong()); break }
        }
        return result
    }

    companion object {
        const val MAX_LEN = 128
        private const val CLS = "[CLS]"
        private const val SEP = "[SEP]"
        private const val UNK = "[UNK]"
        private const val PAD = "[PAD]"
    }
}
