enum class FastutilType(
    val typeName: String,
    val lowercaseName: String,
    val isGeneric: Boolean,
) {
    BOOLEAN("Boolean", "boolean", false),
    BYTE("Byte", "byte", false),
    CHAR("Char", "char", false),
    DOUBLE("Double", "double", false),
    FLOAT("Float", "float", false),
    INT("Int", "int", false),
    LONG("Long", "long", false),
    OBJECT("Object", "object", true),
    SHORT("Short", "short", false),
    REFERENCE("Reference", "reference", true),
    ;

    override fun toString(): String = typeName
}
