package com.fmlite.match.tactic;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FormationConverter implements AttributeConverter<Formation, String> {

    @Override
    public String convertToDatabaseColumn(Formation attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Formation convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Formation.fromValue(dbData);
    }
}
