internal inline fun forEachMapTypes(block: (left: FastutilType, right: FastutilType) -> Unit) {
    FastutilType.values().forEach { left ->
        if (left == FastutilType.BOOLEAN) return@forEach // BooleanMap does not exist

        FastutilType.values().forEach { right ->
            block(left, right)
        }
    }
}
