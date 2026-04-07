package com.company.app.mapper;

import com.company.starter.mapstruct.MapstructMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring") // Indispensable pour l'injection Spring
public interface UserMapper extends MapstructMapper<UserEntity, UserDto> {

    @Override
    @Mapping(target = "fullName", expression = "java(entity.getFirstName() + ' ' + entity.getLastName())")
    UserDto toDto(UserEntity entity);
}

// Classes de données pour le test
class UserEntity { 
    private String firstName; 
    private String lastName;
    // Getters/Setters...
}
class UserDto { 
    private String fullName;
    // Getters/Setters...
}
