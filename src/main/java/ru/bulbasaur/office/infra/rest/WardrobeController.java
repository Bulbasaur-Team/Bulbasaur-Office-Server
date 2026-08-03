package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.domain.model.WardrobeCategory;
import ru.bulbasaur.office.infra.rest.dto.WardrobeBuyRequest;
import ru.bulbasaur.office.infra.rest.dto.WardrobeBuyResponse;
import ru.bulbasaur.office.infra.rest.dto.WardrobeCatalogResponse;
import ru.bulbasaur.office.infra.rest.dto.WardrobeEquipRequest;
import ru.bulbasaur.office.infra.rest.dto.WardrobeEquipResponse;
import ru.bulbasaur.office.infra.rest.dto.WardrobeSellRequest;
import ru.bulbasaur.office.infra.rest.dto.WardrobeSellResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.BuyWardrobeItemUsecase;
import ru.bulbasaur.office.usecase.EquipWardrobeItemUsecase;
import ru.bulbasaur.office.usecase.GetBulbaCoinBalanceUsecase;
import ru.bulbasaur.office.usecase.GetWardrobeCatalogUsecase;
import ru.bulbasaur.office.usecase.SellWardrobeItemUsecase;
import ru.bulbasaur.office.usecase.dto.WardrobeCatalogView;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;
import ru.bulbasaur.office.usecase.dto.WardrobeSellView;

@RestController
@RequestMapping("/api/wardrobe")
@RequiredArgsConstructor
public class WardrobeController {

    private final GetWardrobeCatalogUsecase getCatalog;
    private final BuyWardrobeItemUsecase buyItem;
    private final EquipWardrobeItemUsecase equipItem;
    private final SellWardrobeItemUsecase sellItem;
    private final GetBulbaCoinBalanceUsecase balance;

    @GetMapping("/catalog")
    public WardrobeCatalogResponse catalog(@AuthenticationPrincipal AuthPrincipal player) {
        WardrobeCatalogView view = getCatalog.execute(player.id());
        return new WardrobeCatalogResponse(view.bulbaCoinBalance(), view.appearance(), view.items());
    }

    @PostMapping("/buy")
    public WardrobeBuyResponse buy(@Valid @RequestBody WardrobeBuyRequest request,
                                   @AuthenticationPrincipal AuthPrincipal player) {
        WardrobeItemView item = buyItem.execute(player.id(), request.itemCode());
        return new WardrobeBuyResponse(balance.execute(player.id()), item);
    }

    @PostMapping("/equip")
    public WardrobeEquipResponse equip(@Valid @RequestBody WardrobeEquipRequest request,
                                       @AuthenticationPrincipal AuthPrincipal player) {
        WardrobeCategory category = WardrobeCategory.fromName(request.category())
                .orElseThrow(() -> new IllegalArgumentException("Неизвестная категория: " + request.category()));
        PlayerAppearance appearance = equipItem.execute(player.id(), category, request.itemCode());
        return new WardrobeEquipResponse(appearance);
    }

    @PostMapping("/sell")
    public WardrobeSellResponse sell(@Valid @RequestBody WardrobeSellRequest request,
                                     @AuthenticationPrincipal AuthPrincipal player) {
        WardrobeSellView view = sellItem.execute(player.id(), request.itemCode());
        return new WardrobeSellResponse(view.bulbaCoinBalance(), view.refund(), view.appearance());
    }
}
