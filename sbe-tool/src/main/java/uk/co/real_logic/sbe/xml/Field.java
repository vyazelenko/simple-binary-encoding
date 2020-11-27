/*
 * Copyright 2013-2020 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.sbe.xml;

import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

import static uk.co.real_logic.sbe.xml.XmlSchemaParser.checkForValidName;
import static uk.co.real_logic.sbe.xml.XmlSchemaParser.handleError;

/**
 * Representation for a field (or group or data) member from the SBE schema.
 */
public final class Field
{
    /**
     * Constant to represent an invalid schema ID.
     */
    public static final int INVALID_ID = Integer.MAX_VALUE;  // schemaId must be a short, so this is way out of range.

    private final String name;                 // required for field/data & group
    private final String description;          // optional for field/data & group
    private final int id;                      // required for field/data (not present for group)
    private final Type type;                   // required for field/data (not present for group)
    private final int offset;                  // optional for field/data (not present for group)
    private final String semanticType;         // optional for field/data (not present for group?)
    private final Presence presence;           // optional, defaults to required
    private final String valueRef;             // optional, defaults to null
    private final int blockLength;             // optional for group (not present for field/data)
    private final CompositeType dimensionType; // required for group (not present for field/data)
    private final boolean variableLength;      // true for data (false for field/group)
    private final int sinceVersion;            // optional
    private final int deprecated;              // optional
    private List<Field> groupFieldList;        // used by group fields as the list of child fields in the group
    private int computedOffset;                // holds the calculated offset of this field from message or group
    private int computedBlockLength;           // used to hold the calculated block length of this group
    private final String epoch;                // optional, epoch from which a timestamps start, defaults to "unix"
    private final String timeUnit;             // optional, defaults to "nanosecond".

    /**
     * Constructs {@link Field} instance.
     *
     * @param name           of the field/data/group in a message.
     * @param description    of what the field/data/group is for.
     * @param id             of the field/data.
     * @param type           of the field/data.
     * @param offset         of the field/data within the message.
     * @param semanticType   of the field/data.
     * @param presence       of the field.
     * @param valueRef       of the field.
     * @param blockLength    of the group.
     * @param dimensionType  of the group.
     * @param variableLength true for data (false for field/group).
     * @param sinceVersion   the field was added in.
     * @param deprecated     as of this version.
     * @param epoch          from which a timestamps start.
     * @param timeUnit       of the field.
     */
    public Field(
        final String name,
        final String description,
        final int id,
        final Type type,
        final int offset,
        final String semanticType,
        final Presence presence,
        final String valueRef,
        final int blockLength,
        final CompositeType dimensionType,
        final boolean variableLength,
        final int sinceVersion,
        final int deprecated,
        final String epoch,
        final String timeUnit)
    {
        this.name = name;
        this.description = description;
        this.id = id;
        this.type = type;
        this.offset = offset;
        this.semanticType = semanticType;
        this.presence = presence;
        this.valueRef = valueRef;
        this.blockLength = blockLength;
        this.dimensionType = dimensionType;
        this.variableLength = variableLength;
        this.sinceVersion = sinceVersion;
        this.deprecated = deprecated;
        this.groupFieldList = null;
        this.computedOffset = 0;
        this.computedBlockLength = 0;
        this.epoch = epoch;
        this.timeUnit = timeUnit;
    }

    /**
     * Validate the field/data/group definition.
     *
     * @param node          from the XML Schema Parsing.
     * @param typeByNameMap name to type mappings.
     */
    public void validate(final Node node, final Map<String, Type> typeByNameMap)
    {
        if (type != null &&
            semanticType != null &&
            type.semanticType() != null &&
            !semanticType.equals(type.semanticType()))
        {
            handleError(node, "Mismatched semanticType on type and field: " + name);
        }

        checkForValidName(node, name);

        if (null != valueRef)
        {
            validateValueRef(node, typeByNameMap);
        }

        if (type instanceof EnumType && presence == Presence.CONSTANT)
        {
            if (null == valueRef)
            {
                handleError(node, "valueRef not set for constant enum");
            }
        }

        if (null != valueRef && presence == Presence.CONSTANT)
        {
            final String valueRefType = valueRef.substring(0, valueRef.indexOf('.'));

            if (!(type instanceof EnumType))
            {
                if (type instanceof EncodedDataType)
                {
                    final EnumType enumType = (EnumType)typeByNameMap.get(valueRefType);

                    if (((EncodedDataType)type).primitiveType() != enumType.encodingType())
                    {
                        handleError(node, "valueRef does not match field type: " + valueRef);
                    }
                }
                else
                {
                    handleError(node, "valueRef does not match field type: " + valueRef);
                }
            }
            else if (!valueRefType.equals(type.name()))
            {
                handleError(node, "valueRef for enum name not found: " + valueRefType);
            }
        }
    }

