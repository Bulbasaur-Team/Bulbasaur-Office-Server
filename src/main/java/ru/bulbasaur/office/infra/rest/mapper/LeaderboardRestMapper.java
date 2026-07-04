package ru.bulbasaur.office.infra.rest.mapper;

import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import ru.bulbasaur.office.infra.rest.dto.LeaderboardEntryResponse;
import ru.bulbasaur.office.infra.rest.dto.LeaderboardResponse;
import ru.bulbasaur.office.usecase.dto.LeaderboardRowView;
import ru.bulbasaur.office.usecase.dto.LeaderboardView;

@Mapper(componentModel = "spring")
public interface LeaderboardRestMapper {

    @Mapping(target = "entries", source = "top")
    @Mapping(target = "you", source = "me")
    LeaderboardResponse toResponse(LeaderboardView view);

    @Mapping(target = "you", source = "currentPlayer")
    LeaderboardEntryResponse toEntry(LeaderboardRowView row);
}
