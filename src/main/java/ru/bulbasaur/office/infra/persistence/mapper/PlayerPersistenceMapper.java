package ru.bulbasaur.office.infra.persistence.mapper;

import org.mapstruct.Mapper;
import ru.bulbasaur.office.domain.model.Player;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;

@Mapper(componentModel = "spring")
public interface PlayerPersistenceMapper {

    Player toPlayer(PlayerEntity entity);

    StoredPlayer toStoredPlayer(PlayerEntity entity);
}