    /**
     * Set the list of child fields in the group.
     *
     * @param fields child fields in the group.
     */
    public void groupFields(final List<Field> fields)
    {
        groupFieldList = fields;
    }

    /**
     * Returns the list of child fields in the group.
     *
     * @return the list of child fields in the group.
     */
    public List<Field> groupFields()
    {
        return groupFieldList;
    }

    /**
     * Set the calculated offset of this field from message or group.
     *
     * @param offset the calculated offset of this field.
     */
    public void computedOffset(final int offset)
    {
        computedOffset = offset;
    }

    /**
     * Returns the calculated offset of this field from message or group.
     *
     * @return the calculated offset of this field from message or group.
     */
    public int computedOffset()
    {
        return computedOffset;
    }

    /**
     * Returns the name of the field/data/group.
     *
     * @return the name of the field/data/group.
     */
    public String name()
    {
        return name;
    }

    /**
     * Returns the description of the field/data/group.
     *
     * @return the description of the field/data/group.
     */
    public String description()
    {
        return description;
    }

    /**
     * Returns the id of the field/data.
     *
     * @return the id of the field/data.
     */
    public int id()
    {
        return id;
    }

    /**
     * Returns the type of the field/data.
     *
     * @return the type of the field/data.
     */
    public Type type()
    {
        return type;
    }

    /**
     * Returns the offset of the field/data.
     *
     * @return the offset of the field/data.
     */
    public int offset()
    {
        return offset;
    }

    /**
     * Returns the block length of the group.
     *
     * @return the block length of the group.
     */
    public int blockLength()
    {
        return blockLength;
    }

    /**
     * Sets the calculated block length of this group.
     *
     * @param length the calculated block length of this group.
     */
    public void computedBlockLength(final int length)
    {
        computedBlockLength = length;
    }

    /**
     * Returns the calculated block length of this group.
     *
     * @return the calculated block length of this group.
     */
    public int computedBlockLength()
    {
        return computedBlockLength;
    }

    /**
     * Returns the presence.
     *
     * @return the presence.
     */
    public Presence presence()
    {
        return presence;
    }

    /**
     * Returns the value ref.
     *
     * @return the value ref.
     */
    public String valueRef()
    {
        return valueRef;
    }

    /**
     * Returns the semantic type of the field/data.
     *
     * @return the semantic type of the field/data.
     */
    public String semanticType()
    {
        return semanticType;
    }

    /**
     * Returns the dimension type of the group.
     *
     * @return the dimension type of the group.
     */
    public CompositeType dimensionType()
    {
        return dimensionType;
    }

    /**
     * Returns {@code true} if this is data.
     *
     * @return {@code true} if this is data.
     */
    public boolean isVariableLength()
    {
        return variableLength;
    }

    /**
     * Returns the version the field/data/group was added in.
     *
     * @return the version the field/data/group was added in.
     */
    public int sinceVersion()
    {
        return sinceVersion;
    }

    /**
     * Returns the version in which the field/data/group was deprecated.
     *
     * @return the version in which the field/data/group was deprecated.
     */
    public int deprecated()
    {
        return deprecated;
    }

    /**
     * Returns epoch from which a timestamps start, defaults to "unix".
     *
     * @return epoch from which a timestamps start, defaults to "unix".
     */
    public String epoch()
    {
        return epoch;
    }

    /**
     * Returns time unit for the time-based field.
     *
     * @return time unit for the time-based field.
     */
    public String timeUnit()
    {
        return timeUnit;
    }

    public String toString()
    {
        return "Field{name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", id=" + id +
            ", type=" + type +
            ", offset=" + offset +
            ", semanticType='" + semanticType + '\'' +
            ", presence=" + presence +
            ", valueRef='" + valueRef + '\'' +
            ", blockLength=" + blockLength +
            ", dimensionType=" + dimensionType +
            ", variableLength=" + variableLength +
            ", sinceVersion=" + sinceVersion +
            ", deprecated=" + deprecated +
            ", groupFieldList=" + groupFieldList +
            ", computedOffset=" + computedOffset +
            ", computedBlockLength=" + computedBlockLength +
            ", epoch='" + epoch + '\'' +
            ", timeUnit=" + timeUnit +
            '}';
    }

    private void validateValueRef(final Node node, final Map<String, Type> typeByNameMap)
    {
        final int periodIndex = valueRef.indexOf('.');
        if (periodIndex < 1 || periodIndex == (valueRef.length() - 1))
        {
            handleError(node, "valueRef format not valid (enum-name.valid-value-name): " + valueRef);
        }

        final String valueRefType = valueRef.substring(0, periodIndex);
        final Type valueType = typeByNameMap.get(valueRefType);
        if (null == valueType)
        {
            handleError(node, "valueRef for enum name not found: " + valueRefType);
        }

        if (valueType instanceof EnumType)
        {
            final EnumType enumType = (EnumType)valueType;
            final String validValueName = valueRef.substring(periodIndex + 1);

            if (null == enumType.getValidValue(validValueName))
            {
                handleError(node, "valueRef for validValue name not found: " + validValueName);
            }
        }
        else
        {
            handleError(node, "valueRef for is not of type enum: " + valueRefType);
        }
    }

