package Server.model;

public enum SearchType {
    BY_NAME("byName", true),      // 默认使用模糊搜索
    BY_STUDENT_NUMBER("byStudentNumber", false), // 默认使用精确搜索
    BY_CARD_NUMBER("byCardNumber", false); // 默认使用精确搜索

    private final String value;
    private final boolean fuzzyDefault;

    SearchType(String value, boolean fuzzyDefault) {
        this.value = value;
        this.fuzzyDefault = fuzzyDefault;
    }

    public String getValue() {
        return value;
    }

    public boolean isFuzzyDefault() {
        return fuzzyDefault;
    }

    public static SearchType fromValue(String value) {
        for (SearchType type : SearchType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的搜索类型: " + value);
    }
}
