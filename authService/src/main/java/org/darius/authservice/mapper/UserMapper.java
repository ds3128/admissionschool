package org.darius.authservice.mapper;

import jdk.jfr.Name;
import org.darius.authservice.common.dtos.CreateUserDtoRequest;
import org.darius.authservice.common.dtos.UserDtoResponse;
import org.darius.authservice.entities.Role;
import org.darius.authservice.entities.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    @Mapping(target = "role", source = "role", qualifiedByName = "roleToString")
    UserDtoResponse ToUserDtoResponse(Users user);
    Users toUser(CreateUserDtoRequest request);

    @Named("roleToString")
    default String roleToString(Role role) {
        if (role == null) return null;
        return role.getRoleType().name();
    }
}
