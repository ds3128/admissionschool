package org.darius.authservice.mapper;

import org.darius.authservice.common.dtos.CreateUserDtoRequest;
import org.darius.authservice.common.dtos.UserDtoResponse;
import org.darius.authservice.entities.Users;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    UserDtoResponse ToUserDtoResponse(Users user);
    Users toUser(CreateUserDtoRequest request);
}
