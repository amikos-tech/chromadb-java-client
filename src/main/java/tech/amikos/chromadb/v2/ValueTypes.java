package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Grouping of value-type index configurations for schema defaults and keys. */
public final class ValueTypes {

    private final StringValueType string;
    private final FloatListValueType floatList;
    private final SparseVectorValueType sparseVector;
    private final IntValueType integer;
    private final FloatValueType floating;
    private final BoolValueType bool;

    private ValueTypes(Builder builder) {
        this.string = builder.string;
        this.floatList = builder.floatList;
        this.sparseVector = builder.sparseVector;
        this.integer = builder.integer;
        this.floating = builder.floating;
        this.bool = builder.bool;
    }

    public static Builder builder() { return new Builder(); }

    public StringValueType getString() { return string; }
    public FloatListValueType getFloatList() { return floatList; }
    public SparseVectorValueType getSparseVector() { return sparseVector; }
    public IntValueType getInt() { return integer; }
    public FloatValueType getFloat() { return floating; }
    public BoolValueType getBool() { return bool; }

    public boolean isEmpty() {
        return string == null
                && floatList == null
                && sparseVector == null
                && integer == null
                && floating == null
                && bool == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValueTypes)) return false;
        ValueTypes valueTypes = (ValueTypes) o;
        return Objects.equals(string, valueTypes.string)
                && Objects.equals(floatList, valueTypes.floatList)
                && Objects.equals(sparseVector, valueTypes.sparseVector)
                && Objects.equals(integer, valueTypes.integer)
                && Objects.equals(floating, valueTypes.floating)
                && Objects.equals(bool, valueTypes.bool);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, floatList, sparseVector, integer, floating, bool);
    }

    @Override
    public String toString() {
        return "ValueTypes{"
                + "string=" + string
                + ", floatList=" + floatList
                + ", sparseVector=" + sparseVector
                + ", integer=" + integer
                + ", floating=" + floating
                + ", bool=" + bool
                + '}';
    }

    public static final class Builder {
        private StringValueType string;
        private FloatListValueType floatList;
        private SparseVectorValueType sparseVector;
        private IntValueType integer;
        private FloatValueType floating;
        private BoolValueType bool;

        Builder() {}

        public Builder string(StringValueType string) { this.string = string; return this; }
        public Builder floatList(FloatListValueType floatList) { this.floatList = floatList; return this; }
        public Builder sparseVector(SparseVectorValueType sparseVector) { this.sparseVector = sparseVector; return this; }
        public Builder integer(IntValueType integer) { this.integer = integer; return this; }
        public Builder floating(FloatValueType floating) { this.floating = floating; return this; }
        public Builder bool(BoolValueType bool) { this.bool = bool; return this; }

        public ValueTypes build() { return new ValueTypes(this); }
    }
}