    /**
     * Builder to make creation of {@link Field} easier.
     */
    public static class Builder
    {
        private String name;
        private String description;
        private int id = INVALID_ID;
        private Type type;
        private int offset;
        private String semanticType;
        private Presence presence;
        private String refValue;
        private int blockLength;
        private CompositeType dimensionType;
        private boolean variableLength;
        private int sinceVersion = 0;
        private int deprecated = 0;
        private String epoch;
        private String timeUnit;

        /**
         * Set the name of the field/data/group.
         *
         * @param name of the field/data/group.
         * @return this for a fluent API.
         */
        public Builder name(final String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Set the description of the field/data/group.
         *
         * @param description of the field/data/group.
         * @return this for a fluent API.
         */
        public Builder description(final String description)
        {
            this.description = description;
            return this;
        }

        /**
         * Set the id of the field/data.
         *
         * @param id of the field/data.
         * @return this for a fluent API.
         */
        public Builder id(final int id)
        {
            this.id = id;
            return this;
        }

        /**
         * Set the type of the field/data.
         *
         * @param type of the field/data.
         * @return this for a fluent API.
         */
        public Builder type(final Type type)
        {
            this.type = type;
            return this;
        }

        /**
         * Set the offset of the field/data.
         *
         * @param offset of the field/data in the message.
         * @return this for a fluent API.
         */
        public Builder offset(final int offset)
        {
            this.offset = offset;
            return this;
        }

        /**
         * Set the semantic type of the field/data.
         *
         * @param semanticType of the field/data.
         * @return this for a fluent API.
         */
        public Builder semanticType(final String semanticType)
        {
            this.semanticType = semanticType;
            return this;
        }

        /**
         * Set the presence of the field/data/group.
         *
         * @param presence of the field/data/group.
         * @return this for a fluent API.
         */
        public Builder presence(final Presence presence)
        {
            this.presence = presence;
            return this;
        }

        /**
         * Set the value ref of the field/data/group.
         *
         * @param refValue of the field/data/group.
         * @return this for a fluent API.
         */
        public Builder valueRef(final String refValue)
        {
            this.refValue = refValue;
            return this;
        }

        /**
         * Set the block length of the group.
         *
         * @param blockLength in bytes of the group.
         * @return this for a fluent API.
         */
        public Builder blockLength(final int blockLength)
        {
            this.blockLength = blockLength;
            return this;
        }

        /**
         * Set the dimension type of the group.
         *
         * @param dimensionType of the group.
         * @return this for a fluent API.
         */
        public Builder dimensionType(final CompositeType dimensionType)
        {
            this.dimensionType = dimensionType;
            return this;
        }

        /**
         * Mark the field as data.
         *
         * @param variableLength true for data (false for field/group).
         * @return this for a fluent API.
         */
        public Builder variableLength(final boolean variableLength)
        {
            this.variableLength = variableLength;
            return this;
        }

        /**
         * Set the version for this field/data/group.
         *
         * @param sinceVersion context for this field/data/group. This is the schema version in which the type was introduced.
         * @return this for a fluent API.
         */
        public Builder sinceVersion(final int sinceVersion)
        {
            this.sinceVersion = sinceVersion;
            return this;
        }

        /**
         * Set the version in which this field/data/group was deprecated.
         *
         * @param deprecated version in which this field/data/group was deprecated.
         * @return this for a fluent API.
         */
        public Builder deprecated(final int deprecated)
        {
            this.deprecated = deprecated;
            return this;
        }

        /**
         * Set the epoch from which a timestamps start.
         *
         * @param epoch from which a timestamps start.
         * @return this for a fluent API.
         */
        public Builder epoch(final String epoch)
        {
            this.epoch = epoch;
            return this;
        }

        /**
         * Set the time unit of the field.
         *
         * @param timeUnit of the field.
         * @return this for a fluent API.
         */
        public Builder timeUnit(final String timeUnit)
        {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * Create a {@link Field} from the set values.
         *
         * @return {@link Field} instance.
         */
        public Field build()
        {
            return new Field(
                name,
                description,
                id,
                type,
                offset,
                semanticType,
                presence,
                refValue,
                blockLength,
                dimensionType,
                variableLength,
                sinceVersion,
                deprecated,
                epoch,
                timeUnit);
        }
    }
}
